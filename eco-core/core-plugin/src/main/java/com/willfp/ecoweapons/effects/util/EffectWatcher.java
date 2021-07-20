package com.willfp.ecoweapons.effects.util;

import com.willfp.eco.core.EcoPlugin;
import com.willfp.eco.core.PluginDependent;
import com.willfp.ecoweapons.effects.Effect;
import com.willfp.ecoweapons.effects.Effects;
import com.willfp.ecoweapons.sets.Weapon;
import com.willfp.ecoweapons.sets.util.WeaponUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class EffectWatcher extends PluginDependent<EcoPlugin> implements Listener {
    /**
     * Pass an {@link EcoPlugin} in order to interface with it.
     *
     * @param plugin The plugin to manage.
     */
    public EffectWatcher(@NotNull final EcoPlugin plugin) {
        super(plugin);
    }

    /**
     * Listener for item equipping.
     *
     * @param event The event to listen for.
     */
    @EventHandler
    public void itemHoldListener(@NotNull final PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        this.getPlugin().getScheduler().runLater(() -> {
            ItemStack hand = player.getInventory().getItemInMainHand();

            Weapon weapon = WeaponUtils.getWeaponFromItem(hand);

            boolean conditionsMet = WeaponUtils.areConditionsMet(player);

            for (Effect<?> effect : Effects.values()) {
                boolean enabled = true;

                if (weapon == null) {
                    effect.disable(player);
                    continue;
                }

                Object strength = weapon.getEffectStrength(effect);

                if (strength == null) {
                    enabled = false;
                }

                if (!conditionsMet) {
                    enabled = false;
                }

                if (enabled) {
                    effect.enable(player, strength);
                } else {
                    effect.disable(player);
                }
            }
        }, 1);
    }
}
