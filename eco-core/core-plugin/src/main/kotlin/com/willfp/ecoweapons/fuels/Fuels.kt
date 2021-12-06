package com.willfp.ecoweapons.fuels

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableList
import com.willfp.eco.core.config.updating.ConfigUpdater
import com.willfp.ecoweapons.EcoWeaponsPlugin

object Fuels {
    /**
     * Registered fuels.
     */
    private val BY_ID: BiMap<String, Fuel> = HashBiMap.create()

    val CONDITION_HAS_FUEL = ConditionHasFuel()

    /**
     * Get all registered [Fuel]s.
     *
     * @return A list of all [Fuel]s.
     */
    @JvmStatic
    fun values(): List<Fuel> {
        return ImmutableList.copyOf(BY_ID.values)
    }

    /**
     * Get [Fuel] matching id.
     *
     * @param name The id to search for.
     * @return The matching [Fuel], or null if not found.
     */
    @JvmStatic
    fun getByID(name: String): Fuel? {
        return BY_ID[name]
    }

    /**
     * Update all [Fuel]s.
     *
     * @param plugin Instance of EcoWeapons.
     */
    @ConfigUpdater
    @JvmStatic
    fun update(plugin: EcoWeaponsPlugin) {
        for (fuel in values()) {
            removeFuel(fuel)
        }
        for (setConfig in plugin.ecoWeaponsYml.getSubsections("fuels")) {
            addNewFuel(Fuel(setConfig, plugin))
        }
    }

    /**
     * Add new [Fuel] to EcoWeapons.
     *
     * @param fuel The [Fuel] to add.
     */
    @JvmStatic
    fun addNewFuel(fuel: Fuel) {
        BY_ID.remove(fuel.id)
        BY_ID[fuel.id] = fuel
    }

    /**
     * Remove [Fuel] from EcoWeapons.
     *
     * @param weapon The [Fuel] to remove.
     */
    @JvmStatic
    fun removeFuel(fuel: Fuel) {
        BY_ID.remove(fuel.id)
    }
}
