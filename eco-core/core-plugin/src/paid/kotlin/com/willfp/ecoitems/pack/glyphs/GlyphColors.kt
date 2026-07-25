package com.willfp.ecoitems.pack.glyphs

import com.willfp.ecoitems.glyphs.GlyphAnimation

/**
 * The "magic" text colors that mark animated glyph frames for the shader:
 * R is always 0xFE, G encodes the loop flag and fps, B encodes the frame
 * index and total frame count.
 */
object GlyphColors {
    fun greenChannel(animation: GlyphAnimation): Int =
        (if (animation.loop) 0 else 0x80) or animation.fps

    fun blueChannel(animation: GlyphAnimation, frameIndex: Int): Int =
        frameIndex or ((animation.frames - 1) shl 4)

    /** The full RGB for a frame, e.g. 0xFE1050. */
    fun rgb(animation: GlyphAnimation, frameIndex: Int): Int =
        (0xFE shl 16) or (greenChannel(animation) shl 8) or blueChannel(animation, frameIndex)
}
