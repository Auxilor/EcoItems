package com.willfp.ecoitems.display

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.display.Display
import com.willfp.eco.core.display.DisplayModule
import com.willfp.eco.core.display.DisplayPriority
import com.willfp.eco.core.fast.FastItemStack
import com.willfp.eco.core.fast.fast
import com.willfp.eco.util.StringUtils
import com.willfp.ecoitems.fuels.FuelUtils
import com.willfp.ecoitems.items.ItemUtils
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ItemsDisplay(plugin: EcoPlugin) : DisplayModule(plugin, DisplayPriority.LOWEST) {
    override fun display(
        itemStack: ItemStack,
        player: Player?,
        vararg args: Any
    ) {
        val fis = FastItemStack.wrap(itemStack)
        val ecoItem = ItemUtils.getEcoItem(itemStack)
        val fuel = FuelUtils.getFuelFromItem(itemStack)

        if (fuel != null) {
            val lore = fuel.itemStack.fast().lore

            lore.addAll(fis.lore)

            fis.lore = lore
            fis.displayName = fuel.itemStack.fast().displayName
        }

        if (ecoItem != null) {
            val itemFast = FastItemStack.wrap(ecoItem.itemStack)

            val lore = itemFast.lore.map { "${Display.PREFIX}${StringUtils.format(it, player)}" }.toMutableList()

            if (player != null) {
                val lines = ecoItem.getNotMetLines(player)

                if (lines.isNotEmpty()) {
                    lore.add("")
                    lore.addAll(lines.map { Display.PREFIX + it })
                }
            }

            lore.addAll(fis.lore)

            fis.displayName = itemFast.displayName
            fis.addItemFlags(*itemFast.itemFlags.toTypedArray())
            fis.lore = lore
        }
    }
}
