package com.willfp.ecoitems.pack

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.security.MessageDigest

/**
 * Anti-rip: renames the pack's ecoitems textures and models to hashed
 * names and rewrites every reference. Item definitions, sound events,
 * equipment assets, and painting textures keep their names - items in
 * circulation and datapack registrations point at those - so the glue
 * stays readable while the actual art doesn't.
 *
 * Hashes derive from the original paths, so unchanged packs still zip to
 * the same SHA-1.
 */
object PackObfuscator {
    private val gson = Gson()

    // Looked up by convention (equipment layers, painting variants), not by
    // an explicit reference we could rewrite.
    private val UNTOUCHABLE_TEXTURES = listOf(
        "assets/ecoitems/textures/entity/",
        "assets/ecoitems/textures/painting/"
    )

    fun obfuscate(entries: MutableMap<String, ByteArray>) {
        val textures = mutableMapOf<String, String>() // "ecoitems:path" -> "ecoitems:x/hash"
        val models = mutableMapOf<String, String>()

        for (path in entries.keys.toList()) {
            when {
                path.startsWith("assets/ecoitems/textures/") &&
                    path.endsWith(".png") &&
                    UNTOUCHABLE_TEXTURES.none { path.startsWith(it) } -> {
                    val relative = path.removePrefix("assets/ecoitems/textures/").removeSuffix(".png")
                    val hashed = "x/${hash(relative)}"

                    move(entries, path, "assets/ecoitems/textures/$hashed.png")
                    move(entries, "$path.mcmeta", "assets/ecoitems/textures/$hashed.png.mcmeta")
                    textures["ecoitems:$relative"] = "ecoitems:$hashed"
                }

                path.startsWith("assets/ecoitems/models/") && path.endsWith(".json") -> {
                    val relative = path.removePrefix("assets/ecoitems/models/").removeSuffix(".json")
                    val hashed = "x/${hash(relative)}"

                    move(entries, path, "assets/ecoitems/models/$hashed.json")
                    models["ecoitems:$relative"] = "ecoitems:$hashed"
                }
            }
        }

        if (textures.isEmpty() && models.isEmpty()) {
            return
        }

        for (path in entries.keys.toList()) {
            if (!path.endsWith(".json") && !path.endsWith(".mcmeta")) {
                continue
            }

            runCatching {
                val parsed = JsonParser.parseString(entries.getValue(path).decodeToString())
                rewrite(parsed, textures, models, insideTextures = false)
                entries[path] = gson.toJson(parsed).encodeToByteArray()
            }
        }

        // The hashed texture folder still needs to stitch into the block atlas.
        appendAtlasSource(entries)
    }

    private fun move(entries: MutableMap<String, ByteArray>, from: String, to: String) {
        val content = entries.remove(from) ?: return
        entries[to] = content
    }

    private fun hash(path: String): String =
        MessageDigest.getInstance("SHA-1")
            .digest(path.encodeToByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)

    /**
     * Key-aware reference rewriting: `parent`/`model` values are model
     * locations, values inside a `textures` object and font `file` values
     * are texture locations. Everything else recurses.
     */
    private fun rewrite(
        element: JsonElement,
        textures: Map<String, String>,
        models: Map<String, String>,
        insideTextures: Boolean
    ) {
        when {
            element is JsonArray -> element.forEach { rewrite(it, textures, models, insideTextures) }

            element is JsonObject -> {
                for (key in element.keySet().toList()) {
                    val value = element.get(key)

                    if (value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                        val raw = value.asString
                        val replacement = when {
                            insideTextures -> textures[normalize(raw)]
                            key == "file" -> textures[normalize(raw).removeSuffix(".png")]?.plus(".png")
                            key == "parent" || key == "model" -> models[normalize(raw)]
                            else -> null
                        }
                        if (replacement != null) {
                            element.addProperty(key, replacement)
                        }
                    } else {
                        rewrite(value, textures, models, insideTextures || key == "textures")
                    }
                }
            }
        }
    }

    /** Model/texture refs may omit the namespace; ours never do, so add it back. */
    private fun normalize(reference: String): String =
        if (":" in reference) reference else "minecraft:$reference"

    private fun appendAtlasSource(entries: MutableMap<String, ByteArray>) {
        val path = "assets/minecraft/atlases/blocks.json"
        val atlas = entries[path]
            ?.let { runCatching { JsonParser.parseString(it.decodeToString()).asJsonObject }.getOrNull() }
            ?: JsonObject()

        val sources = atlas.getAsJsonArray("sources") ?: JsonArray().also { atlas.add("sources", it) }

        val source = JsonObject()
        source.addProperty("type", "directory")
        source.addProperty("source", "x")
        source.addProperty("prefix", "x/")
        sources.add(source)

        entries[path] = gson.toJson(atlas).encodeToByteArray()
    }
}
