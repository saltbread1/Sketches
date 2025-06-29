#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

uniform vec2 resolution;
uniform vec2 direction;
uniform vec3 startColor;
uniform vec3 endColor;

void main()
{
    vec2 uv = (gl_FragCoord.xy*2.- resolution)/ resolution;
    float t = dot(uv, normalize(direction));
    t = t * 0.5 + 0.5;
    vec3 col = mix(startColor, endColor, t);
    gl_FragColor = vec4(fract(col), 1.);
}
