package com.willfp.ecoitems.display

import com.willfp.eco.core.display.DisplayContext
import com.willfp.eco.core.display.DisplayModule
import com.willfp.eco.core.display.DisplayPriority
import com.willfp.ecoitems.plugin
import com.willfp.ecoitems.rarity.Rarities
import com.willfp.ecoitems.rarity.ecoItemRarity
import net.kyori.adventure.text.Component

object RarityDisplay : DisplayModule(plugin, DisplayPriority.HIGHEST.weight + 1) {
    override fun display(context: DisplayContext) {
        if (!plugin.configYml.getBool("rarity.enabled")) {
            return
        }

        if (context.properties.inGui) {
            return
        }

        val baseRarity = context.itemStack.ecoItemRarity

        if (baseRarity == null) {
            if (!plugin.configYml.getBool("rarity.display-default")) {
                return
            }
        }

        if (baseRarity?.id == "none") {
            return
        }

        val rarity = baseRarity ?: Rarities.defaultRarity

        if (plugin.configYml.getBool("rarity.blank-lore-line")) {
            context.lore.append(listOf(Component.empty()) + rarity.loreComponents)
        } else {
            context.lore.append(rarity.loreComponents)
        }
    }
}
