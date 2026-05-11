uniform sampler2D panorama;

varying vec3 vPosition;
varying vec3 vNormal;
varying vec3 vSampleDirection;
varying vec4 vColor;

const float PI = acos(-1.0);
const float NOISE_SCALE = 6.;
const int OCTAVES = 6;
const int WARP_ITERATIONS = 3;

vec3 toSRGB(vec3 rgb)
{
    float invGamma = 0.416666667;
    return mix(rgb * 12.92, pow(rgb, vec3(invGamma)) * 1.055 - 0.055, vec3(greaterThan(rgb, vec3(0.0031308))));
}

float random(const vec2 st)
{
    return fract(sin(dot(st, vec2(12.9898,78.233))) * 43758.5453);
}

vec2 random2d(const vec2 st)
{
    vec2 val = vec2( dot(st,vec2(127.1,311.7)),
    dot(st,vec2(269.5,183.3)) );
    return fract(sin(val)*43758.5453123);
}

float noise(const vec2 v)
{
    vec2 i = floor(v);
    vec2 f = fract(v);
    vec2 u = f*f*(3.-2.*f);
    return mix( mix( random( i + vec2(0., 0.) ),
                     random( i + vec2(1., 0.) ), u.x),
                mix( random( i + vec2(0., 1.) ),
                     random( i + vec2(1., 1.) ), u.x), u.y);
}

float fbm(const vec2 v)
{
    vec2 p = v;
    float result = 0.0;
    float amplitude = .8;

    for (int i = 0; i < OCTAVES; i++)
    {
        result += amplitude * noise(p);
        amplitude *= 0.5;
        p *= 2.0;
    }

    return result;
}

float domainWarp(const vec2 v)
{
    vec2 n_val = vec2(0);
    for (int i = 0; i < WARP_ITERATIONS; i++)
    {
        vec2 val = n_val * 4. + v;
        float n1 = fbm(val + random2d(vec2(i * 2)) * 4.);
        float n2 = fbm(val + random2d(vec2(i * 2 + 1)) * 7.);
        n_val = vec2(n1, n2);
    }
    return n_val.x;
}

vec3 hsb2rgb(const vec3 hsb)
{
    return ((clamp(abs(fract(hsb.x+vec3(0,2,1)/3.)*6.-3.)-1.,0.,1.)-1.)*hsb.y+1.)*hsb.z;
}

vec3 getBgColor(vec2 uv)
{
    float n_val = domainWarp(uv*NOISE_SCALE);
    float h_val = floor(fract((1. - n_val) * 2.4) * 16.) / 16.;
    float s_val = clamp(n_val*.71, 0., 1.);
    float b_val = clamp((1.-n_val)*1.64, 0., 1.);
    vec3 hsb = vec3(h_val, s_val, b_val);

    return hsb2rgb(hsb);
}

void main()
{
    float phi = atan(vSampleDirection.x, vSampleDirection.z);
    float theta = asin(vSampleDirection.y);
    vec2 uv = vec2(phi / (2.0 * PI) + 0.5, theta / PI + 0.5);
    vec3 bg = getBgColor(uv);

    vec3 N = -normalize(vNormal);
    vec3 L = normalize(vec3(0.0, 0.3, 1.0));
    vec3 V = -normalize(vPosition);
    vec3 H = normalize(L + V);
    vec3 diff = bg * max(dot(N, L), 0.0);
    vec3 spec = vec3(1.0) * pow(max(dot(H, N), 0.0), 100.0);
    vec3 col = diff + spec;
    col *= col * 2.3;

    gl_FragColor = vec4(toSRGB(col), 1.0) * vColor;
}
