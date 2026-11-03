package com.willfp.ecoitems.ext

import com.willfp.eco.core.items.Items
import com.willfp.ecoitems.items.EcoItem
import com.willfp.ecoitems.items.ecoItem
import com.willfp.ecoitems.materials.Material
import com.willfp.ecoitems.materials.Materials
import com.willfp.ecoitems.materials.materialComponents
import com.willfp.ecoitems.plugin
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

internal val PART_MATERIAL_KEY = plugin.createNamespacedKey("part_material")
internal val MODULAR_MATERIALS_KEY = plugin.createNamespacedKey("modular_materials")
private val MINIMUM_MINING_TIER_KEY = plugin.createNamespacedKey("minimum_mining_tier")

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

/** The lowest tier a modular item mines at, whatever its materials, or null for its materials' tier. */
var ItemStack.minimumMiningTier: String?
    get() = itemMeta?.run { persistentDataContainer.get(MINIMUM_MINING_TIER_KEY, PersistentDataType.STRING) }
    set(value) {
        itemMeta = itemMeta?.apply {
            if (value == null) {
                persistentDataContainer.remove(MINIMUM_MINING_TIER_KEY)
            } else {
                persistentDataContainer.set(MINIMUM_MINING_TIER_KEY, PersistentDataType.STRING, value)
            }
        }
    }

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

/** This fresh item, made from the same materials as [source]. */
fun ItemStack.withMaterialsOf(source: ItemStack): ItemStack {
    val copy = clone()
    source.partMaterial?.let { copy.partMaterial = it }
    source.modularMaterials.takeIf { it.isNotEmpty() }?.let { copy.modularMaterials = it }
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

/** The `material:<id>` and `<slot>:<id>` pairs in lookup or command arguments. */
fun materialArgs(args: List<String>): Map<String, String> =
    args.filter { ":" in it }.associate { it.substringBefore(":") to it.substringAfter(":") }

/**
 * Applies `material:<id>` (parts) or `<slot>:<id>` (modular items) arguments, as given to
 * `/ecoitems give`. Unless every slot gets a known material, the item stays its plain base item.
 */
fun ItemStack.withMaterialArgs(args: List<String>): ItemStack {
    val item = ecoItem
    val pairs = materialArgs(args)
    val modular = item?.modular
    val slotMaterials = modular?.run { slots.keys }.orEmpty()
        .mapNotNull { slotId -> Materials[pairs[slotId]]?.let { slotId to it } }
        .toMap()

    return when {
        item?.part != null -> Materials[pairs["material"]]?.let { madeFrom(it) } ?: this
        modular != null && slotMaterials.size == modular.slots.size -> builtFrom(slotMaterials)
        else -> this
    }
}
