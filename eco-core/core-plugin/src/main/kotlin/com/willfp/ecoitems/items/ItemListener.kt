package com.willfp.ecoitems.items

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.inventory.FurnaceBurnEvent
import org.bukkit.event.player.PlayerInteractEvent

object ItemListener : Listener {
    @EventHandler(priority = EventPriority.LOW)
    fun onPlaceItem(event: BlockPlaceEvent) {
        val ecoItem = event.itemInHand.ecoItem ?: return

        // Placer items (custom blocks/furniture) dispatch their own synthetic
        // place events for protection plugins - don't cancel our own placement.
        if (ecoItem.block != null || ecoItem.furniture != null) {
            return
        }

        if (event.itemInHand.type.isBlock) {
            event.isCancelled = true
        }
    }

    // Runs after the block/furniture placement handlers (HIGH): when their
    // placement went through, the event is already cancelled; when it bailed,
    // this still stops the base block from being placed vanilla-style.
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

    /** fuel.burn-ticks overrides how long the item burns in a furnace. */
    @EventHandler(ignoreCancelled = true)
    fun onFurnaceBurn(event: FurnaceBurnEvent) {
        val ticks = event.fuel.ecoItem?.fuelBurnTicks ?: return

        if (ticks <= 0) {
            event.isCancelled = true
        } else {
            event.burnTime = ticks
        }
    }
}
