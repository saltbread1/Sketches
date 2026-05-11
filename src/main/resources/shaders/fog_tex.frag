uniform sampler2D tex;

varying vec2 uv;
varying float fogFactor;
varying vec4 vertColor;

void main()
{
    vec3 col = vertColor.rgb;
    vec3 fogColor = texture(tex, uv).rgb;
    col = mix(col, fogColor, fogFactor);
    gl_FragColor = vec4(col, vertColor.a);
}
