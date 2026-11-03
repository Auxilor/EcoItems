package com.willfp.ecoitems.materials

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.items.Items
import com.willfp.ecoitems.items.EcoItem
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.items.ecoItem
import com.willfp.ecoitems.plugin
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

internal val PART_MATERIAL_KEY = plugin.createNamespacedKey("part_material")
internal val MODULAR_MATERIALS_KEY = plugin.createNamespacedKey("modular_materials")

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
    /** Slot ID to the part shape that fits it. */
    val slots: Map<String, String> = config.getSubsection("slots").getKeys(false)
        .associateWith { config.getString("slots.$it") }

    /** Slot draw order for the model, bottom to top. */
    val layers: List<String> = config.getStrings("layers").ifEmpty { slots.keys.toList() }

    val name: String = config.getString("name")

    val lore: List<String> = config.getStrings("lore")

    /** Whether a slot's part can be replaced at an anvil. */
    val isSwappable: Boolean = config.getBool("swappable")

    /** The anvil level cost of replacing a part. */
    val swapCost: Int = (config.getIntOrNull("swap-cost") ?: 1).coerceAtLeast(1)

    /** The block tag the item mines, or null if it isn't a mining tool. */
    val miningBlocks: String? = config.getStringOrNull("mining.blocks")

    /** The slot whose material gives the mining speed and tier. */
    val miningSlot: String? = config.getStringOrNull("mining.from")
}

var ItemStack.partMaterial: Material?
    get() = itemMeta?.run { persistentDataContainer.get(PART_MATERIAL_KEY, PersistentDataType.STRING) }
        ?.let { Materials[it] }
    set(value) {
        itemMeta = itemMeta?.apply {
            if (value == null) {
                persistentDataContainer.remove(PART_MATERIAL_KEY)
            } else {
                persistentDataContainer.set(PART_MATERIAL_KEY, PersistentDataType.STRING, value.getID())
            }
        }
    }

var ItemStack.modularMaterials: Map<String, Material>
    get() = itemMeta?.run { persistentDataContainer.get(MODULAR_MATERIALS_KEY, PersistentDataType.STRING) }
        .orEmpty()
        .split(",")
        .mapNotNull { entry -> Materials[entry.substringAfter("=")]?.let { entry.substringBefore("=") to it } }
        .toMap()
    set(value) {
        itemMeta = itemMeta?.apply {
            persistentDataContainer.set(
                MODULAR_MATERIALS_KEY,
                PersistentDataType.STRING,
                value.entries.joinToString(",") { "${it.key}=${it.value.getID()}" }
            )
        }
    }

/** The first part item with this shape, used to give back a swapped-out part. */
fun partItemForShape(shape: String): EcoItem? = EcoItems.values().firstOrNull { it.part?.shape == shape }

/** A fresh part item of [item] made from [material]. */
fun partOf(item: EcoItem, material: Material): ItemStack = item.itemStack.madeFrom(material)

/** A copy of this part, made from [material]. */
fun ItemStack.madeFrom(material: Material): ItemStack {
    val copy = clone()
    copy.partMaterial = material
    return copy.withMaterials()
}

/** A copy of this modular item with these slot materials. */
fun ItemStack.builtFrom(materials: Map<String, Material>): ItemStack {
    val copy = clone()
    copy.modularMaterials = materials
    return copy.withMaterials()
}

/**
 * Rebuilds the parts of this stack that come from its materials: name, lore, merged components and
 * model data. Only the materials are stored, so this is safe on any fresh copy of the item.
 */
fun ItemStack.withMaterials(): ItemStack {
    val item = ecoItem ?: return this
    return materialComponents(item)?.let { withComponentsOf(item, it) } ?: this
}

private fun ItemStack.withComponentsOf(item: EcoItem, components: Map<String, Any?>): ItemStack {
    val result = Items.withComponents(this, components)
    for (error in result.errors) {
        plugin.logger.warning("Invalid material component on item ${item.id.key}: $error")
    }
    return result.item
}

/**
 * Applies `material:<id>` (parts) or `<slot>:<id>` (modular items) arguments, as given to
 * `/ecoitems give`. A modular item missing a slot keeps its plain base item.
 */
fun ItemStack.withMaterialArgs(args: List<String>): ItemStack {
    val item = ecoItem
    val pairs = args.filter { ":" in it }.associate { it.substringBefore(":") to it.substringAfter(":") }
    val modular = item?.modular

    return when {
        item?.part != null -> Materials[pairs["material"]]?.let { madeFrom(it) } ?: this
        modular != null -> builtFrom(
            modular.slots.keys.mapNotNull { slotId -> Materials[pairs[slotId]]?.let { slotId to it } }.toMap()
        )
        else -> this
    }
}
