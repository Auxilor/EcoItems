package com.willfp.ecoitems.fuels

import com.willfp.eco.core.fast.FastItemStack
import com.willfp.ecoitems.EcoItemsPlugin.Companion.instance
import com.willfp.ecoitems.items.EcoItem
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

object FuelUtils {
    /**
     * Instance of EcoItems.
     */
    private val PLUGIN = instance

    /**
     * Get fuel from an item.
     *
     * @param itemStack The itemStack to check.
     * @return The fuel, or null if no fuel is found.
     */
    fun getFuelFromItem(itemStack: ItemStack?): Fuel? {
        itemStack ?: return null
        val container = FastItemStack.wrap(itemStack).persistentDataContainer
        return getFuelFromItem(container)
    }

    /**
     * Get fuel on an item.
     *
     * @param meta The itemStack to check.
     * @return The fuel, or null if no fuel is found.
     */
    fun getFuelFromItem(meta: ItemMeta): Fuel? {
        val container = meta.persistentDataContainer
        return getFuelFromItem(container)
    }

    private fun getFuelFromItem(container: PersistentDataContainer): Fuel? {
        val fuelName = container.get(
            PLUGIN.namespacedKeyFactory.create("fuel"),
            PersistentDataType.STRING
        ) ?: return null
        return Fuels.getByID(fuelName)
    }

    /**
     * If player has fuel for an item.
     *
     * @param player The player to check.
     * @param item The item.
     * @return If the player has fuel for it.
     */
    fun hasFuelFor(player: Player, item: EcoItem): Boolean {
        if (item.fuels.isEmpty()) {
            return true
        }
        for (fuel in item.fuels) {
            if (hasFuel(player, fuel)) {
                return true
            }
        }

        return false
    }

    /**
     * If player has fuel.
     *
     * @param player The player to check.
     * @param fuel The fuel.
     * @return If the player has fuel.
     */
    fun hasFuel(player: Player, fuel: Fuel): Boolean {
        for (stack in player.inventory.contents) {
            if (getFuelFromItem(stack) == fuel) {
                return true
            }
        }

        return false
    }
}
