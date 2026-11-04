package com.willfp.ecoitems.pack

import com.google.gson.JsonObject
import com.willfp.ecoitems.EcoItemsPlugin
import com.willfp.ecoitems.items.EcoItem
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.materials.Material
import com.willfp.ecoitems.materials.MaterialPalettes
import com.willfp.ecoitems.materials.Materials
import com.willfp.ecoitems.materials.ModularConfig
import com.willfp.ecoitems.materials.ModularItemService

/**
 * Generates the look of parts and modular items, the armor trim way: one greyscale texture per part
 * shape, recoloured per material by a paletted_permutations atlas source. Writes the palette key and
 * material palettes, the atlas source, a model per shape and material, and the item definitions,
 * which select each layer's model on the material id stored in custom_model_data.
 *
 * Runs before the pack folder is merged, so user files still win. An item that sets its own
 * texture, model or definition keeps that instead of a generated definition.
 */
object MaterialAssetGenerator {
    private val OWN_MODEL_KEYS = listOf("texture", "model", "definition")

    fun generate(plugin: EcoItemsPlugin, entries: MutableMap<String, ByteArray>) {
        val materials = Materials.values()
        val materialIds = materials.map { it.getID() }
        val items = EcoItems.values()
        val textures = items.flatMap { it.allPartTextures() }.distinct()

        for (texture in textures) {
            entries[generatedModel(texture).entry(root = "models", extension = "json")] = modelJson(texture.key)
        }

        if (materialIds.isNotEmpty() && textures.isNotEmpty()) {
            writeRecolouring(plugin, entries, textures, materials)
        }
        writeOverlayModels(plugin, entries, items, materials)
        ModularAnimations.writePosedModels(entries, items, if (textures.isEmpty()) emptyList() else materialIds)

        val textureIds = if (textures.isEmpty()) emptyList() else materialIds
        for (item in items.filterNot { item -> OWN_MODEL_KEYS.any { item.config.getSubsection("item").has(it) } }) {
            definition(item, textureIds)?.let { definition ->
                entries["assets/ecoitems/items/${item.id.key}.json"] =
                    materialGson.toJson(jsonObject("model" to definition)).encodeToByteArray()
            }
        }
    }

    private fun writeRecolouring(
        plugin: EcoItemsPlugin,
        entries: MutableMap<String, ByteArray>,
        textures: List<PackLocation>,
        materials: List<Material>
    ) {
        textures.forEach { warnAboutOffRampColours(plugin, it) }

        val keyImage = paletteImage(MaterialPalettes.RAMP.map { it * GREY_STEP })
        paletteEntries(PALETTE_KEY_NAME).forEach { entries[it] = keyImage }
        for (material in materials) {
            val image = paletteImage(material.palette)
            paletteEntries(materialPaletteName(material.getID())).forEach { entries[it] = image }
        }

        val source = permutationSource(textures, materials.map { it.getID() })
        ATLASES.forEach { entries[it] = withSource(existing = entries[it], source = source) }

        for (texture in textures) {
            for (material in materials) {
                entries[generatedModel(texture, material.getID()).entry(root = "models", extension = "json")] =
                    modelJson("${texture.key}_${material.getID()}")
            }
        }
    }

    private fun writeOverlayModels(
        plugin: EcoItemsPlugin,
        entries: MutableMap<String, ByteArray>,
        items: List<EcoItem>,
        materials: List<Material>
    ) {
        val overlays = items.flatMap { it.allOverlayTextures() } +
            materials.flatMap { material -> material.allOverlays.mapNotNull { PackLocation.parse(it) } }

        for (overlay in overlays.distinct()) {
            warnIfMissing(plugin, overlay)
            entries[generatedModel(overlay).entry(root = "models", extension = "json")] = modelJson(overlay.key)
        }
    }

    private fun definition(item: EcoItem, materialIds: List<String>): JsonObject? {
        val partTexture = item.partTexture()
        val modular = item.modular

        return when {
            partTexture != null -> recolouredOrTintedLayer(partTexture, index = 0, materialIds = materialIds)
            modular != null -> ModularAnimations.modelTree(modular.animation) { state ->
                composite(item, modular, materialIds, state)
            }
            else -> null
        }
    }

    private fun composite(
        item: EcoItem,
        modular: ModularConfig,
        materialIds: List<String>,
        state: String?
    ): JsonObject {
        val pose = modular.animation?.id
        return jsonObject(
            "type" to "minecraft:composite",
            "models" to jsonArray(
                modular.layers.mapIndexedNotNull { index, slotId ->
                    modular.slots[slotId]
                        ?.let { shape -> ModularItemService.partItemForShape(shape)?.partTexture(state) }
                        ?.let { recolouredOrTintedLayer(it, index, materialIds, pose) }
                }.ifEmpty { listOf(baseModel(item)) } +
                    modular.layers.mapIndexedNotNull { index, slotId -> materialOverlayLayer(slotId, index, pose) } +
                    item.overlayTextures(state).map { modelNode(generatedModel(it, pose = pose).key, tintIndex = null) }
            )
        )
    }

    private fun baseModel(item: EcoItem): JsonObject =
        modelNode("minecraft:item/${item.itemStack.type.key.key}", tintIndex = null)

    private fun materialOverlayLayer(slotId: String, index: Int, pose: String?): JsonObject? {
        val cases = Materials.values().mapNotNull { material ->
            material.overlaysIn(slotId)
                .mapNotNull { PackLocation.parse(it) }
                .takeIf { it.isNotEmpty() }
                ?.let { textures ->
                    jsonObject(
                        "when" to material.getID(),
                        "model" to jsonObject(
                            "type" to "minecraft:composite",
                            "models" to jsonArray(
                                textures.map { modelNode(generatedModel(it, pose = pose).key, tintIndex = null) }
                            )
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
        materialIds: List<String>,
        pose: String? = null
    ): JsonObject = if (materialIds.isEmpty()) {
        modelNode(generatedModel(texture, pose = pose).key, tintIndex = index)
    } else {
        recolouredLayer(texture, index, materialIds, pose)
    }

    private fun recolouredLayer(
        texture: PackLocation,
        index: Int,
        materialIds: List<String>,
        pose: String?
    ): JsonObject = jsonObject(
        "type" to "minecraft:select",
        "property" to "minecraft:custom_model_data",
        "index" to index,
        "cases" to jsonArray(
            materialIds.map { id ->
                jsonObject("when" to id, "model" to modelNode(generatedModel(texture, id, pose).key, tintIndex = null))
            }
        ),
        "fallback" to modelNode(generatedModel(texture, pose = pose).key, tintIndex = index)
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
}
