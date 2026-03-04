uniform mat4 modelviewMatrix;
uniform mat4 transformMatrix;
uniform mat3 normalMatrix;

attribute vec4 position;
attribute vec3 normal;
attribute vec4 color;

varying vec3 vPosition;
varying vec3 vNormal;
varying vec2 vUV;
varying vec4 vColor;

const float PI = acos(-1.0);

void main()
{
    vPosition = (modelviewMatrix * position).xyz;
    vNormal = normalize(normalMatrix * normal);
    vColor = color;

    vec3 dir = normalize(position.xyz + normal * 0.08);
    float phi = atan( dir.x, dir.z );
    float theta = asin( dir.y );
    vUV = vec2(phi / (2.0 * PI) + 0.5, theta / PI + 0.5);

    gl_Position = transformMatrix * position;
}
