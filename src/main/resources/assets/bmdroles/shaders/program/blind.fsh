#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;

in vec2 texCoord;

out vec4 fragColor;

// "Blind" look: low-resolution, grayscale, coarse brightness steps, dark grid lines
// between the cells and a soft vignette. Textures are unrecognisable, only shapes/light remain.
void main() {
    // Cell size scales with screen height so it looks the same at any resolution/GUI scale.
    float cell = max(InSize.y / 54.0, 6.0);

    vec2 px = texCoord * InSize;
    vec2 cellId = floor(px / cell);
    vec2 sampleUv = (cellId + 0.5) * cell / InSize;

    vec3 c = texture(DiffuseSampler, sampleUv).rgb;
    float g = dot(c, vec3(0.299, 0.587, 0.114));

    g = pow(g, 0.75);                 // lift dark areas so the picture never collapses to black
    g = floor(g * 6.0 + 0.5) / 6.0;   // coarse brightness levels (semi-visual)

    // grid lines between cells
    vec2 f = fract(px / cell);
    float edge = min(min(f.x, 1.0 - f.x), min(f.y, 1.0 - f.y));
    float gridLine = smoothstep(0.0, 0.12, edge);
    g *= mix(0.6, 1.0, gridLine);

    // soft vignette
    vec2 uv = texCoord - 0.5;
    float vignette = clamp(1.0 - dot(uv, uv) * 0.8, 0.0, 1.0);

    fragColor = vec4(vec3(g * vignette), 1.0);
}
