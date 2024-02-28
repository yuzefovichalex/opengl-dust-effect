/*
 * Copyright 2024 Alexander Yuzefovich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alexyuzefovich.dusteffect;

import static android.opengl.GLES10.glBindTexture;
import static android.opengl.GLES11.glTexParameteri;
import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_POINTS;
import static android.opengl.GLES20.GL_SRC_ALPHA;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glDisableVertexAttribArray;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetError;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLES20.glViewport;
import static javax.microedition.khronos.opengles.GL10.GL_FLOAT;
import static javax.microedition.khronos.opengles.GL10.GL_TEXTURE_2D;
import static javax.microedition.khronos.opengles.GL10.GL_TEXTURE_MAG_FILTER;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alexyuzefovich.dusteffect.utils.ShaderUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * An implementation of GLSurfaceView.Renderer used for drawing a dust ("Thanos") effect on disappearing Views.
 *
 * @author Alexander Yuzefovich
 * */
public class DustEffectRenderer implements GLSurfaceView.Renderer {

    private static final int DEFAULT_ANIMATION_DURATION = 1800;
    private static final int DEFAULT_PARTICLE_SIZE = 1;

    private final Context context;

    private long duration = DEFAULT_ANIMATION_DURATION;
    private int particleSize = DEFAULT_PARTICLE_SIZE;

    private int particlesProgramId;

    private int aParticleIndex;

    private final ConcurrentLinkedQueue<RenderInfo> renderInfos = new ConcurrentLinkedQueue<>();


    public DustEffectRenderer(@NonNull Context context) {
        this.context = context;
    }


    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void setParticleSize(int particleSize) {
        this.particleSize = particleSize;
    }

    @Override
    public void onSurfaceCreated(GL10 arg0, EGLConfig arg1) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        int particlesVertexShaderId = ShaderUtils.createShader(context, GL_VERTEX_SHADER, R.raw.particles_vert);
        int particlesFragmentShaderId = ShaderUtils.createShader(context, GL_FRAGMENT_SHADER, R.raw.particles_frag);
        particlesProgramId = ShaderUtils.createProgram(particlesVertexShaderId, particlesFragmentShaderId);

