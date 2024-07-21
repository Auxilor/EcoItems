package com.willfp.ecoitems.items

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.items.tag.CustomItemTag
import org.bukkit.inventory.ItemStack

class EcoItemTag(plugin: EcoPlugin): CustomItemTag(plugin.createNamespacedKey("item")) {
    override fun matches(p0: ItemStack): Boolean {
        return p0.ecoItem != null
    }

    override fun getExampleItem(): ItemStack? {
        return EcoItems.values().randomOrNull()?.itemStack
    }
}
