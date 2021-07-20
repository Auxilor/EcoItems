package com.willfp.ecoweapons.effects.effects;

import com.willfp.ecoweapons.effects.Effect;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Boss;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("deprecation")
public class BossDamageMultiplier extends Effect<Double> {
    public BossDamageMultiplier() {
        super("boss-damage-multiplier", Double.class);
    }

    @EventHandler
    public void listener(@NotNull final EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Entity victim = event.getEntity();

        if (!(victim instanceof Boss || victim instanceof ElderGuardian) && !victim.getPersistentDataContainer().has(new NamespacedKey("ecobosses", "boss"), PersistentDataType.STRING)) {
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
