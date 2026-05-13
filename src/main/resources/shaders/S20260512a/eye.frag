uniform vec4 lightPosition;

varying vec3 vPosition;
varying vec2 vUV;
varying vec3 vNormal;
varying vec4 vColor;

const float eyeRadius = 1.0;
const float pupilRadiusBase = 0.013;

float random(vec2 st)
{
    return fract(sin(dot(st, vec2(12.9898,78.233))) * 43758.5453);
}

float noise(vec2 v)
{
    vec2 i = floor(v);
    vec2 f = fract(v);
    vec2 u = f*f*(3.-2.*f);
    return mix( mix( random( i + vec2(0., 0.) ),
                     random( i + vec2(1., 0.) ), u.x),
                mix( random( i + vec2(0., 1.) ),
                     random( i + vec2(1., 1.) ), u.x), u.y);
}

float fbm(vec2 p, float g, const int count)
{
    float val = 0.0;
    float amp = 1.0;
    float freq = 1.0;
    for (int i = 0; i < count; i++)
    {
        val += amp * (noise(freq * p) - 0.5);
        amp *= g;
        freq *= 2.01;
    }
    return val * 0.5 + 0.5;
}

void calcParams(in vec2 uv,
                out float n0, out float n1, out float n2,
                out float r0, out float r1, out float r2, out float r3)
{
    // iris
    n0 = fbm(uv * 4.0, 0.5, 4);

    // behind streaks
    float t = atan(abs(uv.y), uv.x) * 0.8;
    float a0 = t - 0.16 * fbm(uv * 10.0, 0.5, 4);
    float b = sqrt(length(uv));
    n1 = fbm(vec2(a0 * 40.0, b * 10.0), 0.3, 4);
    n1 = smoothstep(0.2, 0.9, n1) * 0.6;
    // front streaks
    float a1 = t - 0.08 * fbm(uv * 10.0, 0.5, 4);
    n2 = fbm(vec2(a1 * 80.0, b * 20.0), 0.5, 4);
    n2 = smoothstep(0.35, 1.0, n2) * 0.9;

    float r = pupilRadiusBase / (dot(uv, uv) + 1e-6);
    r0 = smoothstep(0.08, 1.0, r * r); // pupil
    r1 = smoothstep(0.16, 1.0, r * 3.3); // iris (middle)
    r2 = smoothstep(0.5, 0.9, sqrt((1.0 - r) * 0.8)); // streaks
    r3 = smoothstep(0.1, 1.0, (1.0 - r) * (1.0 - r) * 0.8); // iris color (middle)
}

vec3 calcColor(vec2 uv, float irisRadius)
{
    // parameters
    float r0, r1, r2, r3;
    float n0, n1, n2;
    calcParams(uv, n0, n1, n2, r0, r1, r2, r3);

    // iris
    vec3 col = mix(vec3(0.04, 0.25, 0.48), vec3(0.23, 0.52, 0.44), n0);
    col = mix(col, mix(vec3(0.37, 0.24, 0.13), vec3(0.84, 0.91, 0.11), r3), r1);
    // pupil
    col = mix(col, vec3(0.06, 0.05, 0.02), r0);
    // streaks
    col = mix(col, vec3(0.15, 0.14, 0.13) * r2, n1);
    col = mix(col, vec3(0.86, 0.84, 0.81) * r2, n2);
    // conjunctiva
    col = mix(col, vec3(1.0), smoothstep(irisRadius - 0.04, irisRadius + 0.04, length(uv)));

    return col;
}

void main()
{
    vec2 uv = vUV * 2.0 - 1.0;

    // eye
    vec3 color = calcColor(uv, 0.44);

    // world corrdinate
    vec3 lightDir = normalize(lightPosition.xyz - vPosition * lightPosition.w);

    vec3 N = normalize(vNormal);
    vec3 L = normalize(lightPosition.xyz - vPosition * lightPosition.w);
    vec3 V = -normalize(vPosition);
    vec3 H = normalize(L + V);
    vec3 diff = vColor.xyz * color * max(dot(N, L), 0.0);
    vec3 spec = vec3(1.0) * pow(max(dot(H, N), 0.0), 100.0);
    color = min(diff + spec, vec3(1.0));

    // add background
    gl_FragColor = vec4(color, 1.0);
}
