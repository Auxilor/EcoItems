package com.willfp.ecoitems.materials

/**
 * The greyscale ramp part textures are drawn with. The resource pack swaps each grey for the
 * material's palette colour at the same position, like vanilla armor trims.
 */
object MaterialPalettes {
    val RAMP = listOf(34, 62, 92, 122, 152, 186, 220, 250)

    private const val CHANNEL_MAX = 255
    private const val CHANNEL_BITS = 8

    private val CHANNEL_SHIFTS = listOf(16, 8, 0)
    private val SHADES = listOf(0.22, 0.38, 0.54, 0.7, 0.85, 1.0, 1.0, 1.0)
    private val LIGHTEN = listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.3, 0.6)

    /** A palette from one colour: darker shades below it, lighter tints above. */
    fun generate(color: Int): List<Int> = SHADES.zip(LIGHTEN) { shade, lighten -> shadeOf(color, shade, lighten) }

    private fun shadeOf(color: Int, shade: Double, lighten: Double): Int = CHANNEL_SHIFTS.fold(0) { rgb, shift ->
        val shaded = (color shr shift and CHANNEL_MAX) * shade
        rgb shl CHANNEL_BITS or (shaded + (CHANNEL_MAX - shaded) * lighten)
            .toInt()
            .coerceIn(minimumValue = 0, maximumValue = CHANNEL_MAX)
    }
}
