package com.willfp.ecoitems.rarity

import com.willfp.eco.util.formatEcoRich
import org.bukkit.inventory.ItemStack
import wiki.ecohub.config.Config
import wiki.ecohub.core.registry.KRegistrable
import wiki.ecohub.game.item.Items
import wiki.ecohub.game.item.lookup.EmptyTestableItem

class Rarity(
    override val id: String,
    val config: Config
) : KRegistrable {
    val items = config.getStrings("items")
        .map { Items.lookup(it) }
        .filterNot { it is EmptyTestableItem }

    val weight = config.getInt("weight")

    val lore = config.getFormattedStrings("lore")

    val loreComponents = config.getStrings("lore").formatEcoRich()

    val tag = RarityTag(this)

    init {
        if (id != "item") {
            Items.registerTag(tag)
        }
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
