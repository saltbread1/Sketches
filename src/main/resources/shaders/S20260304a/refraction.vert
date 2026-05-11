uniform mat4 modelviewMatrix;
uniform mat4 transformMatrix;
uniform mat3 normalMatrix;
uniform mat4 modelMatrix;
uniform mat4 tViewMatrix;

attribute vec4 position;
attribute vec3 normal;
attribute vec4 color;

varying vec3 vPosition; // world
varying vec3 vNormal; // world
varying vec4 vColor;

void main()
{
    vPosition = (modelMatrix * position).xyz;
    vNormal = normalize(mat3(tViewMatrix) * normalMatrix * normal);
    vColor = color;

    gl_Position = transformMatrix * position;
}
