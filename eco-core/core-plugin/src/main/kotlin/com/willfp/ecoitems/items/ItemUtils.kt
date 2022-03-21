package com.willfp.ecoitems.items

import com.willfp.eco.util.NamespacedKeyUtils
import com.willfp.ecoitems.EcoItemsPlugin.Companion.instance
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

object ItemUtils {
    /**
     * Instance of EcoItems.
     */
    private val PLUGIN = instance

    private val legacyKey = NamespacedKeyUtils.create("ecoweapons", "weapon")

    /**
     * Get EcoItem from an item.
     *
     * @param itemStack The itemStack to check.
     * @return The EcoItem, or null if no EcoItem is found.
     */
    fun getEcoItem(itemStack: ItemStack?): EcoItem? {
        itemStack ?: return null
        val meta = itemStack.itemMeta ?: return null
        val item = getEcoItem(meta)
        itemStack.itemMeta = meta
        return item
    }

    /**
     * Get EcoItem from an item.
     *
     * @param meta The itemStack to check.
     * @return The EcoItem, or null if no EcoItem is found.
     */
    fun getEcoItem(meta: ItemMeta): EcoItem? {
        val container = meta.persistentDataContainer
        val legacy = container.get(
            legacyKey,
            PersistentDataType.STRING
        )

        if (legacy != null) {
            container.set(
                PLUGIN.namespacedKeyFactory.create("item"),
                PersistentDataType.STRING,
                legacy
            )
            container.remove(legacyKey)
        }

        val id = container.get(
            PLUGIN.namespacedKeyFactory.create("item"),
            PersistentDataType.STRING
        ) ?: return null

        return EcoItems.getByID(id)
    }

    /**
     * Get EcoItem on a player.
     *
     * @param player The player to check.
     * @return The EcoItem, or null if no EcoItem is found.
     */
    fun getEcoItemsOnPlayer(player: Player): List<EcoItem> {
        val list = mutableListOf<EcoItem>()

        val mainhand = getEcoItem(player.inventory.itemInMainHand)
        if (mainhand != null) {
            list.add(mainhand)
        }

        if (PLUGIN.configYml.getBool("check-offhand")) {
            val offhand = getEcoItem(player.inventory.itemInOffHand)
            if (offhand != null) {
                list.add(offhand)
            }
        }

        return list
    }
}

fun Material.getBaseDamage(): Double {
    return when (this) {
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
    return when (this) {
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
