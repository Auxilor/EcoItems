package com.willfp.ecoitems.pack

import com.willfp.ecoitems.EcoItemsPlugin

/**
 * Generates the per-item pack entries for the modern (1.21.4+) item model
 * system: an item definition in assets/ecoitems/items/, and a generated model
 * for plain textures. The texture and model files themselves live in the
 * vanilla-structured pack folder and are copied wholesale by [PackBuilder].
 *
 * The minecraft:item_model component on the item points at the definition
 * (ecoitems:<id>), which is applied by EcoItem itself.
 */
object ItemAssetGenerator {
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
        val model = asset.model

        val modelKey = when {
            model == null -> {
                val texture = asset.texture ?: return "no texture or model"

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

                texture.key
            }

            else -> {
                if (!model.file(plugin, "models", "json").exists() && model.namespace != "minecraft") {
                    return "model file pack/${model.entry("models", "json")} does not exist"
                }

                model.key
            }
        }

        entries["assets/ecoitems/items/${asset.id}.json"] = definitionJson(modelKey)

        return null
    }

    private fun definitionJson(modelKey: String): ByteArray = """
        {
          "model": {
            "type": "minecraft:model",
            "model": ${modelKey.toJsonString()}
          }
        }
    """.trimIndent().encodeToByteArray()

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
