package com.willfp.ecoweapons.weapons;


import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.willfp.eco.core.config.interfaces.JSONConfig;
import com.willfp.eco.core.config.updating.ConfigUpdater;
import com.willfp.ecoweapons.EcoWeaponsPlugin;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@UtilityClass
public class Weapons {
    /**
     * Registered weapons.
     */
    private static final BiMap<String, Weapon> BY_NAME = HashBiMap.create();

    /**
     * Get all registered {@link Weapon}s.
     *
     * @return A list of all {@link Weapon}s.
     */
    public static List<Weapon> values() {
        return ImmutableList.copyOf(BY_NAME.values());
    }

    /**
     * Get {@link Weapon} matching name.
     *
     * @param name The name to search for.
     * @return The matching {@link Weapon}, or null if not found.
     */
    @Nullable
    public static Weapon getByName(@NotNull final String name) {
        return BY_NAME.get(name);
    }

    /**
     * Update all {@link Weapon}s.
     *
     * @param plugin Instance of EcoWeapons.
     */
    @ConfigUpdater
    public static void update(@NotNull final EcoWeaponsPlugin plugin) {
        for (Weapon weapon : values()) {
            removeWeapon(weapon);
        }

        for (JSONConfig setConfig : plugin.getEcoWeaponsJson().getSubsections("weapons")) {
            addNewWeapon(new Weapon(setConfig, plugin));
        }
    }

    /**
     * Add new {@link Weapon} to EcoWeapons.
     *
     * @param weapon The {@link Weapon} to add.
     */
    public static void addNewWeapon(@NotNull final Weapon weapon) {
        BY_NAME.remove(weapon.getName());
        BY_NAME.put(weapon.getName(), weapon);
    }

    /**
     * Remove {@link Weapon} from EcoWeapons.
     *
     * @param weapon The {@link Weapon} to remove.
     */
    public static void removeWeapon(@NotNull final Weapon weapon) {
        BY_NAME.remove(weapon.getName());
    }
}
