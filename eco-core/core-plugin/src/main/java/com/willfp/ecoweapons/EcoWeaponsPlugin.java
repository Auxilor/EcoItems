package com.willfp.ecoweapons;

import com.willfp.eco.core.EcoPlugin;
import com.willfp.eco.core.command.impl.PluginCommand;
import com.willfp.eco.core.display.DisplayModule;
import com.willfp.ecoweapons.commands.CommandEcoweapons;
import com.willfp.ecoweapons.config.EcoWeaponsJson;
import com.willfp.ecoweapons.display.WeaponsDisplay;
import com.willfp.ecoweapons.effects.Effect;
import com.willfp.ecoweapons.effects.Effects;
import com.willfp.ecoweapons.effects.util.EffectListener;
import com.willfp.ecoweapons.util.DiscoverRecipeListener;
import lombok.Getter;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

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
        super(1241, 12134, "&#ff0000");
        instance = this;

        this.ecoWeaponsJson = new EcoWeaponsJson(this);
    }

    @Override
    protected void handleEnable() {
        for (Effect effect : Effects.values()) {
            if (effect instanceof Listener) {
                this.getEventManager().registerListener((Listener) effect);
            }
        }
    }

    @Override
    protected List<Listener> loadListeners() {
        return Arrays.asList(
                new EffectListener(),
                new DiscoverRecipeListener(this)
        );
    }

    @Override
    protected List<PluginCommand> loadPluginCommands() {
        return Arrays.asList(
                new CommandEcoweapons(this)
        );
    }

    @Override
    protected @Nullable DisplayModule createDisplayModule() {
        return new WeaponsDisplay(this);
    }
}
