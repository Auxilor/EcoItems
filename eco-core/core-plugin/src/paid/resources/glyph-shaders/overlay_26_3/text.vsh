#version 330
#extension GL_ARB_separate_shader_objects : require

// EcoItems animated glyph shader.
// Frames of an animated glyph are stacked at the same position; each frame
// is tagged with a magic text color (R=254, G=loop|fps, B=frame|count) and
// this shader shows only the frame matching the current GameTime.
//
// 26.3 compiles shaders with real #include (no #moj_import) and matches
// stages by explicit location, so every in/out mirrors vanilla's text.vsh.

#if !defined(IS_SEE_THROUGH) && !defined(IS_GUI)
#include <minecraft:fog.glsl>
#include <minecraft:sample_lightmap.glsl>
#endif

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:globals.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;

#if !defined(IS_SEE_THROUGH) && !defined(IS_GUI)
layout(location = 3) in ivec2 UV2;
uniform sampler2D Sampler2;
layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
#endif

layout(location = 2) out vec4 vertexColor;
layout(location = 3) out vec2 texCoord0;

%ANIM_CONFIGS%

vec4 ecoitems_lit_text_color(vec4 color) {
#if defined(IS_SEE_THROUGH) || defined(IS_GUI)
    return color;
#else
    return color * sample_lightmap(Sampler2, UV2);
#endif
}

void main() {
    vec3 pos = Position;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
#if !defined(IS_SEE_THROUGH) && !defined(IS_GUI)
    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);
#endif
    texCoord0 = UV0;
    vertexColor = ecoitems_lit_text_color(Color);

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

        vertexColor = ecoitems_lit_text_color(vec4(1.0, 1.0, 1.0, visible));
    }
}
