package com.willfp.ecoitems.items

import com.willfp.eco.core.fast.FastItemStack
import com.willfp.eco.core.fast.fast
import com.willfp.eco.util.namespacedKeyOf
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlot
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

private fun Material.defaultAddNumberSum(attribute: Attribute): Double =
    EquipmentSlot.values().sumOf { slot ->
        runCatching { getDefaultAttributeModifiers(slot).get(attribute) }
            .getOrNull()
            ?.filter { it.operation == AttributeModifier.Operation.ADD_NUMBER }
            ?.sumOf { it.amount }
            ?: 0.0
    }

val Material.attackDamageModifier: Double
    get() = defaultAddNumberSum(Attribute.ATTACK_DAMAGE)

val Material.attackSpeedModifier: Double
    get() = defaultAddNumberSum(Attribute.ATTACK_SPEED)
