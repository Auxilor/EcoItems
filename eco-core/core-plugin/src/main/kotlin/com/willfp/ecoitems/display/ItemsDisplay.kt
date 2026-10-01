package com.willfp.ecoitems.display

import com.willfp.eco.core.display.DisplayContext
import com.willfp.eco.core.display.DisplayModule
import com.willfp.eco.core.display.DisplayPriority
import com.willfp.eco.core.fast.FastItemStack
import com.willfp.eco.core.fast.fast
import com.willfp.eco.util.formatEco
import com.willfp.eco.util.formatEcoRich
import com.willfp.ecoitems.items.ecoItem
import com.willfp.ecoitems.plugin
import com.willfp.libreforge.ItemProvidedHolder
import net.kyori.adventure.text.Component

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

        if (ecoItem.displayName != null) {
            val formatted = ecoItem.displayName.formatEco(context.placeholderContext)
            if (fis.displayName != formatted) {
                fis.displayName = formatted
            }
        }

        fis.addItemFlags(*FastItemStack.wrap(ecoItem.itemStack).itemFlags.toTypedArray())
    }
}
