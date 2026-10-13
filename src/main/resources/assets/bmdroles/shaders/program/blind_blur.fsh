#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform vec2 BlurDir;
uniform float Radius;
uniform float Desaturate;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

// One direction of a separable gaussian blur (easy mode "blind").
void main() {
    vec2 stepVec = BlurDir / InSize;
    float sigma = max(Radius * 0.5, 0.5);

    vec3 sum = vec3(0.0);
    float total = 0.0;
    for (float i = -Radius; i <= Radius; i += 1.0) {
        float w = exp(-0.5 * (i * i) / (sigma * sigma));
        sum += texture(DiffuseSampler, texCoord + stepVec * i).rgb * w;
        total += w;
    }

    vec3 c = sum / total;
    float g = dot(c, vec3(0.299, 0.587, 0.114));
    fragColor = vec4(mix(c, vec3(g), Desaturate), 1.0);
}
