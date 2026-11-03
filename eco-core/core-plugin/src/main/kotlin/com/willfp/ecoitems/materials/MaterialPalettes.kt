package com.willfp.ecoitems.materials

/**
 * The greyscale ramp part textures are drawn with. The resource pack swaps each grey for the
 * material's palette colour at the same position, like vanilla armor trims.
 */
object MaterialPalettes {
    /**
     * The 8 greys (0-255 per channel) part textures must be drawn with, darkest to lightest. They are
     * spaced roughly evenly across the range so shading reads clearly, and a material's palette maps
     * onto them in the same order. Changing them breaks every existing part texture.
     */
    val RAMP = listOf(34, 62, 92, 122, 152, 186, 220, 250)

    private const val CHANNEL_MAX = 255
    private const val CHANNEL_BITS = 8

    private val CHANNEL_SHIFTS = listOf(16, 8, 0)

    /**
     * For a generated palette, how much of the material colour each ramp step keeps: the darkest step
     * is the colour at 22% brightness, the sixth step is the colour itself.
     */
    val GENERATED_BRIGHTNESS = listOf(0.22, 0.38, 0.54, 0.7, 0.85, 1.0, 1.0, 1.0)

    /** How far each generated step is then mixed towards white: the top two become 30% and 60% lighter highlights. */
    val GENERATED_HIGHLIGHT = listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.3, 0.6)

    /** A palette from one colour: darker shades below it, lighter tints above. */
    fun generate(color: Int): List<Int> =
        GENERATED_BRIGHTNESS.zip(GENERATED_HIGHLIGHT) { shade, lighten -> shadeOf(color, shade, lighten) }

    private fun shadeOf(color: Int, shade: Double, lighten: Double): Int = CHANNEL_SHIFTS.fold(0) { rgb, shift ->
        val shaded = (color shr shift and CHANNEL_MAX) * shade
        rgb shl CHANNEL_BITS or (shaded + (CHANNEL_MAX - shaded) * lighten)
            .toInt()
            .coerceIn(minimumValue = 0, maximumValue = CHANNEL_MAX)
    }
}
