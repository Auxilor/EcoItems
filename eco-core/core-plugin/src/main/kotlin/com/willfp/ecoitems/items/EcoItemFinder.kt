package com.willfp.ecoitems.items

import org.bukkit.inventory.ItemStack
import wiki.ecohub.rules.holder.slot.ItemHolderFinder
import wiki.ecohub.rules.holder.slot.SlotType

object EcoItemFinder : ItemHolderFinder<EcoItem>() {
    override fun find(item: ItemStack): List<EcoItem> {
        return listOfNotNull(item.ecoItem)
    }

    override fun isValidInSlot(holder: EcoItem, slot: SlotType): Boolean {
        return holder.slot.isOrContains(slot)
    }
}
