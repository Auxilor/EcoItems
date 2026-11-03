package com.willfp.ecoitems.items

import com.willfp.ecoitems.ext.modularMaterials
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable

/** Repairing an item, made from [result], with [itemsUsed] repair items. */
data class ItemRepair(
    val result: ItemStack,
    val itemsUsed: Int
)

/**
 * Anvil repairs of EcoItems with their repair-items, custom items included, and of modular items with
 * the material in their repair-from slot.
 */
object ItemRepairs {
    private const val ITEMS_FOR_FULL_REPAIR = 4

    /** [item] repaired with [addition], or null if [addition] doesn't repair it. */
    fun repair(item: ItemStack?, addition: ItemStack?): ItemRepair? {
        val ecoItem = item?.ecoItem
        val meta = item?.itemMeta as? Damageable

        val canRepair = addition != null && ecoItem != null && meta != null &&
            meta.hasDamage() && ecoItem.repairsWith(item, addition)

        return if (canRepair) repaired(item, meta, addition.amount) else null
    }

    private fun EcoItem.repairsWith(item: ItemStack, addition: ItemStack): Boolean =
        repairItems.any { it.matches(addition) } ||
            modular?.repairSlot?.let { item.modularMaterials[it] }?.matches(addition) == true

    private fun repaired(item: ItemStack, meta: Damageable, available: Int): ItemRepair {
        val maxDamage = if (meta.hasMaxDamage()) meta.maxDamage else item.type.maxDurability.toInt()
        val perItem = ((maxDamage + ITEMS_FOR_FULL_REPAIR - 1) / ITEMS_FOR_FULL_REPAIR).coerceAtLeast(1)
        val used = available.coerceAtMost((meta.damage + perItem - 1) / perItem)

        meta.damage = (meta.damage - used * perItem).coerceAtLeast(0)
        return ItemRepair(item.clone().apply { itemMeta = meta }, used)
    }
}
