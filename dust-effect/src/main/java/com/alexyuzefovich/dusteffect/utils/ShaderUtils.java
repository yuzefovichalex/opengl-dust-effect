package com.alexyuzefovich.dusteffect.utils;

import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;

import android.content.Context;
import android.util.Log;

/**
 * @author Alexander Yuzefovich
 * */
public class ShaderUtils {

    public static int createProgram(int vertexShaderId, int fragmentShaderId) {
        final int programId = glCreateProgram();
        if (programId == 0) {
            return 0;
        }
        glAttachShader(programId, vertexShaderId);
        glAttachShader(programId, fragmentShaderId);
        glLinkProgram(programId);
        final int[] linkStatus = new int[1];
        glGetProgramiv(programId, GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            glDeleteProgram(programId);
            return 0;
        }
        return programId;
    }

    public static int createShader(Context context, int type, int shaderRawId) {
        String shaderText = FileUtils.readTextFromRaw(context, shaderRawId);
        return ShaderUtils.createShader(type, shaderText);
    }

    public static int createShader(int type, String shaderText) {
        final int shaderId = glCreateShader(type);
        if (shaderId == 0) {
            return 0;
        }
        glShaderSource(shaderId, shaderText);
        glCompileShader(shaderId);
        final int[] compileStatus = new int[1];
        glGetShaderiv(shaderId, GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            Log.e("ShaderUtils", "Shader compilation failure. Reason: " + glGetShaderInfoLog(shaderId));
            glDeleteShader(shaderId);
            return 0;
        }
        return shaderId;
    }

}
