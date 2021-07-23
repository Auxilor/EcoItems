package com.willfp.ecoweapons.effects.effects;

import com.willfp.eco.core.config.interfaces.JSONConfig;
import com.willfp.ecoweapons.effects.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EffectRailgun extends Effect {
    public EffectRailgun() {
        super("railgun");
    }

    @Override
    public void handleAltClick(@NotNull final Player player,
                               @NotNull final RayTraceResult blockRay,
                               @Nullable final RayTraceResult entityRay,
                               @NotNull final PlayerInteractEvent event,
                               @NotNull final JSONConfig args) {
        double damage = args.getDouble("damage");
        Particle particle = Particle.valueOf(args.getString("particle", false).toUpperCase());

        player.playSound(player.getLocation(), Sound.valueOf(args.getString("sound", false).toUpperCase()), 1, 1);

        showBeam(player.getLocation().add(0, player.getEyeHeight(), 0), blockRay, particle);

        if (entityRay.getHitEntity() instanceof LivingEntity victim) {
            victim.damage(damage, player);
        } else {
            Location location = blockRay.getHitPosition().toLocation(player.getWorld());
            assert location.getWorld() != null;

            for (Entity nearbyEntity : location.getWorld().getNearbyEntities(location, 2, 2, 2)) {
                if (nearbyEntity instanceof LivingEntity livingEntity) {
                    livingEntity.damage(damage, player);
                }
            }
        }
    }

    private void showBeam(@NotNull final Location playerLoc,
                          @NotNull final RayTraceResult rayTrace,
                          @NotNull final Particle particle) {
        Vector ray = rayTrace.getHitPosition().clone().subtract(playerLoc.toVector());
        Vector raySegment = ray.clone().normalize();
        Location pos = playerLoc.clone();
        World world = pos.getWorld();
        assert world != null;

        double rayLength = ray.length();
        for (int i = 0; i < rayLength; i++) {
            pos.add(raySegment);
            world.spawnParticle(particle, pos, 1, 0, 0, 0, 0, null, true);
        }
    }
}
