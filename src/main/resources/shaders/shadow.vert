uniform mat4 modelviewMatrix;
uniform mat4 transformMatrix;
uniform mat3 normalMatrix;
uniform mat4 texMatrix;
uniform mat4 invViewMatrix;
uniform mat4 lgProjViewMatrix;
uniform mat4 texProjMatrix;

attribute vec4 position;
attribute vec3 normal;
attribute vec2 texCoord;
attribute vec4 color;

varying vec3 vPosition;
varying vec3 vNormal;
varying vec2 vTexCoord;
varying vec4 vLgPosition;
varying vec4 vLgTexCoord;
varying vec4 vColor;

void main()
{
    vPosition = (modelviewMatrix * position).xyz;
    vNormal = normalize(normalMatrix * normal);
    vTexCoord = (texMatrix * vec4(texCoord, 1.0, 1.0)).st;
    vLgPosition = lgProjViewMatrix * invViewMatrix * modelviewMatrix * position;
    vLgTexCoord = texProjMatrix * vLgPosition;
    vColor = color;

    gl_Position = transformMatrix * position;
}
