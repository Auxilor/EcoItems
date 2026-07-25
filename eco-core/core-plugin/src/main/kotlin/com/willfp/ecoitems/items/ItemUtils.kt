package com.willfp.ecoitems.items

import com.willfp.eco.core.fast.FastItemStack
import com.willfp.eco.core.fast.fast
import com.willfp.eco.util.namespacedKeyOf
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

        val legacyId = pdc.get(legacyKey, PersistentDataType.STRING)
        if (legacyId != null) {
            pdc.remove(legacyKey)
            pdc.set(key, PersistentDataType.STRING, legacyId)
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
