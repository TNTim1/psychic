#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;

    // Discard completely transparent pixels (standard for entities/GUI)
    if (color.a < 0.1) {
        discard;
    }

    // Calculate Grayscale (Luminance)
    float gray = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));

    // Set RGB to the gray value and multiply Alpha for transparency
    // Adjust 0.6 to your preferred transparency level (0.0 to 1.0)
    fragColor = vec4(vec3(gray), color.a * 0.6);
}