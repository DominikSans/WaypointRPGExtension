#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <minecraft:globals.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler2;

out float sphericalVertexDistance;
out float cylindricalVertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
flat out float waypointRpgText;

const float WAYPOINT_RPG_OPACITY = 252.0 / 255.0;
// Vanilla/Sodium may quantize TextDisplay opacity by one byte while building
// text vertices. Accept 251..253 for the 252 marker, while still excluding 255.
const float BYTE_TOLERANCE = 1.5 / 255.0;
const float WAYPOINT_DEPTH_BIAS = 0.0002;

const float SRPG_FOV = 100.0;
const float SRPG_INV_TAN_HALF_FOV = 1.0 / tan(radians(SRPG_FOV * 0.5));

mat4 srpg_force_fov(mat4 projection) {
    if (projection[2][3] != 0.0) {
        float aspectInv = projection[0][0] / projection[1][1];
        projection[0][0] = SRPG_INV_TAN_HALF_FOV * aspectInv;
        projection[1][1] = SRPG_INV_TAN_HALF_FOV;
    }
    return projection;
}

void main() {
    waypointRpgText = abs(Color.a - WAYPOINT_RPG_OPACITY) <= BYTE_TOLERANCE ? 1.0 : 0.0;

    mat4 projection = GameTime < 0.0 ? srpg_force_fov(ProjMat) : ProjMat;
    gl_Position = projection * ModelViewMat * vec4(Position, 1.0);
    if (waypointRpgText > 0.5) {
        gl_Position.z -= WAYPOINT_DEPTH_BIAS * gl_Position.w;
    }

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
    texCoord0 = UV0;
}
