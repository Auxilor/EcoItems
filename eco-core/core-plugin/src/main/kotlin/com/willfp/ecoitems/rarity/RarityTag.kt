package com.willfp.ecoitems.rarity

import com.willfp.ecoitems.plugin
import org.bukkit.inventory.ItemStack
import wiki.ecohub.game.item.lookup.CustomItemTag

class RarityTag(
    private val rarity: Rarity
) : CustomItemTag(plugin.createNamespacedKey(rarity.id)) {
    override fun matches(p0: ItemStack): Boolean {
        return p0.ecoItemRarity == rarity
    }

    override fun getExampleItem(): ItemStack? = rarity.items.randomOrNull()?.getItem()
}
