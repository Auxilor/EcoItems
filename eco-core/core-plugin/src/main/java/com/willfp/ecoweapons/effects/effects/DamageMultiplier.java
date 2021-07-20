package com.willfp.ecoweapons.effects.effects;

import com.willfp.ecoweapons.effects.Effect;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;

public class DamageMultiplier extends Effect<Double> {
    public DamageMultiplier() {
        super("damage-multiplier", Double.class);
    }

    @EventHandler
    public void listener(@NotNull final EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player attacker = null;
        if (event.getDamager() instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) event.getDamager()).getShooter();
            if (shooter == null) {
                return;
            }

            if (shooter instanceof Player) {
                attacker = (Player) shooter;
            }
        } else if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        }

        if (attacker == null) {
            return;
        }

        this.applyIfEnabled(attacker, multiplier -> event.setDamage(event.getDamage() * multiplier));
    }
}
