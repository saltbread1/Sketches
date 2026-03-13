uniform mat4 modelviewMatrix;
uniform mat4 transformMatrix;
uniform mat3 normalMatrix;

attribute vec4 position;
attribute vec3 normal;
attribute vec4 color;

varying vec3 vPosition;
varying vec3 vNormal;
varying vec3 vSampleDirection;
varying vec4 vColor;

void main()
{
    vPosition = (modelviewMatrix * position).xyz;
    vNormal = normalize(normalMatrix * normal);
    vSampleDirection = mix(normalize(position.xyz), normalize(normal), 0.1);
    vColor = color;

    gl_Position = transformMatrix * position;
}
