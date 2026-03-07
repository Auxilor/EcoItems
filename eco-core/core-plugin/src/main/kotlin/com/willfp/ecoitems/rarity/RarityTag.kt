package com.willfp.ecoitems.rarity

import com.willfp.eco.core.items.tag.CustomItemTag
import com.willfp.ecoitems.plugin
import org.bukkit.inventory.ItemStack

class RarityTag(
    private val rarity: Rarity
) : CustomItemTag(plugin.createNamespacedKey(rarity.id)) {
    override fun matches(p0: ItemStack): Boolean {
        return p0.ecoItemRarity == rarity
    }

    override fun getExampleItem(): ItemStack? {
        return rarity.items.randomOrNull()?.item
    }
}
