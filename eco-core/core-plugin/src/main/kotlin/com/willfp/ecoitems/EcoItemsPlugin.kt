package com.willfp.ecoitems

import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.core.display.DisplayModule
import com.willfp.ecoitems.commands.CommandEcoItems
import com.willfp.ecoitems.config.ItemsYml
import com.willfp.ecoitems.display.ItemsDisplay
import com.willfp.ecoitems.fuels.FuelHandler
import com.willfp.ecoitems.items.ItemAttributeListener
import com.willfp.ecoitems.items.ItemListener
import com.willfp.ecoitems.items.ItemUtils
import com.willfp.ecoitems.items.toSingletonList
import com.willfp.ecoitems.util.DiscoverRecipeListener
import com.willfp.libreforge.LibReforgePlugin
import com.willfp.libreforge.chains.EffectChains
import org.bukkit.event.Listener

class EcoItemsPlugin : LibReforgePlugin(1241, 12205, "&#ff0000") {
    /**
     * items.yml.
     */
    val itemsYml: ItemsYml

    /**
     * Internal constructor called by bukkit on plugin load.
     */
    init {
        instance = this
        itemsYml = ItemsYml(this)
        registerHolderProvider { ItemUtils.getEcoItemOnPlayer(it).toSingletonList() }
    }

    override fun handleEnableAdditional() {
        itemsYml.getSubsections("chains").mapNotNull {
            EffectChains.compile(it, "Chains")
        }
    }

    override fun handleReloadAdditional() {
        FuelHandler.createRunnable(this)
    }

    override fun loadListeners(): List<Listener> {
        return listOf(
            DiscoverRecipeListener(this),
            ItemListener(this),
            FuelHandler(),
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
        return "6.19.0"
    }

    companion object {
        /**
         * Instance of EcoItems.
         */
        @JvmStatic
        lateinit var instance: EcoItemsPlugin
    }
}