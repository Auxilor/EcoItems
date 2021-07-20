package com.willfp.ecoweapons;

import com.willfp.eco.core.EcoPlugin;
import com.willfp.ecoweapons.config.EcoWeaponsJson;
import lombok.Getter;
import org.bukkit.event.Listener;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class EcoWeaponsPlugin extends EcoPlugin {
    /**
     * Instance of EcoWeapons.
     */
    @Getter
    private static EcoWeaponsPlugin instance;

    /**
     * tiers.json.
     */
    @Getter
    private final EcoWeaponsJson ecoWeaponsJson;

    /**
     * Internal constructor called by bukkit on plugin load.
     */
    public EcoWeaponsPlugin() {
        super(88246, 12134, "&#ff0000");
        instance = this;

        this.ecoWeaponsJson = new EcoWeaponsJson(this);
    }

    @Override
    protected List<Listener> loadListeners() {
        return Arrays.asList(

        );
    }
}
