package com.willfp.ecoitems.pack

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.items.EcoItem
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.materials.MaterialPalettes
import com.willfp.ecoitems.materials.Materials
import com.willfp.ecoitems.materials.partItemForShape

/**
 * Generates the look of parts and modular items, the armor trim way: one greyscale texture per part
 * shape, recoloured per material by a paletted_permutations atlas source. Writes the palette key and
 * material palettes, the atlas source, a model per shape and material, and the item definitions,
 * which select each layer's model on the material id stored in custom_model_data.
 *
 * Runs before the pack folder is merged, so user files still win.
 */
object MaterialAssetGenerator {
    private const val ITEMS_ATLAS = "assets/minecraft/atlases/items.json"
    private const val PALETTE_KEY = "ecoitems:material_key"
    private const val PALETTE_KEY_ENTRY = "assets/ecoitems/textures/palettes/material_key.png"


    fun generate(plugin: EcoItemsPlugin, entries: MutableMap<String, ByteArray>) {
        val materialIds = Materials.values().map { it.getID() }
        val textures = EcoItems.values().mapNotNull { it.partTexture() }.distinct()

        if (materialIds.isEmpty() || textures.isEmpty()) {
            return
        }

        val materials = Materials.values()
        val items = EcoItems.values()
        textures.forEach { warnAboutStrayGreys(plugin, it) }

        entries[PALETTE_KEY_ENTRY] = paletteImage(MaterialPalettes.RAMP.map { it * GREY_STEP })
        for (material in materials) {
            entries[paletteEntry(material.getID())] = paletteImage(material.palette)
        }

        entries[ITEMS_ATLAS] = withSource(
            existing = entries[ITEMS_ATLAS],
            source = permutationSource(textures, materialIds)
        )

        val overlays = items.flatMap { it.overlayTextures() } +
            materials.flatMap { material -> material.allOverlays.mapNotNull { PackLocation.parse(it) } }
        for (overlay in overlays.distinct()) {
            warnIfMissing(plugin, overlay)
            entries.putIfAbsent(overlay.entry(root = "models", extension = "json"), modelJson(overlay.key))
        }

        for (texture in textures) {
            entries.putIfAbsent(texture.entry(root = "models", extension = "json"), modelJson(texture.key))
            for (id in materialIds) {
                entries.putIfAbsent(recolouredEntry(texture, id), modelJson("${texture.key}_$id"))
            }
        }

        for (item in items) {
            val path = "assets/ecoitems/items/${item.id.key}.json"
            val definition = definition(item, materialIds)
            if (definition != null && path !in entries) {
                entries[path] = materialGson.toJson(jsonObject("model" to definition)).encodeToByteArray()
            }
        }
    }


    private fun definition(item: EcoItem, materialIds: List<String>): JsonObject? {
        val partTexture = item.partTexture()
        val modular = item.modular

        return when {
            partTexture != null -> recolouredOrTintedLayer(partTexture, index = 0, materialIds = materialIds)
            modular != null -> jsonObject(
                "type" to "minecraft:composite",
                "models" to jsonArray(
                    modular.layers.mapIndexedNotNull { index, slotId ->
                        modular.slots[slotId]
                            ?.let { shape -> partItemForShape(shape)?.partTexture() }
                            ?.let { recolouredOrTintedLayer(it, index, materialIds) }
                    } +
                        modular.layers.mapIndexedNotNull { index, slotId -> materialOverlayLayer(slotId, index) } +
                        item.overlayTextures().map { modelNode(it.key, tintIndex = null) }
                )
            )
            else -> null
        }
    }

    private fun materialOverlayLayer(slotId: String, index: Int): JsonObject? {
        val cases = Materials.values().mapNotNull { material ->
            material.overlaysIn(slotId)
                .mapNotNull { PackLocation.parse(it) }
                .takeIf { it.isNotEmpty() }
                ?.let { textures ->
                    jsonObject(
                        "when" to material.getID(),
                        "model" to jsonObject(
                            "type" to "minecraft:composite",
                            "models" to jsonArray(textures.map { modelNode(it.key, tintIndex = null) })
                        )
                    )
                }
        }

        return cases.takeIf { it.isNotEmpty() }?.let { overlayCases ->
            jsonObject(
                "type" to "minecraft:select",
                "property" to "minecraft:custom_model_data",
                "index" to index,
                "cases" to jsonArray(overlayCases),
                "fallback" to jsonObject("type" to "minecraft:empty")
            )
        }
    }

    private fun recolouredOrTintedLayer(
        texture: PackLocation,
        index: Int,
        materialIds: List<String>
    ): JsonObject = jsonObject(
        "type" to "minecraft:select",
        "property" to "minecraft:custom_model_data",
        "index" to index,
        "cases" to jsonArray(
            materialIds.map { id ->
                jsonObject("when" to id, "model" to modelNode("${texture.key}_$id", tintIndex = null))
            }
        ),
        "fallback" to modelNode(texture.key, tintIndex = index)
    )

    private fun modelNode(model: String, tintIndex: Int?): JsonObject {
        val node = jsonObject("type" to "minecraft:model", "model" to model)
        if (tintIndex != null) {
            val tint = jsonObject(
                "type" to "minecraft:custom_model_data",
                "index" to tintIndex,
                "default" to jsonArray(listOf(1.0, 1.0, 1.0))
            )
            node.add("tints", jsonArray(listOf(tint)))
        }
        return node
    }

    private fun permutationSource(textures: List<PackLocation>, materialIds: List<String>): JsonObject = jsonObject(
        "type" to "minecraft:paletted_permutations",
        "textures" to jsonArray(textures.map { it.key }),
        "palette_key" to PALETTE_KEY,
        "permutations" to permutations(materialIds)
    )

    private fun permutations(materialIds: List<String>): JsonObject {
        val json = JsonObject()
        materialIds.forEach { json.addProperty(it, "ecoitems:material/$it") }
        return json
    }

    private fun withSource(existing: ByteArray?, source: JsonObject): ByteArray {
        val atlas = existing
            ?.let { runCatching { JsonParser.parseString(it.decodeToString()).asJsonObject }.getOrNull() }
            ?: JsonObject()
        val sources = atlas.getAsJsonArray("sources") ?: JsonArray().also { atlas.add("sources", it) }
        sources.add(source)
        return materialGson.toJson(atlas).encodeToByteArray()
    }

    private fun paletteEntry(materialId: String): String = "assets/ecoitems/textures/palettes/material/$materialId.png"

    private fun recolouredEntry(texture: PackLocation, materialId: String): String =
        "assets/${texture.namespace}/models/${texture.path}_$materialId.json"

    private fun modelJson(texture: String): ByteArray = materialGson.toJson(
        jsonObject("parent" to "minecraft:item/handheld", "textures" to jsonObject("layer0" to texture))
    ).encodeToByteArray()


}
