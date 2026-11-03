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
}
