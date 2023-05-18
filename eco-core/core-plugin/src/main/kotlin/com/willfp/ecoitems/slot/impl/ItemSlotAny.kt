package com.willfp.ecoitems.slot.impl

import com.willfp.ecoitems.slot.ItemSlot
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object ItemSlotAny : ItemSlot("any") {
    override fun getItems(player: Player): List<ItemStack?> {
        return player.inventory.contents.toList()
    }
}
