package com.willfp.ecoweapons.effects.effects;

import com.willfp.eco.core.config.interfaces.JSONConfig;
import com.willfp.ecoweapons.effects.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

public class EffectStrikeLightning extends Effect {
    public EffectStrikeLightning() {
        super("strike-lightning");
    }

    @Override
    public void handleMeleeAttack(@NotNull final Player player,
                                  @NotNull final LivingEntity victim,
                                  @NotNull final EntityDamageByEntityEvent event,
                                  @NotNull final JSONConfig args) {
        handle(victim.getLocation(), args);
    }

    @Override
    public void handleProjectileHit(@NotNull final Player player,
                                    @NotNull final Projectile projectile,
                                    @NotNull final ProjectileHitEvent event,
                                    @NotNull final JSONConfig args) {
        handle(projectile.getLocation(), args);
    }

    @Override
    public void handleProjectileHitEntity(@NotNull final Player player,
                                          @NotNull final LivingEntity victim,
                                          @NotNull final Projectile projectile,
                                          @NotNull final ProjectileHitEvent event,
                                          @NotNull final JSONConfig args) {
        handle(victim.getLocation(), args);
    }

    @Override
    public void handleAltClick(@NotNull final Player player,
                               @NotNull final RayTraceResult rayTrace,
                               @NotNull final PlayerInteractEvent event,
                               @NotNull final JSONConfig args) {
        handle(rayTrace.getHitPosition().toLocation(player.getWorld()), args);
    }

    private void handle(@NotNull final Location location,
                        @NotNull final JSONConfig args) {
        World world = location.getWorld();
        assert world != null;

        for (int i = 0; i < args.getDouble("amount"); i++) {
            this.getPlugin().getScheduler().runLater(() -> {
                world.strikeLightning(location);
            }, i);
        }
    }
}
