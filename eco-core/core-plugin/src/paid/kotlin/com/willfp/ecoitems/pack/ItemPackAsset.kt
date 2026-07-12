package com.willfp.ecoitems.pack

import com.willfp.ecoitems.items.EcoItem

private val PATH_REGEX = Regex("[a-z0-9_./-]+")
private val NAMESPACED_REGEX = Regex("[a-z0-9_.-]+:[a-z0-9_./-]+")

/**
 * The pack-facing part of an item config: a texture to generate a model for,
 * or a model supplied by the user.
 */
class ItemPackAsset(
    val id: String,
    val texture: String?,
    val textureParent: String,
    val model: String?
) {
    companion object {
        fun fromItem(item: EcoItem): ItemPackAsset? {
            val itemConfig = item.config.getSubsection("item")

            val texture = itemConfig.getStringOrNull("texture")
            val model = itemConfig.getStringOrNull("model")
            if (texture == null && model == null) {
                return null
            }

            return ItemPackAsset(
                item.id.key,
                texture,
                itemConfig.getStringOrNull("texture-parent") ?: "generated",
                model
            )
        }
    }

    fun validate(): String? {
        if (texture != null && !texture.matches(PATH_REGEX)) {
            return "texture '$texture' may only contain a-z, 0-9, _ . / -"
        }

        if (model != null) {
            val valid = if (":" in model) model.matches(NAMESPACED_REGEX) else model.matches(PATH_REGEX)
            if (!valid) {
                return "model '$model' is not a valid model location"
            }
        }

        return null
    }
}
