package com.willfp.ecoitems.slot.impl

import com.willfp.ecoitems.slot.ItemSlot
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object ItemSlotHands : ItemSlot("hands") {
    override fun getItems(player: Player): List<ItemStack> {
        return listOf(
            player.inventory.itemInMainHand,
            player.inventory.itemInOffHand
        )
    }
}
