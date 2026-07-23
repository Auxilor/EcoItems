package com.willfp.ecoitems.loots

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
        val hook = event.hook.location
        val world = hook.world ?: return

        for (loot in Loots.values()) {
            if (!loot.rollsForFishing(world, hook.block.biome, event.player)) {
                continue
            }

            val replacement = LootContributor.rollItems(loot, 0).firstOrNull() ?: continue
            caught.itemStack = replacement
            if (!loot.xp.isEmpty()) {
                event.expToDrop += loot.xp.random().coerceAtLeast(0)
            }
            return
        }
    }
}
