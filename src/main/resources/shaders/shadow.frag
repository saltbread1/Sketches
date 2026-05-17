precision highp float;

uniform vec4 lightPosition;
uniform sampler2D depthBuffer;
uniform vec2 texOffset;
uniform vec3 shadowColor;
uniform float near;
uniform float far;

varying vec3 vPosition;
varying vec3 vNormal;
varying vec2 vTexCoord;
varying vec4 vLgPosition;
varying vec4 vLgTexCoord;
varying vec4 vColor;

const mat3 pcfKernel = mat3(1, 2, 1, 2, 4, 2, 1, 2, 1) / 16.0;

float decodeFloat(vec4 v)
{
    uint bits = (uint(v.r * 255.0) << 24u)
        | (uint(v.g * 255.0) << 16u)
        | (uint(v.b * 255.0) <<  8u)
        |  uint(v.a * 255.0);
    return uintBitsToFloat(bits);
}

void main()
{
    vec3 N = normalize(vNormal);
    vec3 L = normalize(lightPosition.xyz - vPosition * lightPosition.w);
    vec3 diff = vColor.rgb * max(dot(N, L), 0.0);
    vec3 color = min(diff, vec3(1.0));

#if 1
//    float depth = vLgPosition.z / vLgPosition.w * 0.5 + 0.5;
    float ndc_z = vLgPosition.z / vLgPosition.w;
    float z_eye = 2.0 * near * far / (near + far - ndc_z * (far - near));
    float depth = (z_eye - near) / (far - near);
#else
    float depth = length(vPosition.xyz - lightPosition.xyz) / (far - near);
#endif
    float bias = 0.0;
    float NdotL = dot(N, L);
    if (NdotL < 0.0)
    { // add bias only at back faces
        bias = max(1e-2 * sqrt(max(0.0, 1.0 / (NdotL * NdotL) - 1.0)), 1e-3);
    }
    float shadow = 0.0;
#if 1
    for (int ix = -1; ix <= 1; ix++)
    {
        for (int iy = -1; iy <= 1; iy++)
        {
            float refDepth = decodeFloat(texture(depthBuffer, vLgTexCoord.st / vLgTexCoord.w + texOffset * vec2(ix, iy)));
            if (depth - refDepth > bias)
            {
                shadow += pcfKernel[ix + 1][iy + 1];
            }
        }
    }
#else
    float refDepth = decodeFloat(texture(depthBuffer, vLgTexCoord.st / vLgTexCoord.w));
    if (depth - refDepth > bias)
    {
        shadow = 1.0;
    }
#endif
    color = mix(color, shadowColor, shadow);

    gl_FragColor = vec4(color, vColor.a);
}
