package com.willfp.ecoitems.items

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.util.NumberUtils
import com.willfp.libreforge.updateEffects
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.event.player.PlayerItemHeldEvent

class ItemListener(
    private val plugin: EcoPlugin
) : Listener {
    @EventHandler
    fun onHoldItem(event: PlayerItemHeldEvent) {
        event.player.updateEffects()
        plugin.scheduler.run { event.player.updateEffects() }
    }

    @EventHandler
    fun onPlaceItem(event: BlockPlaceEvent) {
        ItemUtils.getEcoItem(event.itemInHand) ?: return
        if (event.itemInHand.type.isBlock) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun effectiveDurabilityListener(event: PlayerItemDamageEvent) {
        val ecoItem = ItemUtils.getEcoItem(event.item) ?: return
        val maxDurability = event.item.type.maxDurability.toDouble() //This has to be a double for precise division
        //otherwise ratio will always be 0,1,2,... or some other int
        //then the randfloat wont work because ratio will be 0 so it removes durabilty like 1/100000 times
        val ratio = maxDurability / ecoItem.effectiveDurability
        if (ratio < 1) {
            if (NumberUtils.randFloat(0.0, 1.0) > ratio) {
                event.isCancelled = true
            }
        } else if (ratio > 1) {
            event.damage *= ratio.toInt()
        }
    }
}
