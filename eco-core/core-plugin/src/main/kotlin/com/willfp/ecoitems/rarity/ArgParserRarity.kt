package com.willfp.ecoitems.rarity

import com.willfp.eco.core.items.args.LookupArgParser
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import java.util.function.Predicate

object ArgParserRarity : LookupArgParser {
    override fun parseArguments(args: Array<out String>?, meta: ItemMeta): Predicate<ItemStack>? {
        val rarityArg = args?.firstOrNull { it.startsWith("rarity:") } ?: return null
        val rarity = Rarities[rarityArg.split(":").getOrNull(1)] ?: return null

        meta.persistentDataContainer.nbtRarity = rarity

        return Predicate { it.ecoItemRarity == rarity }
    }
}
