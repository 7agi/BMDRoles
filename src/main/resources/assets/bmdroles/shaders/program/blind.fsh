#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

// "Blind" look: low-resolution, grayscale, coarse brightness steps, dark grid lines
// between the cells and a vignette. Textures are unrecognisable, only shapes/light remain.
void main() {
    // Cell size scales with screen height so it looks the same at any resolution/GUI scale.
    float cell = max(InSize.y / 54.0, 6.0);

    vec2 px = texCoord * InSize;
    vec2 cellId = floor(px / cell);
    vec2 sampleUv = (cellId + 0.5) * cell / InSize;

    vec3 c = texture(DiffuseSampler, sampleUv).rgb;
    float g = dot(c, vec3(0.299, 0.587, 0.114));

    // coarse brightness levels (semi-visual)
    g = floor(g * 5.0 + 0.5) / 5.0;

    // grid lines between cells
    vec2 f = fract(px / cell);
    float edge = min(min(f.x, 1.0 - f.x), min(f.y, 1.0 - f.y));
    float gridLine = smoothstep(0.0, 0.14, edge);
    g *= mix(0.45, 1.0, gridLine);

    // vignette
    vec2 uv = texCoord - 0.5;
    float vignette = clamp(1.0 - dot(uv, uv) * 1.2, 0.0, 1.0);

    fragColor = vec4(vec3(g * vignette), 1.0);
}
