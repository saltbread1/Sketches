uniform vec4 lightPosition;
uniform vec3 fogColor;

varying vec3 vPosition;
varying vec3 vNormal;
varying vec4 vColor;
varying float vFogFactor;

void main()
{
    vec3 N = normalize(vNormal);
    vec3 L = normalize(lightPosition.xyz - vPosition * lightPosition.w);
    vec3 V = -normalize(vPosition);
    vec3 H = normalize(L + V);
    vec3 diff = vColor.rgb * max(dot(N, L), 0.0);
    vec3 spec = vec3(1.0) * pow(max(dot(H, N), 0.0), 100.0);
    vec3 col = min(diff + spec, vec3(1.0));
    col = mix(col, fogColor, vFogFactor);

    gl_FragColor = vec4(col, 1.0);
}
