package com.willfp.ecoweapons.weapons

import com.willfp.ecoweapons.EcoWeaponsPlugin.Companion.instance
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

object WeaponUtils {
    /**
     * Instance of EcoWeapons.
     */
    private val PLUGIN = instance

    /**
     * Get weapon from an item.
     *
     * @param itemStack The itemStack to check.
     * @return The weapon, or null if no weapon is found.
     */
    fun getWeaponFromItem(itemStack: ItemStack?): Weapon? {
        itemStack ?: return null
        val meta = itemStack.itemMeta ?: return null
        return getWeaponFromItem(meta)
    }

    /**
     * Get weapon on an item.
     *
     * @param meta The itemStack to check.
     * @return The weapon, or null if no weapon is found.
     */
    fun getWeaponFromItem(meta: ItemMeta): Weapon? {
        val container = meta.persistentDataContainer
        val weaponName = container.get(
            PLUGIN.namespacedKeyFactory.create("weapon"),
            PersistentDataType.STRING
        ) ?: return null
        return Weapons.getByID(weaponName)
    }

    /**
     * Get weapon on a player.
     *
     * @param player The player to check.
     * @return The weapon, or null if no weapon is found.
     */
    fun getWeaponOnPlayer(player: Player): Weapon? {
        return getWeaponFromItem(player.inventory.itemInMainHand)
    }

    /**
     * Get fuel weapon from an item.
     *
     * @param itemStack The itemStack to check.
     * @return The weapon, or null if no weapon is found.
     */
    fun getFuelFromItem(itemStack: ItemStack?): Weapon? {
        itemStack ?: return null
        val meta = itemStack.itemMeta ?: return null
        return getWeaponFromItem(meta)
    }

    /**
     * Get fuel weapon on an item.
     *
     * @param meta The itemStack to check.
     * @return The weapon, or null if no weapon is found.
     */
    fun getFuelFromItem(meta: ItemMeta): Weapon? {
        val container = meta.persistentDataContainer
        val weaponName = container.get(
            PLUGIN.namespacedKeyFactory.create("fuel"),
            PersistentDataType.STRING
        ) ?: return null
        return Weapons.getByID(weaponName)
    }

    /**
     * If player has fuel for a weapon.
     *
     * @param player The player to check.
     * @param weapon The weapon.
     * @return If the player has fuel for it.
     */
    fun hasFuelFor(player: Player, weapon: Weapon): Boolean {
        if (!weapon.fuelEnabled) {
            return true
        }
        for (stack in player.inventory.contents) {
            if (getFuelFromItem(stack) == weapon) {
                return true
            }
        }

        return false
    }
}

inline fun <reified T> T?.toSingletonList(): List<T> {
    return if (this == null) emptyList() else listOf(this)
}