package com.willfp.ecoweapons.effects.effects;

import com.willfp.eco.core.config.interfaces.JSONConfig;
import com.willfp.ecoweapons.effects.Effect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

public class EffectBleed extends Effect implements Listener {
    public EffectBleed() {
        super("spawn-helper");
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
        double bleedDamage = args.getDouble("damage");


        int bleedCount = args.getInt("amount");

        AtomicInteger currentBleedCount = new AtomicInteger(0);

        this.getPlugin().getRunnableFactory().create(bukkitRunnable -> {
            currentBleedCount.addAndGet(1);

            victim.damage(bleedDamage);

            if (currentBleedCount.get() >= bleedCount) {
                bukkitRunnable.cancel();
            }
        }).runTaskTimer(0, 10);
    }
}
