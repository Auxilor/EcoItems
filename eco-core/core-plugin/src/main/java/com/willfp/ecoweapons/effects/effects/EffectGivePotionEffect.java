package com.willfp.ecoweapons.effects.effects;

import com.willfp.eco.core.config.interfaces.JSONConfig;
import com.willfp.ecoweapons.effects.Effect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class EffectGivePotionEffect extends Effect implements Listener {
    public EffectGivePotionEffect() {
        super("give-potion-effect");
    }

    @Override
    public void handleMeleeAttack(@NotNull final Player player,
                                  @NotNull final LivingEntity victim,
                                  @NotNull final EntityDamageByEntityEvent event,
                                  @NotNull final JSONConfig args) {
        handle(player, victim, args);
    }

    @Override
    public void handleProjectileHitEntity(@NotNull final Player player,
                                          @NotNull final LivingEntity victim,
                                          @NotNull final Projectile projectile,
                                          @NotNull final ProjectileHitEvent event,
                                          @NotNull final JSONConfig args) {
        handle(player, victim, args);
    }

    private void handle(@NotNull final Player player,
                        @NotNull final LivingEntity victim,
                        @NotNull final JSONConfig args) {
        PotionEffectType type = PotionEffectType.getByName(args.getString("potion").toUpperCase());
        int duration = args.getInt("duration");
        int strength = args.getInt("strength") - 1;

        victim.addPotionEffect(
                new PotionEffect(
                        type,
                        duration,
                        strength
                )
        );
    }
}
