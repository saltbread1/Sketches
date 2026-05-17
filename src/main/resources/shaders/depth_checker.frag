precision highp float;

uniform sampler2D depthBuffer;

varying vec4 vertTexCoord;

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
    float depth = decodeFloat(texture(depthBuffer, vertTexCoord.st));
    gl_FragColor = vec4(vec3(depth), 1.0);
}
