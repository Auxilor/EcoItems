package com.willfp.ecoitems.pack

import com.willfp.ecoitems.items.EcoItem

/**
 * The pack-facing part of an item config: a texture to generate a model for,
 * or a model supplied by the user. Both are "[ns:]path" locations relative
 * to textures/ and models/ respectively.
 */
class ItemPackAsset(
    val id: String,
    val texture: PackLocation?,
    val textureParent: String,
    val model: PackLocation?,
    val invalid: String?
) {
    companion object {
        fun fromItem(item: EcoItem): ItemPackAsset? {
            val itemConfig = item.config.getSubsection("item")

            val texture = itemConfig.getStringOrNull("texture")
            val model = itemConfig.getStringOrNull("model")
            if (texture == null && model == null) {
                return null
            }

            val parsedTexture = texture?.let { PackLocation.parse(it) }
            val parsedModel = model?.let { PackLocation.parse(it) }

            val invalid = when {
                texture != null && parsedTexture == null -> "texture '$texture' is not a valid location"
                model != null && parsedModel == null -> "model '$model' is not a valid location"
                else -> null
            }

            return ItemPackAsset(
                item.id.key,
                parsedTexture,
                itemConfig.getStringOrNull("texture-parent") ?: "generated",
                parsedModel,
                invalid
            )
        }
    }
}
