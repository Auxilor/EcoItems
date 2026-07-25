package com.willfp.ecoitems.pack.huds

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.huds.Hud
import com.willfp.ecoitems.pack.glyphs.AssignedGlyph
import com.willfp.ecoitems.pack.glyphs.GlyphAssetGenerator

/**
 * Generates one font per offset HUD (ecoitems:hud/<id>): the vanilla ASCII
 * page re-declared at the HUD's text-ascent, plus every glyph provider (at
 * their own ascents) and the shift space provider. Applying this single font
 * to the HUD component gives raised text with working glyphs and shifts.
 */
object HudFontGenerator {
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    private const val PAD = '\u0000'

    // The vanilla ascii.png code-page-437 layout (the same grid the client
    // uses). \u0000 cells are empty - they must not be spaces, which would
    // redefine the space character's width from the bitmap.
    private val ASCII_CHARS = listOf(
        "${PAD}".repeat(16),
        "${PAD}".repeat(16),
        " !\"#\$%&'()*+,-./",
        "0123456789:;<=>?",
        "@ABCDEFGHIJKLMNO",
        "PQRSTUVWXYZ[\\]^_",
        "`abcdefghijklmno",
        "pqrstuvwxyz{|}~${PAD}",
        "${PAD}".repeat(16),
        "${PAD}".repeat(12) + "£${PAD}${PAD}ƒ",
        "${PAD}".repeat(6) + "ªº${PAD}${PAD}¬${PAD}${PAD}$PAD«»",
        "░▒▓│┤╡╢╖╕╣║╗╝╜╛┐",
        "└┴┬├─┼╞╟╚╔╩╦╠═╬╧",
        "╨╤╥╙╘╒╓╫╪┘┌█▄▌▐▀",
        "${PAD}".repeat(13) + "∅∈${PAD}",
        "≡±≥≤⌠⌡÷≈°∙${PAD}√ⁿ²■${PAD}"
    )

    fun generate(
        plugin: EcoItemsPlugin,
        huds: Collection<Hud>,
        glyphs: Collection<AssignedGlyph>,
        entries: MutableMap<String, ByteArray>
    ) {
        for (hud in huds.sortedBy { it.id }) {
            val configured = hud.textAscent ?: continue

            // The client requires ascent <= height (8 for the ascii page).
            val ascent = configured.coerceAtMost(8).also {
                if (it != configured) {
                    plugin.logger.warning("HUD ${hud.id} has text-ascent $configured, clamped to 8 (the maximum)")
                }
            }

            val ascii = JsonObject()
            ascii.addProperty("type", "bitmap")
            ascii.addProperty("file", "minecraft:font/ascii.png")
            ascii.addProperty("ascent", ascent)
            ascii.addProperty("height", 8)
            val chars = JsonArray()
            ASCII_CHARS.forEach { chars.add(it) }
            ascii.add("chars", chars)

            val (glyphProviders, spaceProvider) = GlyphAssetGenerator.buildProviders(plugin, glyphs)

            val providers = JsonArray()
            providers.add(ascii)
            glyphProviders.forEach { providers.add(it) }
            providers.add(spaceProvider)

            val font = JsonObject()
            font.add("providers", providers)
            entries["assets/ecoitems/font/hud/${hud.id}.json"] = gson.toJson(font).encodeToByteArray()
        }
    }
}
