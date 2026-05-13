uniform mat4 modelviewMatrix;
uniform mat4 transformMatrix;
uniform mat3 normalMatrix;
uniform mat4 texMatrix;

attribute vec4 position;
attribute vec3 normal;
attribute vec4 color;
attribute vec2 texCoord;

varying vec3 vPosition;
varying vec2 vUV;
varying vec3 vNormal;
varying vec4 vColor;

void main()
{
    vPosition = (modelviewMatrix * position).xyz;
    vUV = (texMatrix * vec4(texCoord, 1.0, 1.0)).st;
    vNormal = normalize(normalMatrix * normal);
    vColor = color;

    gl_Position = transformMatrix * position;
}
