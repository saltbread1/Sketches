#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

uniform vec2 direction;
uniform vec3 startColor;
uniform vec3 endColor;

varying vec4 vertColor;
varying vec4 vertTexCoord;

void main()
{
    vec2 uv = vertTexCoord.st * 2.0 - 1.0;
    float t = dot(uv, normalize(direction)) * 0.70710678;
    t = t * 0.5 + 0.5;
    t = clamp(t, 0.0, 1.0);
    vec3 col = mix(startColor, endColor, t);
    gl_FragColor = vec4(fract(col), 1.) * vertColor;
}
