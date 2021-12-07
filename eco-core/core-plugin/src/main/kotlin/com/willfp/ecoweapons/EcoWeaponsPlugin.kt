package com.willfp.ecoweapons

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.command.impl.PluginCommand
import com.willfp.eco.core.display.DisplayModule
import com.willfp.eco.core.integrations.IntegrationLoader
import com.willfp.ecoweapons.commands.CommandEcoweapons
import com.willfp.ecoweapons.config.EcoWeaponsYml
import com.willfp.ecoweapons.display.WeaponsDisplay
import com.willfp.ecoweapons.fuels.FuelHandler
import com.willfp.ecoweapons.util.DiscoverRecipeListener
import com.willfp.ecoweapons.weapons.WeaponListener
import com.willfp.ecoweapons.weapons.WeaponModifierListener
import com.willfp.ecoweapons.weapons.WeaponUtils
import com.willfp.ecoweapons.weapons.toSingletonList
import com.willfp.libreforge.LibReforge
import org.bukkit.event.Listener

class EcoWeaponsPlugin : EcoPlugin(1241, 12134, "&#ff0000") {
    /**
     * ecoweapons.yml.
     */
    val ecoWeaponsYml: EcoWeaponsYml

    /**
     * Internal constructor called by bukkit on plugin load.
     */
    init {
        instance = this
        ecoWeaponsYml = EcoWeaponsYml(this)
        LibReforge.init(this)
        LibReforge.registerHolderProvider { WeaponUtils.getWeaponOnPlayer(it).toSingletonList() }
    }

    override fun handleEnable() {
        LibReforge.enable(this)
    }

    override fun handleDisable() {
        LibReforge.disable(this)
    }

    override fun handleReload() {
        LibReforge.reload(this)
        FuelHandler.createRunnable(this)
    }

    override fun loadListeners(): List<Listener> {
        return listOf(
            DiscoverRecipeListener(this),
            WeaponListener(this),
            FuelHandler(),
            WeaponModifierListener(this)
        )
    }

    override fun loadIntegrationLoaders(): List<IntegrationLoader> {
        return LibReforge.getIntegrationLoaders()
    }

    override fun loadPluginCommands(): List<PluginCommand> {
        return listOf(
            CommandEcoweapons(this)
        )
    }

    override fun createDisplayModule(): DisplayModule {
        return WeaponsDisplay(this)
    }

    override fun getMinimumEcoVersion(): String {
        return "6.15.0"
    }

    companion object {
        /**
         * Instance of EcoWeapons.
         */
        @JvmStatic
        lateinit var instance: EcoWeaponsPlugin
    }
}