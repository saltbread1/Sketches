#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

uniform vec3 fogColor;

varying vec4 vertColor;
varying float fogFactor;

void main()
{
    vec3 col = vertColor.rgb;
    col = mix(col, fogColor, fogFactor);
    gl_FragColor = vec4(mix(col, fogColor, fogFactor), vertColor.a);
}
