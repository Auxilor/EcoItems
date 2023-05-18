package com.willfp.ecoitems.slot

import com.willfp.eco.core.registry.Registry
import com.willfp.ecoitems.slot.impl.ItemSlotAny
import com.willfp.ecoitems.slot.impl.ItemSlotBoots
import com.willfp.ecoitems.slot.impl.ItemSlotChestplate
import com.willfp.ecoitems.slot.impl.ItemSlotHands
import com.willfp.ecoitems.slot.impl.ItemSlotHelmet
import com.willfp.ecoitems.slot.impl.ItemSlotLeggings
import com.willfp.ecoitems.slot.impl.ItemSlotMainhand
import com.willfp.ecoitems.slot.impl.ItemSlotOffhand

object ItemSlots : Registry<ItemSlot>() {
    fun getByID(id: String?): ItemSlot {
        if (id == null) {
            return ItemSlotHands // Legacy
        }

        return get(id) ?: ItemSlotMainhand
    }

    init {
        register(ItemSlotAny)
        register(ItemSlotBoots)
        register(ItemSlotChestplate)
        register(ItemSlotHands)
        register(ItemSlotHelmet)
        register(ItemSlotLeggings)
        register(ItemSlotMainhand)
        register(ItemSlotOffhand)
    }
}
