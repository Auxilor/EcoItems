// EcoItems animated glyph shader.
// Frames of an animated glyph are stacked at the same position; each frame
// is tagged with a magic text color (R=254, G=loop|fps, B=frame|count) and
// this shader shows only the frame matching the current GameTime.

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

%ANIM_CONFIGS%

void main() {
    vec3 pos = Position;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);
    texCoord0 = UV0;
    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);

    int rInt = int(Color.r * 255.0 + 0.5);
    int gRaw = int(Color.g * 255.0 + 0.5);
    int bRaw = int(Color.b * 255.0 + 0.5);

    // Only detect the animation marker on the primary pass: shadow-pass
    // colors are divided by 4 and would false-positive on vanilla shadows.
    // R=254 alone can occur in gradients, so the fps/loop and frame-count
    // tuple must also match an actually configured animated glyph.
    bool isPrimaryAnim = false;
    if (ECOITEMS_ANIM_CONFIG_COUNT > 0 && rInt == 254) {
        int candidateFrameIndex = bRaw & 0x0F;
        int candidateTotalFrames = ((bRaw >> 4) & 0x0F) + 1;
        if (candidateFrameIndex < candidateTotalFrames) {
            for (int i = 0; i < ECOITEMS_ANIM_CONFIG_COUNT; i++) {
                if (gRaw == ECOITEMS_ANIM_CONFIGS[i].x && candidateTotalFrames == ECOITEMS_ANIM_CONFIGS[i].y) {
                    isPrimaryAnim = true;
                    break;
                }
            }
        }
    }

    if (isPrimaryAnim) {
        bool loop = (gRaw < 128);
        float fps = max(1.0, float(gRaw & 0x7F));
        int frameIndex = bRaw & 0x0F;
        int totalFrames = ((bRaw >> 4) & 0x0F) + 1;

        float timeSeconds = (GameTime <= 1.0) ? (GameTime * 1200.0) : (GameTime / 20.0);
        int rawFrame = int(floor(timeSeconds * fps));
        int currentFrame = loop ? (rawFrame % totalFrames) : min(rawFrame, totalFrames - 1);

        float visible = (frameIndex == currentFrame) ? 1.0 : 0.0;

        vertexColor = vec4(1.0, 1.0, 1.0, visible) * texelFetch(Sampler2, UV2 / 16, 0);
    }
}
