package com.willfp.ecoitems.pack.glyphs

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.placeholder.RegistrablePlaceholder
import com.willfp.eco.core.placeholder.context.PlaceholderContext
import com.willfp.ecoitems.plugin as ecoItemsPlugin
import java.util.regex.Pattern

/** `%ecoitems_glyph_<id>%` - the glyph's characters (legacy-colored for animated). */
object GlyphPlaceholder : RegistrablePlaceholder {
    private val pattern = Pattern.compile("glyph_([a-z0-9_]+)")

    override fun getPattern(): Pattern = pattern

    // Aliased import: a bare `plugin` here resolves to the interface's own
    // getPlugin() and recurses.
    override fun getPlugin(): EcoPlugin = ecoItemsPlugin

    override fun getValue(params: String, ctx: PlaceholderContext): String? {
        val matcher = pattern.matcher(params)
        if (!matcher.matches()) {
            return null
        }

        val assigned = GlyphText.assignments[matcher.group(1)] ?: return null
        return if (assigned.glyph.isAnimated) {
            GlyphText.legacyChars(assigned, restore = "§r")
        } else {
            GlyphText.rawChars(assigned)
        }
    }
}

/** `%ecoitems_shift_<pixels>%` - shift characters, e.g. %ecoitems_shift_-12%. */
object ShiftPlaceholder : RegistrablePlaceholder {
    private val pattern = Pattern.compile("shift_(-?\\d+)")

    override fun getPattern(): Pattern = pattern

    override fun getPlugin(): EcoPlugin = ecoItemsPlugin

    override fun getValue(params: String, ctx: PlaceholderContext): String? {
        val matcher = pattern.matcher(params)
        if (!matcher.matches()) {
            return null
        }

        val pixels = matcher.group(1).toIntOrNull() ?: return null
        return ShiftChars.shift(pixels)
    }
}
