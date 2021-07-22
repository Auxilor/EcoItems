package com.willfp.ecoweapons.effects.effects;

import com.willfp.ecoweapons.effects.Effect;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@SuppressWarnings("unchecked")
public class EffectStrikeLightning extends Effect {
    public EffectStrikeLightning() {
        super("strike-lightning");
    }

    @Override
    public void handleMeleeAttack(@NotNull final Player player,
                                  @NotNull final LivingEntity victim,
                                  @NotNull final EntityDamageByEntityEvent event,
                                  @NotNull final Object args) {
        Map<String, Double> argMap = (Map<String, Double>) args;
        World world = victim.getLocation().getWorld();
        assert world != null;

        for (int i = 0; i < argMap.get("amount"); i++) {
            this.getPlugin().getScheduler().runLater(() -> {
                world.strikeLightning(victim.getLocation());
            }, i);
        }
    }

    @Override
    public void handleProjectileHit(@NotNull final Player player,
                                    @NotNull final Projectile projectile,
                                    @NotNull final ProjectileHitEvent event,
                                    @NotNull final Object args) {
        Map<String, Double> argMap = (Map<String, Double>) args;
        World world = projectile.getLocation().getWorld();
        assert world != null;

        for (int i = 0; i < argMap.get("amount"); i++) {
            this.getPlugin().getScheduler().runLater(() -> {
                world.strikeLightning(projectile.getLocation());
            }, i);
        }
    }
}
