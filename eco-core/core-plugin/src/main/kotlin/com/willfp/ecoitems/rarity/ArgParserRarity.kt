package com.willfp.ecoitems.rarity

import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import wiki.ecohub.game.item.editItemMeta
import wiki.ecohub.game.item.lookup.LookupArgParser
import java.util.function.Predicate

object ArgParserRarity : LookupArgParser {
    override fun parseArguments(args: Array<out String>, item: ItemStack): Predicate<ItemStack>? {
        val rarityArg = args.firstOrNull { it.startsWith("rarity:") } ?: return null
        val rarity = Rarities[rarityArg.split(":").getOrNull(1)] ?: return null

        item.editItemMeta<ItemMeta> { it.persistentDataContainer.nbtRarity = rarity }

        return Predicate { it.ecoItemRarity == rarity }
    }
}
