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

const baseUrl = 'http://localhost:5500/';
const duration = 3600;
const particleSize = 1;

var gl = null;
var program = null;
var animationStartTime = -1;
var particlesCount = 0;

async function loadFileContent(path) {
    const response = await fetch(baseUrl + path);
    return await response.text();
}

async function main() {
    const canvas = document.getElementById('canvas');
    gl = canvas.getContext('webgl');

    if (!gl) {
        alert('Your browser doesn\'t support WebGL :(');
    }

    await init();

    const removableElement = document.getElementById('removable');
    removableElement.addEventListener('click', () => removeElementWithAnimation(removableElement));
}

async function init() {
    resizeCanvasToDisplaySize(gl.canvas);

    gl.clearColor(0, 0, 0, 0);
    gl.viewport(0, 0, gl.canvas.width, gl.canvas.height);

    program = await createProgram(gl, 'shaders/particles_vert.glsl', 'shaders/particles_frag.glsl');
    gl.useProgram(program);
}

function resizeCanvasToDisplaySize(canvas) {
    // Lookup the size the browser is displaying the canvas in CSS pixels.
    const displayWidth  = canvas.clientWidth;
    const displayHeight = canvas.clientHeight;
   
    const needResize = canvas.width  !== displayWidth ||
                       canvas.height !== displayHeight;
   
    if (needResize) {
        canvas.width  = displayWidth;
        canvas.height = displayHeight;
    }
   
    return needResize;
}

async function createProgram(gl, vertexShaderPath, fragmentShaderPath) {
    const vertexShaderContent = await loadFileContent(vertexShaderPath);
    const fragmentShaderContent = await loadFileContent(fragmentShaderPath);

    const vertexShader = compileShader(gl, vertexShaderContent, gl.VERTEX_SHADER);
    const fragmentShader = compileShader(gl, fragmentShaderContent, gl.FRAGMENT_SHADER);

    const program = gl.createProgram();
    gl.attachShader(program, vertexShader);
    gl.attachShader(program, fragmentShader);
    gl.linkProgram(program);

    if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
        console.error('Shader program initialization failure:', gl.getProgramInfoLog(program));
    }

    return program;
}

function compileShader(gl, source, type) {
    const shader = gl.createShader(type);
    gl.shaderSource(shader, source);
    gl.compileShader(shader);

    if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
        console.error('Shader compilation failure:', gl.getShaderInfoLog(shader));
        gl.deleteShader(shader);
        return null;
    }

    return shader;
}

function removeElementWithAnimation(element) {
    const image = new Image();
    image.onload = () => {
        loadTexture(image);
        bindParameters(element)
        requestDraw();
        element.parentNode.removeChild(element);
    };
    html2canvas(element, { scale: 2 }).then(canvas => {
        image.src = canvas.toDataURL('image/png');
    });
}

function loadTexture(image) {
    const imageData = getImageData(image);

    var texture = gl.createTexture();
    gl.bindTexture(gl.TEXTURE_2D, texture);

    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);

    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, imageData);

    const textureLocation = gl.getUniformLocation(program, 'u_Texture');
    gl.uniform1i(textureLocation, 0);
}

function getImageData(image) {
    const canvas = document.createElement('canvas');
    canvas.width = image.width;
    canvas.height = image.height;

    const context = canvas.getContext('2d');
    context.drawImage(image, 0, 0);
    return context.getImageData(0, 0, canvas.width, canvas.height);
}

function bindParameters(element) {
    const rect = element.getBoundingClientRect();
    const textureWidth = Math.round(rect.width / particleSize);
    const textureHeight = Math.round(rect.height / particleSize);
    const textureLeft = element.offsetLeft;
    const textureTop = element.offsetTop;
    particlesCount = textureWidth * textureHeight;

    glUniform1f(gl, program, 'u_AnimationDuration', duration);
    glUniform1f(gl, program, 'u_ParticleSize', particleSize);
    glUniform1f(gl, program, 'u_ViewportWidth', window.innerWidth);
    glUniform1f(gl, program, 'u_ViewportHeight', window.innerHeight);
    glUniform1f(gl, program, 'u_TextureWidth', textureWidth);
    glUniform1f(gl, program, 'u_TextureHeight', textureHeight);
    glUniform1f(gl, program, 'u_TextureLeft', textureLeft);
    glUniform1f(gl, program, 'u_TextureTop', textureTop);

    const particleIndices = new Array(particlesCount);
    for (let i = 0; i < particlesCount; i++) {
        particleIndices[i] = i;
    }

    const particleIndicesBuffer = gl.createBuffer();
    gl.bindBuffer(gl.ARRAY_BUFFER, particleIndicesBuffer);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(particleIndices), gl.STATIC_DRAW);

    const particleIndexAttrLocation = gl.getAttribLocation(program, 'a_ParticleIndex');
    gl.enableVertexAttribArray(particleIndexAttrLocation);
    gl.vertexAttribPointer(particleIndexAttrLocation, 1, gl.FLOAT, false, 0, 0);
}

function requestDraw() {
    requestAnimationFrame(draw)
}

function draw(time) {
    gl.clear(gl.COLOR_BUFFER_BIT);

    if (animationStartTime == -1) {
        animationStartTime = time;
    }

    const currentTime = time;
    const elapsedTime = currentTime - animationStartTime;
    if (elapsedTime > duration) {
        animationStartTime = -1;
        return;
    }

    glUniform1f(gl, program, 'u_ElapsedTime', elapsedTime);

    gl.drawArrays(gl.POINTS, 0, particlesCount);

    requestDraw();
}

function glUniform1f(gl, program, name, value) {
    const location = gl.getUniformLocation(program, name);
    gl.uniform1f(location, value);
}

window.onload = main;