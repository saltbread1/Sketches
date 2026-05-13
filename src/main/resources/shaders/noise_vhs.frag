uniform sampler2D texture;
uniform vec2 texOffset;

varying vec4 vertTexCoord;

float random(float n)
{
    return fract(sin(n) * 43758.5453);
}

float random2(vec2 p)
{
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

float noise(vec2 v)
{
    vec2 i = floor(v);
    vec2 f = fract(v);
    vec2 u = f * f * (3. - 2. * f);
    return mix(mix(random2(i + vec2(0., 0.)),
                   random2(i + vec2(1., 0.)), u.x),
               mix(random2(i + vec2(0., 1.)),
                   random2(i + vec2(1., 1.)), u.x), u.y);
}

void main()
{
    vec2 uv = vertTexCoord.st;

    // scan line noise
    float lineId = floor(uv.y / texOffset.y);
    float lineNoise = random(lineId);
    uv.x += lineNoise * 0.008;

    vec3 col = texture2D(texture, uv).rgb;
    col = col * 0.8 + 0.1; // degrade luminance
    col = floor(col * 64.) / 64.; // degrade contrast
    float luma = dot(col, vec3(0.299, 0.587, 0.114));
    col = mix(vec3(luma), col, 2.9); // improve saturate

    // chrominance noise
    float cr = noise(uv / texOffset * vec2(0.12, 0.07)) * 0.15;
    float cb = noise(uv / texOffset * vec2(0.12, 0.07) + vec2(100.0)) * 0.15;
    col.r += cr - cb * 0.3;
    col.b += cb - cr * 0.3;

    // particle noise
    float grain = random2(uv / texOffset) * 0.11;
    col += grain;

    // tape speed variation
    float luma_wave = sin(uv.y / texOffset.y * 0.5) * 0.04;
    col += luma_wave;

    gl_FragColor = vec4(clamp(col, 0.0, 1.0), 1.0);
}
