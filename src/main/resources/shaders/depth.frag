precision highp float;

uniform float near;
uniform float far;

varying vec3 vPosition;

vec4 encodeFloat(float v)
{
    uint bits = floatBitsToUint(v);
    return vec4(
        float((bits >> 24u) & 0xFFu),
        float((bits >> 16u) & 0xFFu),
        float((bits >>  8u) & 0xFFu),
        float( bits         & 0xFFu)
    ) / 255.0;
}

void main()
{
#if 1
//    float depth = gl_FragCoord.z;
    float z_eye = 2.0 * near * far / (near + far - (2.0 * gl_FragCoord.z - 1.0) * (far - near));
    float depth = (z_eye - near) / (far - near);
#else
    float depth = length(vPosition) / (far - near);
#endif
    gl_FragColor = encodeFloat(depth);
}
