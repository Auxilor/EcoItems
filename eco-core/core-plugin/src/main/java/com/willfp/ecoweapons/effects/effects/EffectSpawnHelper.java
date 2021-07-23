package com.willfp.ecoweapons.effects.effects;

import com.willfp.eco.core.config.interfaces.JSONConfig;
import com.willfp.eco.util.NumberUtils;
import com.willfp.ecoweapons.effects.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EffectSpawnHelper extends Effect implements Listener {
    public EffectSpawnHelper() {
        super("spawn-helper");
    }

    @Override
    public void handleMeleeAttack(@NotNull final Player player,
                                  @NotNull final LivingEntity victim,
                                  @NotNull final EntityDamageByEntityEvent event,
                                  @NotNull final JSONConfig args) {
        doSpawn(victim, victim.getLocation(), args);
    }

    @Override
    public void handleProjectileHitEntity(@NotNull final Player player,
                                          @NotNull final LivingEntity victim,
                                          @NotNull final Projectile projectile,
                                          @NotNull final ProjectileHitEvent event,
                                          @NotNull final JSONConfig args) {
        doSpawn(victim, victim.getLocation(), args);
    }

    @Override
    public void handleProjectileHit(@NotNull final Player player,
                                    @NotNull final Projectile projectile,
                                    @NotNull final ProjectileHitEvent event,
                                    @NotNull final JSONConfig args) {
        doSpawn(null, projectile.getLocation(), args);
    }

    @Override
    public void handleAltClick(@NotNull final Player player,
                               @NotNull final RayTraceResult blockRay,
                               @NotNull final RayTraceResult entityRay,
                               @NotNull final PlayerInteractEvent event,
                               @NotNull final JSONConfig args) {
        doSpawn(entityRay.getHitEntity(), blockRay.getHitPosition().toLocation(player.getWorld()), args);
    }

    private void doSpawn(@Nullable final Entity victim,
                         @NotNull final Location location,
                         @NotNull final JSONConfig args) {
        double chance = args.getDouble("chance");

        if (NumberUtils.randInt(0, 100) > chance) {
            return;
        }

        if (victim != null) {
            if (!victim.getMetadata("eco-target").isEmpty()) {
                return;
            }
        }

        World world = location.getWorld();
        assert world != null;

        int toSpawn = args.getInt("amount");
        int ticksToLive = args.getInt("ticks-to-live");
        double health = args.getDouble("health");
        double range = args.getDouble("range");

        EntityType entityType = EntityType.valueOf(args.getString("entity").toUpperCase());

        for (int i = 0; i < toSpawn; i++) {
            Location locToSpawn = location.clone().add(NumberUtils.randFloat(-range, range), NumberUtils.randFloat(0, range), NumberUtils.randFloat(-range, range));
            Mob entity = (Mob) world.spawnEntity(locToSpawn, entityType);

            if (victim instanceof LivingEntity) {
                entity.setTarget((LivingEntity) victim);
            }
            if (health > entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) {
                health = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            }
            entity.setHealth(health);
            if (victim != null) {
                entity.setMetadata("eco-target", this.getPlugin().getMetadataValueFactory().create(victim));
            }
            this.getPlugin().getScheduler().runLater(entity::remove, ticksToLive);
        }
    }

    @EventHandler
    public void onSwitchTarget(@NotNull final EntityTargetEvent event) {
        if (event.getEntity().getMetadata("eco-target").isEmpty()) {
            return;
        }

        LivingEntity target = (LivingEntity) event.getEntity().getMetadata("eco-target").get(0).value();
        event.setTarget(target);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDropItem(@NotNull final EntityDeathEvent event) {
        if (event.getEntity().getMetadata("eco-target").isEmpty()) {
            return;
        }

        event.getDrops().clear();
        event.setDroppedExp(0);
    }
}
