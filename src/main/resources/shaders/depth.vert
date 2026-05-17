uniform mat4 modelviewMatrix;
uniform mat4 transformMatrix;

attribute vec4 position;

varying vec3 vPosition;

void main()
{
    vPosition = (modelviewMatrix * position).xyz;
    gl_Position = transformMatrix * position;
}
