package com.willfp.ecoweapons.effects.effects;

import com.willfp.ecoweapons.effects.Effect;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;

public class BowDamageMultiplier extends Effect<Double> {
    public BowDamageMultiplier() {
        super("bow-damage-multiplier", Double.class);
    }

    @EventHandler
    public void listener(@NotNull final EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getDamager() instanceof Arrow)) {
            return;
        }

        ProjectileSource shooter = ((Projectile) event.getDamager()).getShooter();

        if (!(shooter instanceof Player)) {
            return;
        }

        Player player = (Player) shooter;

        this.applyIfEnabled(player, multiplier -> event.setDamage(event.getDamage() * multiplier));
    }
}
