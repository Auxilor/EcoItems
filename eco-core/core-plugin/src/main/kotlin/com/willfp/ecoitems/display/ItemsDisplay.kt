package com.willfp.ecoitems.display

import com.willfp.eco.util.formatEcoRich
import com.willfp.ecoitems.items.ecoItem
import com.willfp.ecoitems.items.isRenamed
import com.willfp.ecoitems.plugin
import net.kyori.adventure.text.Component
import wiki.ecohub.game.item.FastItemStack
import wiki.ecohub.game.item.fast
import wiki.ecohub.rules.holder.ItemProvidedHolder
import wiki.ecohub.ui.display.DisplayContext
import wiki.ecohub.ui.display.DisplayModule
import wiki.ecohub.ui.display.DisplayPriority
import wiki.ecohub.ui.text.formatEco

object ItemsDisplay : DisplayModule(plugin, DisplayPriority.LOWEST) {
    override fun display(context: DisplayContext) {
        val itemStack = context.itemStack
        val fis = itemStack.fast()
        val ecoItem = fis.ecoItem ?: return

        val lore = ecoItem.lore.formatEcoRich(context.placeholderContext).toMutableList()

        val player = context.player

        if (player != null) {
            val lines = ItemProvidedHolder(ecoItem, itemStack).getNotMetLineComponents(player)

            if (lines.isNotEmpty()) {
                lore.add(Component.empty())
                lore.addAll(lines)
            }
        }

        context.lore.prepend(lore)

        if (ecoItem.displayName != null && !itemStack.isRenamed) {
            val formatted = ecoItem.displayName.formatEco(context.placeholderContext)
            if (fis.displayName != formatted) {
                fis.displayName = formatted
            }
        }

        fis.addItemFlags(*FastItemStack.wrap(ecoItem.itemStack).itemFlags.toTypedArray())
    }
}
