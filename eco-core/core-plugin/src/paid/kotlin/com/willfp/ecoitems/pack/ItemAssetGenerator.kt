package com.willfp.ecoitems.pack

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.willfp.ecoitems.EcoItemsPlugin

/**
 * Generates the per-item pack entries for the modern (1.21.4+) item model
 * system: an item definition in assets/ecoitems/items/ (a node tree when
 * state models or a raw definition are configured), plus generated models
 * for plain textures. The texture and model files themselves live in the
 * vanilla-structured pack folder and are copied wholesale by [PackBuilder].
 *
 * The minecraft:item_model component on the item points at the definition
 * (ecoitems:<id>), which is applied by EcoItem itself.
 */
object ItemAssetGenerator {
    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    fun generate(
        plugin: EcoItemsPlugin,
        assets: List<ItemPackAsset>,
        entries: MutableMap<String, ByteArray>
    ) {
        for (asset in assets) {
            val problem = asset.invalid ?: generate(plugin, asset, entries)
            if (problem != null) {
                plugin.logger.warning("Skipping pack assets for item ${asset.id}: $problem")
            }
        }
    }

    /** Returns an error message, or null on success. */
    private fun generate(
        plugin: EcoItemsPlugin,
        asset: ItemPackAsset,
        entries: MutableMap<String, ByteArray>
    ): String? {
        // State models: check they exist, and generate simple models for
        // texture-based states.
        for ((state, stateModel) in asset.states) {
            for (location in stateModel.locations) {
                if (stateModel.fromTextures) {
                    resolveTexture(plugin, asset, location, entries)?.let { return "$state: $it" }
                } else if (!location.file(plugin, "models", "json").exists() && location.namespace != "minecraft") {
                    return "$state model file pack/${location.entry("models", "json")} does not exist"
                }
            }
        }

        val baseModelKey = when {
            asset.model != null -> {
                val model = asset.model
                if (!model.file(plugin, "models", "json").exists() && model.namespace != "minecraft") {
                    return "model file pack/${model.entry("models", "json")} does not exist"
                }
                model.key
            }

            asset.texture != null -> {
                resolveTexture(plugin, asset, asset.texture, entries)?.let { return it }
                asset.texture.key
            }

            // A raw definition needs no base model.
            asset.definition != null -> ""

            else -> return "no texture or model"
        }

        val definitions = ItemDefinitionBuilder.build(asset, baseModelKey)

        entries["assets/ecoitems/items/${asset.id}.json"] = definitionJson(definitions.main)
        definitions.throwing?.let {
            entries["assets/ecoitems/items/${asset.id}_throwing.json"] = definitionJson(it)
        }

        return null
    }

    /**
     * Checks a texture exists, warns when it's outside the atlas dirs, and
     * generates a simple model for it (unless an explicit model file shadows
     * it). Returns an error message, or null on success.
     */
    private fun resolveTexture(
        plugin: EcoItemsPlugin,
        asset: ItemPackAsset,
        texture: PackLocation,
        entries: MutableMap<String, ByteArray>
    ): String? {
        if (!texture.file(plugin, "textures", "png").exists() && texture.namespace != "minecraft") {
            return "texture file pack/${texture.entry("textures", "png")} does not exist"
        }

        if (!texture.path.startsWith("item/") && !texture.path.startsWith("block/")) {
            plugin.logger.warning(
                "Item ${asset.id}'s texture ${texture.key} is outside textures/item/ and textures/block/, " +
                    "so it won't be stitched into the block atlas and the model may not render"
            )
        }

        // An explicit model file with the same location beats the generated one.
        if (!texture.file(plugin, "models", "json").exists()) {
            entries[texture.entry("models", "json")] = modelJson(asset, texture)
        }

        return null
    }

    private fun definitionJson(model: JsonObject): ByteArray {
        val root = JsonObject()
        root.add("model", model)
        return gson.toJson(root).encodeToByteArray()
    }

    private fun modelJson(asset: ItemPackAsset, texture: PackLocation): ByteArray {
        val parent = when (asset.textureParent) {
            "generated" -> "minecraft:item/generated"
            "handheld" -> "minecraft:item/handheld"
            else -> asset.textureParent
        }

        return """
            {
              "parent": ${parent.toJsonString()},
              "textures": {
                "layer0": ${texture.key.toJsonString()}
              }
            }
        """.trimIndent().encodeToByteArray()
    }
}
