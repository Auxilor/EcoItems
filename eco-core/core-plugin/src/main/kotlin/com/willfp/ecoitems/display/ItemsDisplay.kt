package com.willfp.ecoitems.display

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
import wiki.ecohub.ui.text.StringUtils
import wiki.ecohub.ui.text.formatEco

object ItemsDisplay : DisplayModule(plugin, DisplayPriority.LOWEST) {
    override fun display(context: DisplayContext) {
        val itemStack = context.getItemStack()
        val fis = itemStack.fast()
        val ecoItem = fis.ecoItem ?: return

        val lore = ecoItem.lore
            .map { StringUtils.formatToRichComponent(it, context.getPlaceholderContext()) }
            .toMutableList()

        val player = context.getPlayer()

        if (player != null) {
            val lines = ItemProvidedHolder(ecoItem, itemStack).getNotMetLineComponents(player)

            if (lines.isNotEmpty()) {
                lore.add(Component.empty())
                lore.addAll(lines)
            }
        }

        context.getLore().prepend(lore)

        if (ecoItem.displayName != null && !itemStack.isRenamed) {
            val formatted = ecoItem.displayName.formatEco(context.getPlaceholderContext())
            if (fis.getDisplayName() != formatted) {
                fis.setDisplayName(formatted)
            }
        }

        FastItemStack.wrap(ecoItem.itemStack).getItemFlags().forEach { fis.addItemFlags(it) }
    }
}
