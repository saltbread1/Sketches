uniform sampler2D panorama;

varying vec3 vPosition;
varying vec3 vNormal;
varying vec2 vUV;
varying vec4 vColor;

vec3 toSRGB(vec3 rgb)
{
    float invGamma = 0.416666667;//1.0 / 2.4;
    return mix(rgb * 12.92, pow(rgb, vec3(invGamma)) * 1.055 - 0.055, vec3(greaterThan(rgb, vec3(0.0031308))));
}

void main()
{
    vec3 bg = texture(panorama, vUV).rgb;

    vec3 N = -normalize(vNormal);
    vec3 L = normalize(vec3(0.0, 0.3, 1.0));
    vec3 V = -normalize(vPosition);
    vec3 H = normalize(L + V);
    vec3 diff = bg * max(dot(N, L), 0.0) * vec3(0.96, 0.92, 0.89) * 0.82;
    vec3 spec = bg * pow(max(dot(H, N), 0.0), 20.0) * vec3(0.82, 0.74, 0.71) * 0.58;
    vec3 col = diff + spec;
    col *= col * 1.6;

    gl_FragColor = vec4(toSRGB(col), 1.0);
}
