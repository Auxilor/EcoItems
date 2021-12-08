package com.willfp.ecoweapons.weapons

import com.willfp.ecoweapons.EcoWeaponsPlugin.Companion.instance
import org.bukkit.Material
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
}

inline fun <reified T> T?.toSingletonList(): List<T> {
    return if (this == null) emptyList() else listOf(this)
}

fun Material.getBaseDamage(): Double {
    return when(this) {
        Material.WOODEN_SWORD -> 4.0
        Material.WOODEN_SHOVEL -> 2.5
        Material.WOODEN_PICKAXE -> 2.0
        Material.WOODEN_AXE -> 7.0

        Material.GOLDEN_SWORD -> 4.0
        Material.GOLDEN_SHOVEL -> 2.5
        Material.GOLDEN_PICKAXE -> 2.0
        Material.GOLDEN_AXE -> 7.0

        Material.STONE_SWORD -> 5.0
        Material.STONE_SHOVEL -> 3.5
        Material.STONE_PICKAXE -> 3.0
        Material.STONE_AXE -> 9.0

        Material.IRON_SWORD -> 6.0
        Material.IRON_SHOVEL -> 4.5
        Material.IRON_PICKAXE -> 4.0
        Material.IRON_AXE -> 9.0

        Material.DIAMOND_SWORD -> 7.0
        Material.DIAMOND_SHOVEL -> 5.5
        Material.DIAMOND_PICKAXE -> 5.0
        Material.DIAMOND_AXE -> 9.0

        Material.NETHERITE_SWORD -> 8.0
        Material.NETHERITE_SHOVEL -> 6.5
        Material.NETHERITE_PICKAXE -> 6.0
        Material.NETHERITE_AXE -> 10.0

        Material.TRIDENT -> 9.0

        else -> 1.0
    }
}

fun Material.getBaseAttackSpeed(): Double {
    return when(this) {
        Material.WOODEN_SWORD -> 1.6
        Material.STONE_SWORD -> 1.6
        Material.IRON_SWORD -> 1.6
        Material.GOLDEN_SWORD -> 1.6
        Material.DIAMOND_SWORD -> 1.6
        Material.NETHERITE_SWORD -> 1.6

        Material.TRIDENT -> 1.1

        Material.WOODEN_SHOVEL -> 1.0
        Material.STONE_SHOVEL -> 1.0
        Material.IRON_SHOVEL -> 1.0
        Material.GOLDEN_SHOVEL -> 1.0
        Material.DIAMOND_SHOVEL -> 1.0
        Material.NETHERITE_SHOVEL -> 1.0

        Material.WOODEN_PICKAXE -> 1.2
        Material.STONE_PICKAXE -> 1.2
        Material.IRON_PICKAXE -> 1.2
        Material.GOLDEN_PICKAXE -> 1.2
        Material.DIAMOND_PICKAXE -> 1.2
        Material.NETHERITE_PICKAXE -> 1.2

        Material.WOODEN_AXE -> 0.8
        Material.STONE_AXE -> 0.8
        Material.IRON_AXE -> 0.9
        Material.GOLDEN_AXE -> 1.0
        Material.DIAMOND_AXE -> 1.0
        Material.NETHERITE_AXE -> 1.0

        Material.WOODEN_HOE -> 1.0
        Material.STONE_HOE -> 2.0
        Material.IRON_HOE -> 3.0
        Material.GOLDEN_HOE -> 1.0
        Material.DIAMOND_HOE -> 4.0
        Material.NETHERITE_HOE -> 4.0

        else -> 4.0
    }
}