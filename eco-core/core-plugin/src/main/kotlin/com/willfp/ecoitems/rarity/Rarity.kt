package com.willfp.ecoitems.rarity

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.core.display.Display
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.tag.CustomItemTag
import com.willfp.eco.core.recipe.parts.EmptyTestableItem
import com.willfp.eco.core.registry.KRegistrable
import com.willfp.ecoitems.EcoItemsPlugin
import org.bukkit.inventory.ItemStack

class Rarity(
    override val id: String,
    val config: Config,
    val plugin: EcoItemsPlugin
) : KRegistrable {
    val items = config.getStrings("items")
        .map { Items.lookup(it) }
        .filterNot { it is EmptyTestableItem }

    val weight = config.getInt("weight")

    val lore = config.getFormattedStrings("lore")

    val displayLore = lore.map { Display.PREFIX + it }

    val tag = CustomItemTag(plugin.createNamespacedKey(id)) {
        it.ecoItemRarity == this
    }

    init {
        Items.registerTag(tag)
    }

    fun matches(item: ItemStack): Boolean {
        return items.any { it.matches(item) }
    }

    override fun equals(other: Any?): Boolean {
        return other is Rarity && other.id == this.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
