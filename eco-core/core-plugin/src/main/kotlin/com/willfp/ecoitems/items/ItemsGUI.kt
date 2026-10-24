package com.willfp.ecoitems.items

import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.core.gui.addCustomSlots
import com.willfp.eco.core.gui.addPageChanger
import com.willfp.eco.core.gui.component.ConfigButton
import com.willfp.eco.core.gui.component.PagedAreaComponent
import com.willfp.eco.core.gui.component.addComponent
import com.willfp.eco.core.gui.component.addPagedArea
import com.willfp.eco.core.gui.component.closeButton
import com.willfp.eco.core.gui.getSlotArea
import com.willfp.eco.core.gui.getSlotLocations
import com.willfp.eco.core.gui.menu
import com.willfp.eco.core.gui.menu.Menu
import com.willfp.eco.core.gui.menu.MenuBuilder
import com.willfp.eco.core.gui.page.PageChanger
import com.willfp.eco.core.gui.setMask
import com.willfp.eco.core.gui.slot
import com.willfp.eco.core.gui.slot.Slot
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.builder.ItemStackBuilder
import com.willfp.eco.core.sound.PlayableSound
import com.willfp.ecoitems.pack.PackFeatures
import com.willfp.ecoitems.plugin
import org.bukkit.entity.Player

/**
 * The /ecoitems gui. Folders under items/ are categories: with more than one,
 * the GUI opens on a category menu and each folder is a sub-menu; with none,
 * it's the flat item list.
 */
object ItemsGUI {
    // Swapped on reload, read by whichever thread opens the GUI.
    @Volatile
    private lateinit var mainMenu: Menu

    @Volatile
    private var categoryMenus = mapOf<String, Menu>()

    private data class Category(val path: String, val name: String, val items: List<EcoItem>)

    fun reload() {
        val folders = itemFolders()
        val grouped = EcoItems.values()
            .filterNot { it.excludeFromGui }
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

            addPagedArea(CategoryPane(categories))
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

            addPagedArea(ItemScrollPane(items))

            if (showBack) {
                addComponent(
                    ConfigButton(
                        plugin.configYml.getSubsection("items-gui.back-button"),
                        enabledByDefault = false,
                        itemModifier = { item ->
                            PackFeatures.instance?.decorateGuiItem(
                                plugin,
                                item,
                                plugin.configYml.getStringOrNull("items-gui.back-button.model")
                            ) ?: item
                        }
                    ) { player, _, _, _ -> mainMenu.open(player) }
                )
            }
        }

    /** The shared shell: title, mask, page changers, close button, custom slots. */
    private fun MenuBuilder.frame(plainTitle: String) {
        val background = plugin.configYml.getString("items-gui.background.glyph")
            .takeIf { plugin.configYml.getBool("items-gui.background.enabled") }
        title = PackFeatures.instance?.decorateGuiTitle(plugin, plainTitle, background) ?: plainTitle

        setMask(plugin.configYml, "items-gui")

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

            // A locked-model swaps in when there's no page to turn to;
            // without one the button hides at the first/last page.
            val locked = plugin.configYml.getStringOrNull("$path.locked-model")?.let {
                PackFeatures.instance?.decorateGuiItem(plugin, fallback.clone(), it)
            }

            for (location in plugin.configYml.getSlotLocations(path)) {
                addPageChanger(
                    direction,
                    active,
                    locked,
                    pageSound,
                    location.row,
                    location.column
                )
            }
        }

        addComponent(
            closeButton(
                plugin.configYml.getSubsection("items-gui.close-button"),
                enabledByDefault = false,
                itemModifier = { item ->
                    PackFeatures.instance?.decorateGuiItem(
                        plugin,
                        item,
                        plugin.configYml.getStringOrNull("items-gui.close-button.model")
                    ) ?: item
                }
            )
        )

        addCustomSlots(plugin.configYml, "items-gui")
    }

    private class CategoryPane(private val categories: List<Category>) :
        PagedAreaComponent<Category>(plugin.configYml.getSlotArea("items-gui.item-area")) {
        private val empty = slot(Items.lookup(plugin.configYml.getString("items-gui.empty-item")))

        override fun getEntries(player: Player, menu: Menu) = categories

        override fun getEmptySlot(player: Player, menu: Menu) = empty

        override fun getEntrySlot(player: Player, menu: Menu, entry: Category): Slot {
            val icon = ItemStackBuilder(entry.items.first().itemStack)
                .setDisplayName(
                    plugin.configYml.getFormattedString("items-gui.category-item.name")
                        .replace("%category%", entry.name)
                )
                .addLoreLines(
                    plugin.configYml.getFormattedStrings("items-gui.category-item.lore").map {
                        it.replace("%count%", entry.items.size.toString())
                    }
                )
                .build()

            return slot(icon) {
                onLeftClick { event, _ -> openCategory(event.whoClicked as Player, entry.path) }
            }
        }
    }
}

private class ItemScrollPane(private val items: List<EcoItem>) :
    PagedAreaComponent<EcoItem>(plugin.configYml.getSlotArea("items-gui.item-area")) {
    private val empty = slot(Items.lookup(plugin.configYml.getString("items-gui.empty-item")))

    override fun getEntries(player: Player, menu: Menu) = items

    override fun getEmptySlot(player: Player, menu: Menu) = empty

    override fun getEntrySlot(player: Player, menu: Menu, entry: EcoItem): Slot =
        slot(entry.itemStack) {
            onLeftClick { event, _ ->
                val itemStack = entry.itemStack
                itemStack.amount = 1

                DropQueue(event.whoClicked as Player)
                    .addItem(itemStack)
                    .forceTelekinesis()
                    .push()
            }
        }
}
