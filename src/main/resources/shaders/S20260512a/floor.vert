uniform mat4 modelviewMatrix;
uniform mat4 transformMatrix;
uniform mat3 normalMatrix;
uniform vec2 fogRange;

attribute vec4 position;
attribute vec3 normal;
attribute vec4 color;

varying vec3 vPosition;
varying vec3 vNormal;
varying vec4 vColor;
varying float vFogFactor;

void main()
{
    vPosition = (modelviewMatrix * position).xyz;
    vNormal = normalize(normalMatrix * normal);
    vColor = color;

    float dist = length(vPosition);
    float r = clamp((dist - fogRange.x) / (fogRange.y - fogRange.x), 0.0, 1.0);
    vFogFactor = r * r;

    gl_Position = transformMatrix * position;
}
