package com.willfp.ecoitems.items

import com.willfp.eco.core.items.tag.CustomItemTag
import com.willfp.ecoitems.plugin
import org.bukkit.inventory.ItemStack

object EcoItemTag: CustomItemTag(plugin.createNamespacedKey("item")) {
    override fun matches(p0: ItemStack): Boolean {
        return p0.ecoItem != null
    }

    override fun getExampleItem(): ItemStack? {
        return EcoItems.values().randomOrNull()?.itemStack
    }
}
