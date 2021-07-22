package com.willfp.ecoweapons.effects.effects;

import com.willfp.eco.util.LightningUtils;
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
        Map<String, Integer> argMap = (Map<String, Integer>) args;

        for (int i = 0; i < argMap.get("amount"); i++) {
            LightningUtils.strike(victim, argMap.get("damage"));
        }
    }

    @Override
    public void handleProjectileHit(@NotNull final Player player,
                                    @NotNull final Projectile projectile,
                                    @NotNull final ProjectileHitEvent event,
                                    @NotNull final Object args) {
        Map<String, Integer> argMap = (Map<String, Integer>) args;

        for (int i = 0; i < argMap.get("amount"); i++) {
            World world = projectile.getLocation().getWorld();
            assert world != null;
            world.strikeLightning(projectile.getLocation());
        }
    }
}
