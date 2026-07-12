package com.willfp.ecoitems.pack

import com.willfp.ecoitems.EcoItemsPlugin

/**
 * Generates the per-item pack entries for the modern (1.21.4+) item model
 * system: an item definition in assets/ecoitems/items/, and a generated model
 * for plain textures. The texture and model files themselves are mapped into
 * the pack wholesale by [PackBuilder].
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
            val problem = asset.validate() ?: generate(plugin, asset, entries)
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
                if (!plugin.dataFolder.resolve("pack/textures/$texture.png").exists()) {
                    return "texture file pack/textures/$texture.png does not exist"
                }

                // An explicit model file with the same name beats the generated one.
                if (!plugin.dataFolder.resolve("pack/models/$texture.json").exists()) {
                    entries["assets/ecoitems/models/item/$texture.json"] = modelJson(asset, texture)
                }

                "ecoitems:item/$texture"
            }

            ":" in model -> model

            else -> {
                if (!plugin.dataFolder.resolve("pack/models/$model.json").exists()) {
                    return "model file pack/models/$model.json does not exist"
                }

                "ecoitems:item/$model"
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

    private fun modelJson(asset: ItemPackAsset, texture: String): ByteArray {
        val parent = when (asset.textureParent) {
            "generated" -> "minecraft:item/generated"
            "handheld" -> "minecraft:item/handheld"
            else -> asset.textureParent
        }

        return """
            {
              "parent": ${parent.toJsonString()},
              "textures": {
                "layer0": ${"ecoitems:item/$texture".toJsonString()}
              }
            }
        """.trimIndent().encodeToByteArray()
    }
}
