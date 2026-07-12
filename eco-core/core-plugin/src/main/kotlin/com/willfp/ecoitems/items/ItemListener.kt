package com.willfp.ecoitems.items

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.player.PlayerInteractEvent

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
    fun preventShootingItemsAsArrows(event: EntityShootBowEvent) {
        if (event.consumable?.ecoItem != null) {
            event.isCancelled = true
        }
    }
}
