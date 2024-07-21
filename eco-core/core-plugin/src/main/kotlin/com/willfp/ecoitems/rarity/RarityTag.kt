package com.willfp.ecoitems.rarity

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.items.tag.CustomItemTag
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.items.ecoItem
import org.bukkit.inventory.ItemStack

class RarityTag(
    plugin: EcoPlugin,
    private val rarity: Rarity
) : CustomItemTag(plugin.createNamespacedKey(rarity.id)) {
    override fun matches(p0: ItemStack): Boolean {
        return p0.ecoItemRarity == rarity
    }

    override fun getExampleItem(): ItemStack? {
        return rarity.items.randomOrNull()?.item
    }
}