        aParticleIndex = glGetAttribLocation(particlesProgramId, "a_ParticleIndex");
    }

    @Override
    public void onSurfaceChanged(GL10 arg0, int width, int height) {
        glViewport(0, 0, width, height);
        glUseProgram(particlesProgramId);
        glUniform1f("u_ViewportWidth", width);
        glUniform1f("u_ViewportHeight", height);
    }

    @Override
    public void onDrawFrame(GL10 arg0) {
        glClear(GL_COLOR_BUFFER_BIT);

        if (renderInfos.isEmpty()) {
            return;
        }

        glUseProgram(particlesProgramId);

        glUniform1f("u_AnimationDuration", duration);
        glUniform1f("u_ParticleSize", particleSize);

        long currentTime = System.currentTimeMillis();
        for (Iterator<RenderInfo> iterator = renderInfos.iterator(); iterator.hasNext(); ) {
            RenderInfo renderInfo = iterator.next();
            if (!renderInfo.isDataReady) {
                continue;
            }
            renderInfo.loadTextureIfNeeded();
            boolean isFrameDrawn = drawFrame(renderInfo, currentTime);
            if (!isFrameDrawn) {
                renderInfo.recycle();
                iterator.remove();
            }
        }
    }

    /**
     * @return true if frame was successfully drawn or false if not. False means that passed RenderInfo
     * is not needed anymore and can be recycled.
     * */
    private boolean drawFrame(@NonNull RenderInfo renderInfo, long currentTime) {
        if (renderInfo.animationStartTime == -1) {
            renderInfo.animationStartTime = System.currentTimeMillis();
        }

        long elapsedTime = currentTime - renderInfo.animationStartTime;
        if (elapsedTime > duration) {
            return false;
        }

        int uTexture = glGetUniformLocation(particlesProgramId, "u_Texture");
        glUniform1i(uTexture, 0);

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, renderInfo.textureId);

        glUniform1f("u_ElapsedTime", elapsedTime);

        glUniform1f("u_TextureWidth", renderInfo.columnCount);
        glUniform1f("u_TextureHeight", renderInfo.rowCount);
        glUniform1f("u_TextureLeft", renderInfo.textureLeft);
        glUniform1f("u_TextureTop", renderInfo.textureTop);

        glVertexAttribPointer(aParticleIndex, 1, GL_FLOAT, false, 0, renderInfo.particlesIndicesBuffer);
        glEnableVertexAttribArray(aParticleIndex);

        glDrawArrays(GL_POINTS, 0, renderInfo.columnCount * renderInfo.rowCount);

        glDisableVertexAttribArray(aParticleIndex);

        return true;
    }

    public void composeView(@NonNull View view) {
        RenderInfo renderInfo = new RenderInfo(particleSize);
        renderInfos.add(renderInfo);
        renderInfo.composeView(view);
    }

    private void glUniform1f(@NonNull String name, float param) {
        int location = glGetUniformLocation(particlesProgramId, name);
        GLES20.glUniform1f(location, param);
    }

    private void checkError() {
        int e = glGetError();
        Log.d("ParticlesRenderer", "OpenGL error, code " + e);
    }


    private static class RenderInfo {

        private final int particleSize;

        private long animationStartTime = -1;

        private int columnCount;
        private int rowCount;
        private float textureLeft;
        private float textureTop;

        @Nullable
        private Bitmap sourceBitmap;

        private int textureId;

        @Nullable
        private FloatBuffer particlesIndicesBuffer;

        private boolean isDataReady;
        private boolean isTextureLoaded;


        public RenderInfo(int particleSize) {
            this.particleSize = particleSize;
        }


        public void loadTextureIfNeeded() {
            if (isTextureLoaded) {
                return;
            }

            Bitmap sourceBitmap = this.sourceBitmap;
            if (sourceBitmap == null || sourceBitmap.isRecycled()) {
                throw new IllegalStateException("Source bitmap can't be used: null or recycled.");
            }

            final int[] textureHandle = new int[1];
            glGenTextures(1, textureHandle, 0);

            if (textureHandle[0] != 0) {
                glBindTexture(GL_TEXTURE_2D, textureHandle[0]);

                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

                GLUtils.texImage2D(GL_TEXTURE_2D, 0, sourceBitmap, 0);

                this.sourceBitmap = null;
            }

            if (textureHandle[0] == 0) {
                throw new RuntimeException("Error loading texture.");
            }

            textureId = textureHandle[0];
            isTextureLoaded = true;
        }

        public void composeView(@NonNull View view) {
            animationStartTime = -1;
            isDataReady = false;
            isTextureLoaded = false;
            textureId = 0;

            int[] viewLocation = new int[2];
            view.getLocationOnScreen(viewLocation);

            columnCount = view.getWidth() / particleSize;
            rowCount = view.getHeight() / particleSize;
            textureLeft = viewLocation[0];
            textureTop = viewLocation[1];

            ExecutorService executorService = Executors.newFixedThreadPool(2);

            executorService.submit(() -> {
                Bitmap viewBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(viewBitmap);
                view.draw(c);
                sourceBitmap = viewBitmap;
            });

            executorService.submit(() -> {
                int particlesCount = columnCount * rowCount;

                float[] particlesIndices = new float[particlesCount];
                for (int i = 0; i < particlesCount; i++) {
                    particlesIndices[i] = i;
                }

                particlesIndicesBuffer = createFloatBuffer(particlesIndicesBuffer, particlesIndices);
            });

            executorService.shutdown();

            try {
                boolean terminated = executorService.awaitTermination(1, TimeUnit.MINUTES);
                if (!terminated) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }

            isDataReady = true;
        }

        public void recycle() {
            int[] textureHandle = { textureId };
            glDeleteTextures(1, textureHandle, 0);
        }

        @NonNull
        private FloatBuffer createFloatBuffer(
            @Nullable FloatBuffer existingBuffer,
            @NonNull float[] values
        ) {
            FloatBuffer buffer;
            if (existingBuffer != null && existingBuffer.capacity() == values.length) {
                buffer = existingBuffer;
            } else {
                buffer = ByteBuffer
                    .allocateDirect(values.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            }
            buffer.put(values);
            buffer.position(0);
            return buffer;
        }

    }

}
