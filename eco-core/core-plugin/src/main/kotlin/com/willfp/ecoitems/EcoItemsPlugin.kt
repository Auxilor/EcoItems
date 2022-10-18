package com.willfp.ecoitems

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.core.display.DisplayModule
import com.willfp.ecoitems.commands.CommandEcoItems
import com.willfp.ecoitems.display.ItemsDisplay
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.items.ItemAttributeListener
import com.willfp.ecoitems.items.ItemListener
import com.willfp.ecoitems.items.ItemUtils
import com.willfp.ecoitems.util.DiscoverRecipeListener
import com.willfp.libreforge.LibReforgePlugin
import org.bukkit.event.Listener

class EcoItemsPlugin : LibReforgePlugin() {
    /**
     * Internal constructor called by bukkit on plugin load.
     */
    init {
        instance = this
        registerHolderProvider { ItemUtils.getEcoItemsOnPlayer(it) }
    }

    override fun handleEnableAdditional() {
        this.copyConfigs("items")

        EcoItems.update(this) // Preliminary update
    }

    override fun loadListeners(): List<Listener> {
        return listOf(
            DiscoverRecipeListener(this),
            ItemListener(this),
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

    override fun getMinimumEcoVersion(): String {
        return "6.35.1"
    }

    companion object {
        /**
         * Instance of EcoItems.
         */
        @JvmStatic
        lateinit var instance: EcoItemsPlugin
    }
}
