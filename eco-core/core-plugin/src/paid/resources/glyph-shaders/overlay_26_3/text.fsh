#version 330
#extension GL_ARB_separate_shader_objects : require

// EcoItems animated glyph shader.
// Frames of an animated glyph are stacked at the same position; each frame
// is tagged with a magic text color (R=254, G=loop|fps, B=frame|count) and
// this shader shows only the frame matching the current GameTime.
//
// 26.3 compiles shaders with real #include (no #moj_import), matches stages
// by explicit location, and renders text through order-independent
// transparency passes, so the output side mirrors vanilla's text.fsh.

#if !defined(IS_SEE_THROUGH) && !defined(IS_GUI)
#include <minecraft:fog.glsl>
#endif

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:oit.glsl>

uniform sampler2D Sampler0;

#if !defined(IS_SEE_THROUGH) && !defined(IS_GUI)
layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
#endif

layout(location = 2) in vec4 vertexColor;
layout(location = 3) in vec2 texCoord0;

#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

vec4 ecoitems_sample_text() {
    vec4 texel = texture(Sampler0, texCoord0);
#ifdef IS_GRAYSCALE
    return texel.rrrr;
#else
    return texel;
#endif
}

vec4 ecoitems_final_color(vec4 color) {
#ifdef OIT_ACCUMULATE
    color = sampleColorForAccumulation(color);
#endif

#if !defined(IS_SEE_THROUGH) && !defined(IS_GUI)
#ifdef OIT_ACCUMULATE
    vec4 fogColor = vec4(FogColor.rgb * color.a, FogColor.a);
#else
    vec4 fogColor = FogColor;
#endif

    color = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, fogColor);
#endif

    return color;
}

void main() {
    vec4 color = ecoitems_sample_text() * vertexColor * ColorModulator;

    if (color.a < 0.1) {
        discard;
    }

#ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
#else
    fragColor = ecoitems_final_color(color);
#endif
}
