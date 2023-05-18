package com.willfp.ecoitems.items

import com.willfp.eco.util.NumberUtils
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import kotlin.math.roundToInt

object ItemListener : Listener {
    @EventHandler(priority = EventPriority.LOW)
    fun onPlaceItem(event: BlockPlaceEvent) {
        event.itemInHand.ecoItem ?: return

        if (event.itemInHand.type.isBlock) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlaceItem2(event: PlayerInteractEvent) {
        event.item.ecoItem ?: return

        if (event.item?.type?.isBlock != true) {
            return
        }

        if (event.action == Action.RIGHT_CLICK_BLOCK) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun effectiveDurabilityListener(event: PlayerItemDamageEvent) {
        val ecoItem = event.item.ecoItem ?: return

        val maxDurability = event.item.type.maxDurability.toInt()

        val ratio = maxDurability.toDouble() / ecoItem.effectiveDurability

        if (ratio < 1) {
            if (NumberUtils.randFloat(0.0, 1.0) > ratio) {
                event.isCancelled = true
            }
        } else if (ratio > 1) {
            event.damage *= ratio.roundToInt()
        }
    }
}
