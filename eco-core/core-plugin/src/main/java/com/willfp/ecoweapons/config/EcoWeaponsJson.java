package com.willfp.ecoweapons.config;

import com.willfp.eco.core.EcoPlugin;
import com.willfp.eco.core.config.json.JSONStaticBaseConfig;
import org.jetbrains.annotations.NotNull;

public class EcoWeaponsJson extends JSONStaticBaseConfig {
    /**
     * Create tiers.json.
     *
     * @param plugin Instance of EcoWeapons.
     */
    public EcoWeaponsJson(@NotNull final EcoPlugin plugin) {
        super("ecoweapons", plugin);
    }
}
