package com.willfp.ecoitems.items

import com.willfp.eco.core.fast.FastItemStack
import com.willfp.eco.core.fast.fast
import com.willfp.eco.util.namespacedKeyOf
import com.willfp.libreforge.ItemProvidedHolder
import com.willfp.libreforge.slot.SlotType
import com.willfp.libreforge.slot.SlotTypes
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

private val legacyKey = namespacedKeyOf("ecoweapons", "weapon")
private val key = namespacedKeyOf("ecoitems", "item")

var ItemStack?.ecoItem: EcoItem?
    get() {
        this ?: return null
        val fis = this.fast()
        return fis.ecoItem
    }
    set(value) {
        this ?: return
        val fis = this.fast()
        fis.ecoItem = value
    }

var FastItemStack.ecoItem: EcoItem?
    get() {
        val pdc = this.persistentDataContainer

        if (pdc.get(legacyKey, PersistentDataType.STRING) != null) {
            pdc.remove(legacyKey)
            pdc.set(key, PersistentDataType.STRING, pdc.get(legacyKey, PersistentDataType.STRING)!!)
        }

        return EcoItems.getByID(pdc.get(key, PersistentDataType.STRING))
    }
    set(value) {
        val pdc = this.persistentDataContainer

        if (value == null) {
            pdc.remove(key)
        } else {
            pdc.set(key, PersistentDataType.STRING, value.id.key)
        }
    }

val Material.baseDamage: Double
    get() {
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

val Material.baseAttackSpeed: Double
    get() {
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
