package com.willfp.ecoitems.items

import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.core.gui.GUIComponent
import com.willfp.eco.core.gui.addPageChanger
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.page.PageChanger
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.ConfigSlot
import com.willfp.eco.core.gui.slot.FillerMask
import com.willfp.eco.core.gui.slot.MaskItems
import com.willfp.eco.core.gui.slot.Slot
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.sound.PlayableSound
import com.willfp.ecoitems.pack.PackFeatures
import com.willfp.ecoitems.plugin
import org.bukkit.entity.Player
import kotlin.math.ceil

object ItemsGUI {
    private lateinit var menu: Menu
    private var items: List<EcoItem> = emptyList()

    fun reload() {
        items = EcoItems.values().sortedBy { it.id.key }

        menu = menu(plugin.configYml.getInt("items-gui.rows")) {
            val plainTitle = plugin.configYml.getFormattedString("items-gui.title")
            val background = plugin.configYml.getString("items-gui.background.glyph")
                .takeIf { plugin.configYml.getBool("items-gui.background.enabled") }
            title = PackFeatures.instance?.decorateGuiTitle(plugin, plainTitle, background) ?: plainTitle

            setMask(
                FillerMask(
                    MaskItems.fromItemNames(plugin.configYml.getStrings("items-gui.mask.items")),
                    *plugin.configYml.getStrings("items-gui.mask.pattern").toTypedArray()
                )
            )

            val pane = ItemScrollPane(items)
            addComponent(
                plugin.configYml.getInt("items-gui.item-area.row"),
                plugin.configYml.getInt("items-gui.item-area.column"),
                pane
            )

            val pageSound = PlayableSound.create(plugin.configYml.getSubsection("items-gui.page-change.sound"))
            for (direction in PageChanger.Direction.entries) {
                val name = direction.name.lowercase()
                val path = "items-gui.page-change.$name"
                val fallback = Items.lookup(plugin.configYml.getString("$path.item")).item
                val active = PackFeatures.instance?.decorateGuiItem(
                    plugin,
                    fallback,
                    plugin.configYml.getStringOrNull("$path.model")
                ) ?: fallback

                addPageChanger(
                    direction,
                    active,
                    null,
                    pageSound,
                    plugin.configYml.getInt("$path.row"),
                    plugin.configYml.getInt("$path.column")
                )
            }

            if (plugin.configYml.getBool("items-gui.close-button.enabled")) {
                val fallback = ItemStackBuilder(
                    Items.lookup(plugin.configYml.getString("items-gui.close-button.item"))
                )
                    .setDisplayName(plugin.configYml.getFormattedString("items-gui.close-button.name"))
                    .addLoreLines(plugin.configYml.getStrings("items-gui.close-button.lore"))
                    .build()
                val close = PackFeatures.instance?.decorateGuiItem(
                    plugin,
                    fallback,
                    plugin.configYml.getStringOrNull("items-gui.close-button.model")
                ) ?: fallback

                setSlot(
                    plugin.configYml.getInt("items-gui.close-button.row"),
                    plugin.configYml.getInt("items-gui.close-button.column"),
                    slot(close) { onLeftClick { event, _ -> event.whoClicked.closeInventory() } }
                )
            }

            maxPages {
                if (items.isEmpty()) 0 else ceil(items.size.toDouble() / pane.size).toInt()
            }

            for (config in plugin.configYml.getSubsections("items-gui.custom-slots")) {
                setSlot(config.getInt("row"), config.getInt("column"), ConfigSlot(config))
            }
        }
    }

    fun open(player: Player) = menu.open(player)
}

private class ItemScrollPane(private val items: List<EcoItem>) : GUIComponent {
    private val empty = slot(Items.lookup(plugin.configYml.getString("items-gui.empty-item")))

    override fun getSlotAt(row: Int, column: Int, player: Player, menu: Menu): Slot {
        val index = column + ((row - 1) * columns) - 1
        val item = items.getOrNull(index + size * (menu.getPage(player) - 1)) ?: return empty

        return slot(item.itemStack) {
            onLeftClick { event, _ ->
                DropQueue(event.whoClicked as Player)
                    .addItem(item.itemStack)
                    .forceTelekinesis()
                    .push()
            }
        }
    }

    override fun getRows() = plugin.configYml.getInt("items-gui.item-area.height")
    override fun getColumns() = plugin.configYml.getInt("items-gui.item-area.width")

    val size = rows * columns
}
