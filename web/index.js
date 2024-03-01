const baseUrl = 'http://localhost:5500/'
const duration = 3600
const particleSize = 1

async function loadFileContent(path) {
    const response = await fetch(baseUrl + path);
    return await response.text();
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

async function main() {
    const canvas = document.getElementById('canvas');
    const gl = canvas.getContext('webgl');

    if (!gl) {
        alert('Your browser doesn\'t support WebGL :(');
    }

    const program = await createProgram(gl, 'shaders/particles_vert.glsl', 'shaders/particles_frag.glsl');

    gl.useProgram(program);

    const removableElement = document.getElementById('removable');
    removableElement.addEventListener('click', () => removeElementWithAnimation(gl, program, removableElement));
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

var animationStartTime = -1;

function resizeCanvasToDisplaySize(canvas) {
    // Lookup the size the browser is displaying the canvas in CSS pixels.
    const displayWidth  = canvas.clientWidth;
    const displayHeight = canvas.clientHeight;
   
    // Check if the canvas is not the same size.
    const needResize = canvas.width  !== displayWidth ||
                       canvas.height !== displayHeight;
   
    if (needResize) {
        // Make the canvas the same size
        canvas.width  = displayWidth;
        canvas.height = displayHeight;
    }
   
    return needResize;
}

function removeElementWithAnimation(gl, program, element) {
    var img = new Image();
    img.onload = function() {
        resizeCanvasToDisplaySize(gl.canvas);

        gl.viewport(0, 0, gl.canvas.width, gl.canvas.height);

        const rect = element.getBoundingClientRect();

        var canvas = document.createElement('canvas');
        canvas.width = img.width;
        canvas.height = img.height;
        var ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0);
        var imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
        
        const textureWidth = Math.round(512 / particleSize);
        const textureHeight = Math.round(512 / particleSize);
        const textureLeft = element.offsetLeft;
        const textureTop = element.offsetTop;
        console.log(rect.width + ' ' + rect.height)

        
    
        gl.viewport(0, 0, window.innerWidth, window.innerHeight)

        var texture = gl.createTexture();
        gl.bindTexture(gl.TEXTURE_2D, texture);
    
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, gl.CLAMP_TO_EDGE);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST);
        gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);

        gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, imageData);
    

        const textureLocation = gl.getUniformLocation(program, 'u_Texture');
        gl.uniform1i(textureLocation, 0);

        glUniform1f(gl, program, 'u_AnimationDuration', duration);
        glUniform1f(gl, program, 'u_ParticleSize', particleSize);
        glUniform1f(gl, program, 'u_ViewportWidth', window.innerWidth);
        glUniform1f(gl, program, 'u_ViewportHeight', window.innerHeight);
        glUniform1f(gl, program, 'u_TextureWidth', textureWidth);
        glUniform1f(gl, program, 'u_TextureHeight', textureHeight);
        glUniform1f(gl, program, 'u_TextureLeft', textureLeft);
        glUniform1f(gl, program, 'u_TextureTop', textureTop);

        console.log(gl.canvas.width + ' ' + gl.canvas.height)
        const particleIndices = new Array(textureWidth * textureHeight);
        for (let i = 0; i < textureWidth * textureHeight; i++) {
            particleIndices[i] = i;
        }

        const particleIndicesBuffer = gl.createBuffer();
        gl.bindBuffer(gl.ARRAY_BUFFER, particleIndicesBuffer);
        gl.bufferData(gl.ARRAY_BUFFER, new Float32Array(particleIndices), gl.STATIC_DRAW);
    
        const particleIndexAttrLocation = gl.getAttribLocation(program, 'a_ParticleIndex');
        gl.enableVertexAttribArray(particleIndexAttrLocation);
        gl.vertexAttribPointer(particleIndexAttrLocation, 1, gl.FLOAT, false, 0, 0);
        
        requestAnimationFrame((time) => drawFrame(gl, program, particleIndices, textureWidth * textureHeight, performance.now()))
        
        element.parentNode.removeChild(element);
    };
    img.src = document.getElementById('andy').src;
}

function drawFrame(gl, program, particleIndices, particlesCount, time) {
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

    requestAnimationFrame((time) => drawFrame(gl, program, particleIndices, particlesCount, time));
}

function glUniform1f(gl, program, name, value) {
    const location = gl.getUniformLocation(program, name);
    gl.uniform1f(location, value);
}

window.onload = main;