uniform mat4 transformMatrix;

attribute vec4 position;
attribute vec4 color;
attribute vec2 texCoord;

varying vec4 vertColor;
varying vec4 vertTexCoord;

void main()
{
    gl_Position = transformMatrix * position;

    vertColor = color;
    vertTexCoord = vec4(texCoord, 1.0, 1.0);
}
