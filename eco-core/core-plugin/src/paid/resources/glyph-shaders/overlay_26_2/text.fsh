// EcoItems animated glyph shader.
// Frames of an animated glyph are stacked at the same position; each frame
// is tagged with a magic text color (R=254, G=loop|fps, B=frame|count) and
// this shader shows only the frame matching the current GameTime.

#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:globals.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

vec4 ecoitems_sample_text() {
    vec4 texel = texture(Sampler0, texCoord0);
#ifdef IS_GRAYSCALE
    return texel.rrrr;
#else
    return texel;
#endif
}

void main() {
    vec4 color = ecoitems_sample_text() * vertexColor * ColorModulator;

    if (color.a < 0.1) {
        discard;
    }
#if defined(IS_SEE_THROUGH) || defined(IS_GUI)
    fragColor = color;
#else
    fragColor = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
#endif
}
