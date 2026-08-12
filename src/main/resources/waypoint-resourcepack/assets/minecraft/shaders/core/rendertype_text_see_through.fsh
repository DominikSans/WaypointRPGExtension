#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;
flat in float waypointRpgText;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;
    if (color.a < 0.1) {
        discard;
    }

    if (waypointRpgText > 0.5) {
        color.a = 1.0;
        gl_FragDepth = 0.00005;
    }
    fragColor = color * ColorModulator;
}
