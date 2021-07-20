package com.willfp.ecoweapons.effects.effects;

import com.willfp.ecoweapons.effects.Effect;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

public class FallDamageMultiplier extends Effect<Double> {
    public FallDamageMultiplier() {
        super("fall-damage-multiplier", Double.class);
    }

    @EventHandler
    public void listener(@NotNull final EntityDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        Player player = (Player) event.getEntity();

        this.applyIfEnabled(player, multiplier -> event.setDamage(event.getDamage() * multiplier));
    }
}
