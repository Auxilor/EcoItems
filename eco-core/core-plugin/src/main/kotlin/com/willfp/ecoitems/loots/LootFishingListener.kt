package com.willfp.ecoitems.loots

import com.willfp.ecoitems.api.EcoItemsLoot
import org.bukkit.entity.Item
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerFishEvent

/**
 * Fishing loot replaces the vanilla catch, which a drop contribution can't
 * express - contributors add drops, they can't remove them.
 *
 * Runs at LOW so the replacement is in place before libreforge's catch_fish
 * trigger (NORMAL) and telekinesis (HIGH) read the caught stack, which is what
 * puts the replacement through the drop pipeline.
 */
object LootFishingListener : Listener {
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    fun onFish(event: PlayerFishEvent) {
        if (event.state != PlayerFishEvent.State.CAUGHT_FISH) {
            return
        }

        val caught = event.caught as? Item ?: return
        val roll = EcoItemsLoot.rollFishing(event.hook.location, event.player) ?: return
        caught.itemStack = roll.item
        event.expToDrop += roll.experience
    }
}
