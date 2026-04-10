#version 150

uniform sampler2D Sampler0;
uniform vec4 TintColor;

in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 tex = texture(Sampler0, texCoord);

    if (tex.a < 0.01) discard;

    // Check for "Pure" Black (all channels near 0)
    bool isPureBlack = (tex.r < 0.01 && tex.g < 0.01 && tex.b < 0.01);

    // Check for "Pure" White (all channels near 1)
    bool isPureWhite = (tex.r > 0.99 && tex.g > 0.99 && tex.b > 0.99);

    vec3 finalColor;

    if (isPureBlack || isPureWhite) {
        // Keep the original color for pure black/white
        finalColor = tex.rgb;
    } else {
        // Apply tint logic to everything else
        float brightness = dot(tex.rgb, vec3(0.299, 0.587, 0.114));
        finalColor = mix(tex.rgb, TintColor.rgb * brightness * 2.0, TintColor.a);
    }

    fragColor = vec4(finalColor, tex.a);
}