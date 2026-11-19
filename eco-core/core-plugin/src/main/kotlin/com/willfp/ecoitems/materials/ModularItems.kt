package com.willfp.ecoitems.materials

import com.willfp.ecoitems.plugin
import wiki.ecohub.config.Config

private val SLOT_ID = Regex("[a-z0-9_]+")

/** An item that is a part, made from one material. Read from its part: section. */
class PartConfig(config: Config) {
    val shape: String = config.getString("shape")

    val name: String = config.getString("name")

    val cost: Int = (config.getIntOrNull("cost") ?: 1).coerceAtLeast(1)

    /** The greyscale texture the resource pack recolours for each material. */
    val texture: String? = config.getStringOrNull("texture")

    /** A model whose elements use #layer0, drawn in 3D in place of the flat [texture], such as a shield strap. */
    val model: String? = config.getStringOrNull("model")

    /** The texture for each animation state, such as a bow's pulling_0, recoloured like [texture]. */
    val stateTextures: Map<String, String> = config.getSubsection("states").run {
        getKeys(false).withoutUnknownStates().associateWith { getString(it) }
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

    /** The slot whose material repairs the item at an anvil, or null to repair it the vanilla way only. */
    val repairSlot: String? = config.getStringOrNull("repair-from")

    /** The vanilla animation the item's model follows, or null for a still model. */
    val animation: ItemAnimation? = config.getStringOrNull("animation")?.let { id ->
        ItemAnimation.fromId(id).also { animation ->
            if (animation == null) {
                val options = ItemAnimation.entries.joinToString { it.id }
                plugin.logger.warning("Modular animation '$id' is skipped: use $options")
            }
        }
    }

    /** Textures drawn on top in one animation state only, never recoloured. */
    val stateOverlays: Map<String, List<String>> = config.getSubsection("state-overlays").run {
        getKeys(false).withoutUnknownStates().associateWith { getStrings(it) }
    }
}

private val ANIMATION_STATES: Set<String> = ItemAnimation.entries.flatMapTo(mutableSetOf()) { it.states }

private fun Collection<String>.withoutUnknownStates(): List<String> = filter { state ->
    (state in ANIMATION_STATES).also { known ->
        if (!known) {
            plugin.logger.warning("Modular state '$state' is ignored: use ${ANIMATION_STATES.joinToString()}")
        }
    }
}

/** A vanilla item animation a modular item's model can follow, with the states it draws. */
enum class ItemAnimation(val id: String, val states: List<String>) {
    BOW("bow", listOf("pulling_0", "pulling_1", "pulling_2")),
    CROSSBOW("crossbow", listOf("pulling_0", "pulling_1", "pulling_2", "arrow", "rocket")),
    SHIELD("shield", listOf("blocking")),
    TRIDENT("trident", listOf("throwing"));

    companion object {
        fun fromId(id: String): ItemAnimation? = entries.firstOrNull { it.id == id.lowercase() }
    }
}
