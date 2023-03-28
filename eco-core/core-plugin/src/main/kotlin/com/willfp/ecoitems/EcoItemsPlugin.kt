package com.willfp.ecoitems

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.core.display.DisplayModule
import com.willfp.ecoitems.commands.CommandEcoItems
import com.willfp.ecoitems.display.ItemsDisplay
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.items.EcoItemsRecipes
import com.willfp.ecoitems.items.ItemAttributeListener
import com.willfp.ecoitems.items.ItemListener
import com.willfp.ecoitems.items.ItemUtils
import com.willfp.ecoitems.libreforge.ConditionHasEcoItem
import com.willfp.ecoitems.util.DiscoverRecipeListener
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.loader.LibreforgePlugin
import com.willfp.libreforge.loader.configs.ConfigCategory
import com.willfp.libreforge.registerHolderProvider
import org.bukkit.event.Listener

class EcoItemsPlugin : LibreforgePlugin() {
    /**
     * Internal constructor called by bukkit on plugin load.
     */
    init {
        instance = this
    }

    override fun handleEnable() {
        Conditions.register(ConditionHasEcoItem)

        registerHolderProvider { ItemUtils.getEcoItemsOnPlayer(it) }
    }

    override fun loadConfigCategories(): List<ConfigCategory> {
        return listOf(
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

    override fun createDisplayModule(): DisplayModule {
        return ItemsDisplay(this)
    }

    companion object {
        /**
         * Instance of EcoItems.
         */
        @JvmStatic
        lateinit var instance: EcoItemsPlugin
    }
}
