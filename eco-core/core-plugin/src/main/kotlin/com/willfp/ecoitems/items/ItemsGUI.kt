package com.willfp.ecoitems.items

import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.core.gui.GUIComponent
import com.willfp.eco.core.gui.addPageChanger
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.menu.MenuBuilder
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

/**
 * The /ecoitems gui. Folders under items/ are categories: with more than one,
 * the GUI opens on a category menu and each folder is a sub-menu; with none,
 * it's the flat item list.
 */
object ItemsGUI {
    private lateinit var mainMenu: Menu
    private var categoryMenus = mapOf<String, Menu>()

    private data class Category(val path: String, val name: String, val items: List<EcoItem>)

    fun reload() {
        val folders = itemFolders()
        val grouped = EcoItems.values()
            .sortedBy { it.id.key }
            .groupBy { folders[it.id.key] ?: "" }

        if (grouped.size <= 1) {
            categoryMenus = emptyMap()
            mainMenu = itemsMenu(
                grouped.values.firstOrNull().orEmpty(),
                plugin.configYml.getFormattedString("items-gui.title"),
                showBack = false
            )
            return
        }

        val categories = grouped.entries
            .map { (path, items) -> Category(path, categoryName(path), items) }
            .sortedBy { it.name }

        categoryMenus = categories.associate { category ->
            category.path to itemsMenu(
                category.items,
                plugin.configYml.getFormattedString("items-gui.category-title")
                    .replace("%category%", category.name),
                showBack = true
            )
        }

        mainMenu = menu(plugin.configYml.getInt("items-gui.rows")) {
            frame(plugin.configYml.getFormattedString("items-gui.title"))

            val pane = CategoryPane(categories)
            addComponent(
                plugin.configYml.getInt("items-gui.item-area.row"),
                plugin.configYml.getInt("items-gui.item-area.column"),
                pane
            )

            maxPages {
                if (categories.isEmpty()) 0 else ceil(categories.size.toDouble() / pane.size).toInt()
            }
        }
    }

    fun open(player: Player) = mainMenu.open(player)

    private fun openCategory(player: Player, path: String) {
        categoryMenus[path]?.open(player)
    }

    /** Item id -> folder path relative to items/ ("" at the root). */
    private fun itemFolders(): Map<String, String> {
        val root = plugin.dataFolder.resolve("items")
        if (!root.isDirectory) {
            return emptyMap()
        }

        return root.walkTopDown()
            .filter { it.isFile && it.extension == "yml" && !it.name.startsWith("_") }
            .associate {
                it.nameWithoutExtension to it.parentFile.relativeTo(root).invariantSeparatorsPath
            }
    }

    private fun categoryName(path: String): String {
        if (path.isEmpty()) {
            return plugin.configYml.getFormattedString("items-gui.root-category-name")
        }

        return path.substringAfterLast('/')
            .split('_', '-')
            .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
    }

    private fun itemsMenu(items: List<EcoItem>, plainTitle: String, showBack: Boolean): Menu =
        menu(plugin.configYml.getInt("items-gui.rows")) {
            frame(plainTitle)

            val pane = ItemScrollPane(items)
            addComponent(
                plugin.configYml.getInt("items-gui.item-area.row"),
                plugin.configYml.getInt("items-gui.item-area.column"),
                pane
            )

            maxPages {
                if (items.isEmpty()) 0 else ceil(items.size.toDouble() / pane.size).toInt()
            }

            if (showBack && plugin.configYml.getBool("items-gui.back-button.enabled")) {
                val fallback = ItemStackBuilder(
                    Items.lookup(plugin.configYml.getString("items-gui.back-button.item"))
                )
                    .setDisplayName(plugin.configYml.getFormattedString("items-gui.back-button.name"))
                    .build()
                val back = PackFeatures.instance?.decorateGuiItem(
                    plugin,
                    fallback,
                    plugin.configYml.getStringOrNull("items-gui.back-button.model")
                ) ?: fallback

                setSlot(
                    plugin.configYml.getInt("items-gui.back-button.row"),
                    plugin.configYml.getInt("items-gui.back-button.column"),
                    slot(back) { onLeftClick { event, _ -> mainMenu.open(event.whoClicked as Player) } }
                )
            }
        }

    /** The shared shell: title, mask, page changers, close button, custom slots. */
    private fun MenuBuilder.frame(plainTitle: String) {
        val background = plugin.configYml.getString("items-gui.background.glyph")
            .takeIf { plugin.configYml.getBool("items-gui.background.enabled") }
        title = PackFeatures.instance?.decorateGuiTitle(plugin, plainTitle, background) ?: plainTitle

        setMask(
            FillerMask(
                MaskItems.fromItemNames(plugin.configYml.getStrings("items-gui.mask.items")),
                *plugin.configYml.getStrings("items-gui.mask.pattern").toTypedArray()
            )
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

        for (config in plugin.configYml.getSubsections("items-gui.custom-slots")) {
            setSlot(config.getInt("row"), config.getInt("column"), ConfigSlot(config))
        }
    }

    private class CategoryPane(private val categories: List<Category>) : GUIComponent {
        private val empty = slot(Items.lookup(plugin.configYml.getString("items-gui.empty-item")))

        override fun getSlotAt(row: Int, column: Int, player: Player, menu: Menu): Slot {
            val index = column + ((row - 1) * columns) - 1
            val category = categories.getOrNull(index + size * (menu.getPage(player) - 1)) ?: return empty

            val icon = ItemStackBuilder(category.items.first().itemStack)
                .setDisplayName(
                    plugin.configYml.getFormattedString("items-gui.category-item.name")
                        .replace("%category%", category.name)
                )
                .addLoreLines(
                    plugin.configYml.getFormattedStrings("items-gui.category-item.lore").map {
                        it.replace("%count%", category.items.size.toString())
                    }
                )
                .build()

            return slot(icon) {
                onLeftClick { event, _ -> openCategory(event.whoClicked as Player, category.path) }
            }
        }

        override fun getRows() = plugin.configYml.getInt("items-gui.item-area.height")
        override fun getColumns() = plugin.configYml.getInt("items-gui.item-area.width")

        val size = rows * columns
    }
}

private class ItemScrollPane(private val items: List<EcoItem>) : GUIComponent {
    private val empty = slot(Items.lookup(plugin.configYml.getString("items-gui.empty-item")))

    override fun getSlotAt(row: Int, column: Int, player: Player, menu: Menu): Slot {
        val index = column + ((row - 1) * columns) - 1
        val item = items.getOrNull(index + size * (menu.getPage(player) - 1)) ?: return empty

        return slot(item.itemStack) {
            onLeftClick { event, _ ->
                val itemStack = item.itemStack
                itemStack.amount = 1

                DropQueue(event.whoClicked as Player)
                    .addItem(itemStack)
                    .forceTelekinesis()
                    .push()
            }
        }
    }

    override fun getRows() = plugin.configYml.getInt("items-gui.item-area.height")
    override fun getColumns() = plugin.configYml.getInt("items-gui.item-area.width")

    val size = rows * columns
}
