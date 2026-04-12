#version 150

uniform sampler2D Sampler0;
uniform vec2 CanvasSize;      // canvas width, height in pixels
uniform vec2 CanvasOrigin;    // canvas top-left in screen pixels
uniform vec2 Scroll;          // scrollX, scrollY (canvas coords)
uniform float Zoom;           // current zoom

in vec2 texCoord;
out vec4 fragColor;

#define PI 3.14159265358979

// How many times to check for nearby spiral arm
#define ARMS 1
#define GROWTH 18.0           // pixels per radian at zoom=1
#define THICKNESS_BASE 1.2
#define THICKNESS_GROW 4.0    // extra px at max radius

void main() {
    // Convert fragment to canvas-space coordinates (same as canvasToScreen math)
    vec2 fragScreen = gl_FragCoord.xy;
    // gl_FragCoord is bottom-up, flip Y
    fragScreen.y = CanvasOrigin.y + CanvasSize.y - (fragScreen.y - CanvasOrigin.y);

    // Canvas coordinate = (screen - canvasCenter) / zoom + scroll
    vec2 canvasCenter = CanvasOrigin + CanvasSize * 0.5;
    vec2 canvasPos = (fragScreen - canvasCenter) / Zoom + Scroll;

    // Polar coords relative to spiral origin (0,0)
    float dist = length(canvasPos);
    float angle = atan(canvasPos.y, canvasPos.x);

    // Find how close we are to the nearest spiral arm.
    // The arm at angle theta has radius r = GROWTH * theta.
    // So for our distance, the arm passed through here at theta = dist / GROWTH.
    // We check the nearest winding: theta modulo 2*PI
    float theta = dist / GROWTH;
    float thetaMod = mod(theta, 2.0 * PI);
    float angleMod = mod(angle, 2.0 * PI);

    // Angular distance to nearest arm (0..PI)
    float dAngle = abs(thetaMod - angleMod);
    if (dAngle > PI) dAngle = 2.0 * PI - dAngle;

    // Convert angular difference to pixel distance at this radius
    // Arc length = r * dAngle
    float arcDist = dist * dAngle;

    // Thickness grows with distance from center
    float tNorm = clamp(dist / (GROWTH * 40.0), 0.0, 1.0);
    float thickness = (THICKNESS_BASE + THICKNESS_GROW * tNorm) * Zoom;

    // Soft edge falloff
    float edge = 1.0 - smoothstep(thickness * 0.4, thickness, arcDist);

    if (edge < 0.01) {
        fragColor = vec4(0.2, 0.2, 0.2, 1.0); // background
        return;
    }

    // Color gradient along the spiral arm based on tNorm
    // Palette: dark purple → magenta → pink → grey
    vec3 c0 = vec3(0.533, 0.267, 0.667); // #8844AA
    vec3 c1 = vec3(0.800, 0.400, 1.000); // #CC66FF
    vec3 c2 = vec3(0.933, 0.667, 0.800); // #EEAACC
    vec3 c3 = vec3(0.667, 0.667, 0.667); // #AAAAAA

    vec3 col;
    float t = tNorm * 3.0;
    if (t < 1.0)      col = mix(c0, c1, t);
    else if (t < 2.0) col = mix(c1, c2, t - 1.0);
    else               col = mix(c2, c3, t - 2.0);

    // Dither: ordered 2x2 Bayer to keep the pixelated feel
    int bx = int(mod(gl_FragCoord.x, 2.0));
    int by = int(mod(gl_FragCoord.y, 2.0));
    float bayer;
    if      (bx == 0 && by == 0) bayer = 0.0 / 4.0;
    else if (bx == 1 && by == 0) bayer = 2.0 / 4.0;
    else if (bx == 0 && by == 1) bayer = 3.0 / 4.0;
    else                          bayer = 1.0 / 4.0;
    col = floor(col * 8.0 + bayer) / 8.0; // posterize with dither

    vec3 bg = vec3(0.2, 0.2, 0.2);
    fragColor = vec4(mix(bg, col, edge), 1.0);
}