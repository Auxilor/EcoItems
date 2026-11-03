package com.willfp.ecoitems.materials

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoitems.plugin

private val SLOT_ID = Regex("[a-z0-9_]+")

/** An item that is a part, made from one material. Read from its part: section. */
class PartConfig(config: Config) {
    val shape: String = config.getString("shape")

    val name: String = config.getString("name")

    val cost: Int = (config.getIntOrNull("cost") ?: 1).coerceAtLeast(1)

    /** The greyscale texture the resource pack recolours for each material. */
    val texture: String? = config.getStringOrNull("texture")

    /** The texture for each animation state, such as a bow's pulling_0, recoloured like [texture]. */
    val stateTextures: Map<String, String> = config.getSubsection("states").run {
        getKeys(false).associateWith { getString(it) }
    }
}

/** An item built from parts, one material per slot. Read from its modular: section. */
class ModularConfig(config: Config) {
    /** Slot ID to the part shape that fits it. IDs that can't go in a key are skipped with a warning. */
    val slots: Map<String, String> = config.getSubsection("slots").getKeys(false)
        .filter { slotId ->
            SLOT_ID.matches(slotId).also { valid ->
                if (!valid) {
                    plugin.logger.warning("Modular slot '$slotId' is skipped: use lowercase letters, numbers and _")
                }
            }
        }
        .associateWith { config.getString("slots.$it") }

    /** Slot draw order for the model, bottom to top. */
    val layers: List<String> = config.getStrings("layers").ifEmpty { slots.keys.toList() }

    /** Textures drawn above the slot layers as they are, never recoloured. */
    val overlays: List<String> = config.getStrings("overlays")

    /** A texture per slot drawn on this item in place of the part's own, recoloured the same way. */
    val slotTextures: Map<String, String> = config.getSubsection("textures").run {
        getKeys(false).associateWith { getString(it) }
    }

    val name: String = config.getString("name")

    val lore: List<String> = config.getStrings("lore")

    /** Whether a slot's part can be replaced at an anvil. */
    val isSwappable: Boolean = config.getBool("swappable")

    /** The anvil level cost of replacing a part. */
    val swapCost: Int = (config.getIntOrNull("swap-cost") ?: 1).coerceAtLeast(1)

    /** Whether the replaced part is given back. Off unless set, so a swap uses up the old part. */
    val returnsSwappedPart: Boolean = config.getBool("swap-returns-part")

    /** The block tag the item mines, or null if it isn't a mining tool. */
    val miningBlocks: String? = config.getStringOrNull("mining.blocks")

    /** The slot whose material gives the mining speed and tier. */
    val miningSlot: String? = config.getStringOrNull("mining.from")

    /** The vanilla animation the item's model follows, or null for a still model. */
    val animation: ItemAnimation? = config.getStringOrNull("animation")?.let { id ->
        ItemAnimation.fromId(id).also { animation ->
            if (animation == null) {
                plugin.logger.warning("Modular animation '$id' is skipped: use bow or crossbow")
            }
        }
    }

    /** Textures drawn on top in one animation state only, never recoloured. */
    val stateOverlays: Map<String, List<String>> = config.getSubsection("state-overlays").run {
        getKeys(false).associateWith { getStrings(it) }
    }
}

/** A vanilla item animation a modular item's model can follow, with the states it draws. */
enum class ItemAnimation(val id: String, val states: List<String>) {
    BOW("bow", listOf("pulling_0", "pulling_1", "pulling_2")),
    CROSSBOW("crossbow", listOf("pulling_0", "pulling_1", "pulling_2", "arrow", "rocket"));

    companion object {
        fun fromId(id: String): ItemAnimation? = entries.firstOrNull { it.id == id.lowercase() }
    }
}
