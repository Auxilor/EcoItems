package com.willfp.ecoitems.pack

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/** Item textures stitch into the items atlas from 26.x and into the blocks atlas before it. */
internal val ATLASES = listOf("assets/minecraft/atlases/items.json", "assets/minecraft/atlases/blocks.json")

private const val PALETTE_KEY = "ecoitems:material_key"

/**
 * 26.3 loads atlas palettes from textures/palettes/, older clients from textures/, so every palette
 * is written to both.
 */
internal fun paletteEntries(palette: String): List<String> = listOf(
    "assets/ecoitems/textures/palettes/$palette.png",
    "assets/ecoitems/textures/$palette.png"
)

internal const val PALETTE_KEY_NAME = "material_key"

internal fun materialPaletteName(materialId: String): String = "material/$materialId"

internal fun permutationSource(textures: List<PackLocation>, materialIds: List<String>): JsonObject = jsonObject(
    "type" to "minecraft:paletted_permutations",
    "textures" to jsonArray(textures.map { it.key }),
    "palette_key" to PALETTE_KEY,
    "permutations" to permutations(materialIds)
)

private fun permutations(materialIds: List<String>): JsonObject {
    val json = JsonObject()
    materialIds.forEach { json.addProperty(it, "ecoitems:${materialPaletteName(it)}") }
    return json
}

internal fun withSource(existing: ByteArray?, source: JsonObject): ByteArray {
    val atlas = existing
        ?.let { runCatching { JsonParser.parseString(it.decodeToString()).asJsonObject }.getOrNull() }
        ?: JsonObject()
    val sources = atlas.getAsJsonArray("sources") ?: JsonArray().also { atlas.add("sources", it) }
    sources.add(source)
    return materialGson.toJson(atlas).encodeToByteArray()
}

internal fun recolouredEntry(texture: PackLocation, materialId: String): String =
    "assets/${texture.namespace}/models/${texture.path}_$materialId.json"

internal fun modelJson(texture: String): ByteArray = materialGson.toJson(
    jsonObject("parent" to "minecraft:item/handheld", "textures" to jsonObject("layer0" to texture))
).encodeToByteArray()
