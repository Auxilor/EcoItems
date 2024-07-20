package com.willfp.ecoitems.display

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.display.Display
import com.willfp.eco.core.display.DisplayModule
import com.willfp.eco.core.display.DisplayPriority
import com.willfp.eco.core.display.DisplayProperties
import com.willfp.eco.core.fast.fast
import com.willfp.ecoitems.rarity.Rarities
import com.willfp.ecoitems.rarity.ecoItemRarity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class RarityDisplay(plugin: EcoPlugin) : DisplayModule(plugin, DisplayPriority.HIGHEST.weight + 1) {
    override fun display(itemStack: ItemStack, player: Player?, properties: DisplayProperties, vararg args: Any) {
        if (!plugin.configYml.getBool("rarity.enabled")) {
            return
        }

        if (properties.inGui) {
            return
        }

        val baseRarity = itemStack.ecoItemRarity

        if (baseRarity == null) {
            if (!plugin.configYml.getBool("rarity.display-default")) {
                return
            }
        }

        if (baseRarity?.id == "none") {
            return
        }

        val rarity = baseRarity ?: Rarities.defaultRarity

        val fis = itemStack.fast()
        val lore = fis.lore.toMutableList()
        if (plugin.configYml.getBool("rarity.blank-lore-line")) {
            lore += Display.PREFIX
        }
        lore += rarity.displayLore
        fis.lore = lore
    }
}
