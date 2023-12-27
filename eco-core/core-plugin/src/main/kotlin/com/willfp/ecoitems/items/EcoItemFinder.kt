package com.willfp.ecoitems.items

import com.willfp.libreforge.slot.ItemHolderFinder
import com.willfp.libreforge.slot.SlotType
import org.bukkit.inventory.ItemStack

object EcoItemFinder: ItemHolderFinder<EcoItem>() {
    override fun find(item: ItemStack): List<EcoItem> {
        return listOfNotNull(item.ecoItem)
    }

    override fun isValidInSlot(holder: EcoItem, slot: SlotType): Boolean {
        return holder.slot == slot
    }
}
