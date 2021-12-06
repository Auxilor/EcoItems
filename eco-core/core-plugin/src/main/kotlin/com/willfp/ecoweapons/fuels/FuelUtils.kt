package com.willfp.ecoweapons.fuels

import com.willfp.ecoweapons.EcoWeaponsPlugin.Companion.instance
import com.willfp.ecoweapons.weapons.Weapon
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

object FuelUtils {
    /**
     * Instance of EcoWeapons.
     */
    private val PLUGIN = instance

    /**
     * Get fuel weapon from an item.
     *
     * @param itemStack The itemStack to check.
     * @return The weapon, or null if no weapon is found.
     */
    fun getFuelFromItem(itemStack: ItemStack?): Fuel? {
        itemStack ?: return null
        val meta = itemStack.itemMeta ?: return null
        return getFuelFromItem(meta)
    }

    /**
     * Get fuel weapon on an item.
     *
     * @param meta The itemStack to check.
     * @return The weapon, or null if no weapon is found.
     */
    fun getFuelFromItem(meta: ItemMeta): Fuel? {
        val container = meta.persistentDataContainer
        val fuelName = container.get(
            PLUGIN.namespacedKeyFactory.create("fuel"),
            PersistentDataType.STRING
        ) ?: return null
        return Fuels.getByID(fuelName)
    }

    /**
     * If player has fuel for a weapon.
     *
     * @param player The player to check.
     * @param weapon The weapon.
     * @return If the player has fuel for it.
     */
    fun hasFuelFor(player: Player, weapon: Weapon): Boolean {
        if (weapon.fuels.isEmpty()) {
            return true
        }
        for (fuel in weapon.fuels) {
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
