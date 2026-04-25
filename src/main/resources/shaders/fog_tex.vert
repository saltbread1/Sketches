uniform mat4 modelviewMatrix;
uniform mat4 transformMatrix;
uniform vec2 fogRange;

attribute vec4 position;
attribute vec4 color;

varying vec2 uv;
varying float fogFactor;
varying vec4 vertColor;

void main()
{
    gl_Position = transformMatrix * position;
    uv = gl_Position.xy / gl_Position.w * 0.5 + 0.5;

    vec4 viewPos = modelviewMatrix * position;
    float dist = length(viewPos);
    fogFactor = clamp((dist - fogRange.x) / (fogRange.y - fogRange.x), 0.0, 1.0);

    vertColor = color;
}
