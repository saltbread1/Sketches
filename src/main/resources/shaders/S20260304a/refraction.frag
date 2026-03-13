uniform sampler2D panorama;
uniform vec3      eyePosition;
uniform float     eta;

varying vec3 vPosition;
varying vec3 vNormal;
varying vec4 vColor;

const float PI = acos(-1.0);

vec3 toSRGB(vec3 rgb)
{
    float invGamma = 0.416666667;
    return mix(rgb * 12.92, pow(rgb, vec3(invGamma)) * 1.055 - 0.055, vec3(greaterThan(rgb, vec3(0.0031308))));
}

void main()
{
    vec3 dir = refract(normalize(vPosition - eyePosition), vNormal, eta);
//    vec3 dir = reflect(normalize(vPosition - eyePosition), vNormal);
    float phi = atan(dir.x, dir.z);
    float theta = asin(dir.y);
    vec2 uv = vec2(phi / (2.0 * PI) + 0.5, theta / PI + 0.5);
    vec3 col = texture(panorama, uv).rgb;
    col *= col * 1.3;

    gl_FragColor = vec4(toSRGB(col), 1.0) * vColor;
}
