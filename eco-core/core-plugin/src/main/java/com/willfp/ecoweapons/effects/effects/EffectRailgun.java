package com.willfp.ecoweapons.effects.effects;

import com.willfp.eco.core.config.interfaces.JSONConfig;
import com.willfp.ecoweapons.effects.Effect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class EffectRailgun extends Effect {
    public EffectRailgun() {
        super("railgun");
    }

    @Override
    public void handleAltClick(@NotNull final Player player,
                               @NotNull final RayTraceResult rayTrace,
                               @NotNull final PlayerInteractEvent event,
                               @NotNull final JSONConfig args) {
        double damage = args.getDouble("damage");

        player.playSound(player.getLocation(), args.getString("sound", false).toUpperCase(), 1, 1);

        showBeam(player.getLocation(), rayTrace);

        if (rayTrace.getHitEntity() instanceof LivingEntity victim) {
            victim.damage(damage);
        }
    }

    private void showBeam(@NotNull final Location playerLoc,
                          @NotNull final RayTraceResult rayTrace) {
        Vector ray = rayTrace.getHitPosition().clone().subtract(playerLoc.toVector());
        Vector raySegment = ray.clone().normalize();
        Location pos = playerLoc.clone();
        World world = pos.getWorld();
        assert world != null;

        double rayLength = ray.length();
        for (int i = 0; i < rayLength; i++) {
            pos.add(raySegment);
            world.spawnParticle(Particle.HEART, pos, 1);
        }
    }
}
