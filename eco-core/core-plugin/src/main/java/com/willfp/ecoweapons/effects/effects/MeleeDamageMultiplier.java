package com.willfp.ecoweapons.effects.effects;

import com.willfp.ecoweapons.effects.Effect;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

public class MeleeDamageMultiplier extends Effect<Double> {
    public MeleeDamageMultiplier() {
        super("melee-damage-multiplier", Double.class);
    }

    @EventHandler
    public void listener(@NotNull final EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player attacker = (Player) event.getDamager();

        this.applyIfEnabled(attacker, multiplier -> event.setDamage(event.getDamage() * multiplier));
    }
}
