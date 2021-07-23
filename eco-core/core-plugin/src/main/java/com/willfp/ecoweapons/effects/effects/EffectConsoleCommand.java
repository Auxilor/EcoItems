package com.willfp.ecoweapons.effects.effects;

import com.willfp.eco.core.config.interfaces.JSONConfig;
import com.willfp.ecoweapons.effects.Effect;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

public class EffectConsoleCommand extends Effect {
    public EffectConsoleCommand() {
        super("console-command");
    }

    @Override
    public void handleMeleeAttack(@NotNull final Player player,
                                  @NotNull final LivingEntity victim,
                                  @NotNull final EntityDamageByEntityEvent event,
                                  @NotNull final JSONConfig args) {
        Bukkit.getServer().dispatchCommand(
                Bukkit.getConsoleSender(),
                args.getString("command", false).replace("%player%", player.getName())
        );
    }

    @Override
    public void handleProjectileHitEntity(@NotNull final Player player,
                                          @NotNull final LivingEntity victim,
                                          @NotNull final Projectile projectile,
                                          @NotNull final ProjectileHitEvent event,
                                          @NotNull final JSONConfig args) {
        Bukkit.getServer().dispatchCommand(
                Bukkit.getConsoleSender(),
                args.getString("command", false).replace("%player%", player.getName())
        );
    }

    @Override
    public void handleProjectileHit(@NotNull final Player player,
                                    @NotNull final Projectile projectile,
                                    @NotNull final ProjectileHitEvent event,
                                    @NotNull final JSONConfig args) {
        Bukkit.getServer().dispatchCommand(
                Bukkit.getConsoleSender(),
                args.getString("command", false).replace("%player%", player.getName())
        );
    }

    @Override
    public void handleAltClick(@NotNull final Player player,
                               @NotNull final RayTraceResult blockRay,
                               @NotNull final RayTraceResult entityRay,
                               @NotNull final PlayerInteractEvent event,
                               @NotNull final JSONConfig args) {
        Bukkit.getServer().dispatchCommand(
                Bukkit.getConsoleSender(),
                args.getString("command", false).replace("%player%", player.getName())
        );
    }
}
