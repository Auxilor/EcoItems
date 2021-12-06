package com.willfp.ecoweapons.weapons

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableList
import com.willfp.eco.core.config.updating.ConfigUpdater
import com.willfp.ecoweapons.EcoWeaponsPlugin
import com.willfp.ecoweapons.fuels.ConditionHasFuel

object Weapons {
    /**
     * Registered weapons.
     */
    private val BY_ID: BiMap<String, Weapon> = HashBiMap.create()

    val CONDITION_HAS_FUEL = ConditionHasFuel()

    /**
     * Get all registered [Weapon]s.
     *
     * @return A list of all [Weapon]s.
     */
    @JvmStatic
    fun values(): List<Weapon> {
        return ImmutableList.copyOf(BY_ID.values)
    }

    /**
     * Get [Weapon] matching id.
     *
     * @param name The id to search for.
     * @return The matching [Weapon], or null if not found.
     */
    @JvmStatic
    fun getByID(name: String): Weapon? {
        return BY_ID[name]
    }

    /**
     * Update all [Weapon]s.
     *
     * @param plugin Instance of EcoWeapons.
     */
    @ConfigUpdater
    @JvmStatic
    fun update(plugin: EcoWeaponsPlugin) {
        for (weapon in values()) {
            removeWeapon(weapon)
        }
        for (setConfig in plugin.ecoWeaponsYml.getSubsections("weapons")) {
            addNewWeapon(Weapon(setConfig, plugin))
        }
    }

    /**
     * Add new [Weapon] to EcoWeapons.
     *
     * @param weapon The [Weapon] to add.
     */
    @JvmStatic
    fun addNewWeapon(weapon: Weapon) {
        BY_ID.remove(weapon.id)
        BY_ID[weapon.id] = weapon
    }

    /**
     * Remove [Weapon] from EcoWeapons.
     *
     * @param weapon The [Weapon] to remove.
     */
    @JvmStatic
    fun removeWeapon(weapon: Weapon) {
        BY_ID.remove(weapon.id)
    }
}
