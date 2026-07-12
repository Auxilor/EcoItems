package com.willfp.ecoitems.pack.glyphs

import kotlin.math.abs

/**
 * The pixel-shift characters: a vanilla `space` font provider with
 * powers-of-two advances, composed by binary decomposition. Injected into
 * minecraft:default and the standalone ecoitems:shift font.
 *
 * Fixed private-use-area codepoints, deliberately disjoint from Oraxen's
 * 0xF800 range so merged packs can coexist.
 */
object ShiftChars {
    private const val BASE = 0xF850
    private const val MAX_POWER = 10

    const val MAX_SHIFT = (1 shl (MAX_POWER + 1)) - 1 // 2047

    /** Codepoint -> advance in pixels. */
    val advances: Map<Int, Int> = buildMap {
        for (power in 0..MAX_POWER) {
            put(BASE + power, 1 shl power)
            put(BASE + MAX_POWER + 1 + power, -(1 shl power))
        }
    }

    /** The characters that shift by [pixels] (clamped to +/-2047). */
    fun shift(pixels: Int): String {
        if (pixels == 0) {
            return ""
        }

        val magnitude = abs(pixels).coerceAtMost(MAX_SHIFT)
        val negativeOffset = if (pixels < 0) MAX_POWER + 1 else 0

        val builder = StringBuilder()
        for (power in MAX_POWER downTo 0) {
            if (magnitude and (1 shl power) != 0) {
                builder.appendCodePoint(BASE + negativeOffset + power)
            }
        }

        return builder.toString()
    }
}
