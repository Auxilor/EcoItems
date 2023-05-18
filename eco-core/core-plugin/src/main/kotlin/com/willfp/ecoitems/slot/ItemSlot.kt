package com.willfp.ecoitems.slot

import com.willfp.eco.core.registry.KRegistrable
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

abstract class ItemSlot(
    override val id: String
) : KRegistrable {
    abstract fun getItems(player: Player): List<ItemStack?>

    override fun equals(other: Any?): Boolean {
        return other is ItemSlot && this.id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
