package com.willfp.ecoitems.api

import com.willfp.ecoitems.loots.LootContributor
import com.willfp.ecoitems.loots.LootFishingListener
import com.willfp.ecoitems.loots.Loots
import org.bukkit.Location
import org.bukkit.entity.Player

/**
 * EcoItems loot for catches that never fire a `PlayerFishEvent`, such as a
 * fishing minion's.
 */
object EcoItemsLoot {
    /**
     * Rolls `fishing` loot as a player fishing at [location] would.
     *
     * The first loot that rolls and yields an item wins, as in [LootFishingListener].
     *
     * @return The catch, or null to keep the vanilla one.
     */
    @JvmStatic
    fun rollFishing(location: Location, player: Player): FishingRoll? {
        val world = location.world ?: return null
        val biome = location.block.biome

        for (loot in Loots.values()) {
            if (!loot.rollsForFishing(world, biome, player)) {
                continue
            }
            val item = LootContributor.rollItems(loot, 0).firstOrNull() ?: continue
            return FishingRoll(item, if (loot.xp.isEmpty()) 0 else loot.xp.random().coerceAtLeast(0))
        }
        return null
    }
}
