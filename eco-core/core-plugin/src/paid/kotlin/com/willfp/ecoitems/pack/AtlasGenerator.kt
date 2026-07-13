package com.willfp.ecoitems.pack

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.willfp.ecoitems.EcoItemsPlugin

/**
 * Generates atlas directory sources for custom texture roots, so textures
 * anywhere under pack/assets/<ns>/textures/ stitch into the block atlas
 * without users writing atlas files. Roots vanilla already stitches (item/,
 * block/) and non-atlas roots (fonts, equipment, paintings, guis) are left
 * alone. A user atlas file in the pack folder merges over this.
 */
object AtlasGenerator {
    private const val ATLAS = "assets/minecraft/atlases/blocks.json"
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    private val SPECIAL_ROOTS = setOf(
        "item", "block", "entity", "painting", "font", "glyph", "gui",
        "colormap", "environment", "particle", "mob_effect", "map", "misc", "trims"
    )

    fun generate(plugin: EcoItemsPlugin, entries: MutableMap<String, ByteArray>) {
        val roots = sortedSetOf<String>()

        // Texture roots from the pack folder and from anything already
        // assembled (imports, migrated content).
        val assets = plugin.dataFolder.resolve("pack/assets")
        for (namespace in assets.listFiles().orEmpty().filter { it.isDirectory }) {
            for (root in namespace.resolve("textures").listFiles().orEmpty().filter { it.isDirectory }) {
                roots += root.name
            }
        }
        for (path in entries.keys) {
            val match = Regex("assets/[^/]+/textures/([^/]+)/.+").matchEntire(path) ?: continue
            roots += match.groupValues[1]
        }

        roots.removeAll(SPECIAL_ROOTS)

        val existing = entries[ATLAS]?.let {
            runCatching { JsonParser.parseString(it.decodeToString()).asJsonObject }.getOrNull()
        }
        if (roots.isEmpty() && existing == null) {
            return
        }

        val sources = JsonArray()
        val present = mutableSetOf<String>()

        existing?.getAsJsonArray("sources")?.forEach { source ->
            sources.add(source)
            (source as? JsonObject)?.get("source")?.asString?.let { present += it }
        }

        for (root in roots) {
            if (root in present) continue
            val source = JsonObject()
            source.addProperty("type", "directory")
            source.addProperty("source", root)
            source.addProperty("prefix", "$root/")
            sources.add(source)
        }

        val json = JsonObject()
        json.add("sources", sources)
        entries[ATLAS] = gson.toJson(json).encodeToByteArray()
    }
}
