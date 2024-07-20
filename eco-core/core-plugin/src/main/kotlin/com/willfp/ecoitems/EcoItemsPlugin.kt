package com.willfp.ecoitems

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.core.display.DisplayModule
import com.willfp.eco.core.items.Items
import com.willfp.eco.core.items.tag.CustomItemTag
import com.willfp.ecoitems.commands.CommandEcoItems
import com.willfp.ecoitems.display.ItemsDisplay
import com.willfp.ecoitems.display.RarityDisplay
import com.willfp.ecoitems.items.EcoItemFinder
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.items.EcoItemsRecipes
import com.willfp.ecoitems.items.ItemAttributeListener
import com.willfp.ecoitems.items.ItemListener
import com.willfp.ecoitems.items.ecoItem
import com.willfp.ecoitems.libreforge.ConditionHasEcoItem
import com.willfp.ecoitems.rarity.ArgParserRarity
import com.willfp.ecoitems.rarity.Rarities
import com.willfp.ecoitems.util.DiscoverRecipeListener
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.ConfigCategory
import com.willfp.libreforge.registerHolderProvider
import org.bukkit.event.Listener

internal lateinit var plugin: EcoItemsPlugin
    private set

class EcoItemsPlugin : LibreforgePlugin() {
    init {
        plugin = this
    }

    override fun handleEnable() {
        Items.registerArgParser(ArgParserRarity)
        Items.registerTag(CustomItemTag(this.createNamespacedKey("item")) {
            it.ecoItem != null
        })

        Conditions.register(ConditionHasEcoItem)

        registerHolderProvider(EcoItemFinder.toHolderProvider())
    }

    override fun loadConfigCategories(): List<ConfigCategory> {
        return listOf(
            Rarities,
            EcoItems,
            EcoItemsRecipes
        )
    }

    override fun loadListeners(): List<Listener> {
        return listOf(
            DiscoverRecipeListener(this),
            ItemListener,
            ItemAttributeListener(this)
        )
    }

    override fun loadPluginCommands(): List<PluginCommand> {
        return listOf(
            CommandEcoItems(this)
        )
    }

    override fun loadDisplayModules(): List<DisplayModule> {
        return listOf(
            ItemsDisplay(this),
            RarityDisplay(this)
        )
    }
}
