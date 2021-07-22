package com.willfp.ecoweapons.effects;

import com.willfp.eco.core.config.interfaces.JSONConfig;
import com.willfp.ecoweapons.EcoWeaponsPlugin;
import com.willfp.ecoweapons.weapons.Weapon;
import lombok.AccessLevel;
import lombok.Getter;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class Effect {
    /**
     * Instance of EcoWeapons.
     */
    @Getter(AccessLevel.PROTECTED)
    private final EcoWeaponsPlugin plugin = EcoWeaponsPlugin.getInstance();

    /**
     * The name of the effect.
     */
    @Getter
    private final String name;

    /**
     * The cooldown end times linked to players.
     */
    private final Map<UUID, Long> tracker = new HashMap<>();

    /**
     * Create a new effect.
     *
     * @param name The effect name.
     */
    protected Effect(@NotNull final String name) {
        this.name = name;

        Effects.addNewEffect(this);
    }

    /**
     * Handle {@link TriggerType#MELEE_ATTACK}.
     *
     * @param player The player.
     * @param victim The victim.
     * @param event  The event.
     * @param args   The effect args.
     */
    public void handleMeleeAttack(@NotNull final Player player,
                                  @NotNull final LivingEntity victim,
                                  @NotNull final EntityDamageByEntityEvent event,
                                  @NotNull final JSONConfig args) {
        // Override when needed.
    }

    /**
     * Handle {@link TriggerType#PROJECTILE_HIT_ENTITY}.
     *
     * @param player     The player.
     * @param victim     The victim.
     * @param projectile The projectile.
     * @param event      The event.
     * @param args       The effect args.
     */
    public void handleProjectileHitEntity(@NotNull final Player player,
                                          @NotNull final LivingEntity victim,
                                          @NotNull final Projectile projectile,
                                          @NotNull final ProjectileHitEvent event,
                                          @NotNull final JSONConfig args) {
        // Override when needed.
    }

    /**
     * Handle {@link TriggerType#PROJECTILE_HIT}.
     *
     * @param player     The player.
     * @param projectile The projectile.
     * @param event      The event.
     * @param args       The effect args.
     */
    public void handleProjectileHit(@NotNull final Player player,
                                    @NotNull final Projectile projectile,
                                    @NotNull final ProjectileHitEvent event,
                                    @NotNull final JSONConfig args) {
        // Override when needed.
    }

    /**
     * Handle {@link TriggerType#ALT_CLICK} and {@link TriggerType#SHIFT_ALT_CLICK}.
     *
     * @param player    The player.
     * @param blockRay  The block ray.
     * @param entityRay The entity ray.
     * @param event     The event.
     * @param args      The effect args.
     */
    public void handleAltClick(@NotNull final Player player,
                               @NotNull final RayTraceResult blockRay,
                               @NotNull final RayTraceResult entityRay,
                               @NotNull final PlayerInteractEvent event,
                               @NotNull final JSONConfig args) {
        // Override when needed.
    }

    /**
     * Utility method to get a player's cooldown time of a specific effect.
     *
     * @param player The player to query.
     * @return The time left in seconds before next use.
     */
    public int getCooldown(@NotNull final Player player) {
        if (!tracker.containsKey(player.getUniqueId())) {
            return 0;
        }

        long msLeft = tracker.get(player.getUniqueId()) - System.currentTimeMillis();

        long secondsLeft = (long) Math.ceil((double) msLeft / 1000);

        return NumberConversions.toInt(secondsLeft);
    }

    /**
     * Handle cooldowns.
     *
     * @param player      The player.
     * @param weapon      The weapon.
     * @param triggerType The trigger type.
     * @return If the cooldown is over.
     */
    public boolean isCooldownOver(@NotNull final Player player,
                                  @NotNull final Weapon weapon,
                                  @NotNull final TriggerType triggerType) {
        int cooldown = getCooldown(player);

        if (cooldown > 0) {
            if (this.getPlugin().getConfigYml().getBool("cooldown-in-actionbar")) {
                String message = this.getPlugin().getLangYml().getString("messages.on-cooldown").replace("%seconds%", String.valueOf(cooldown));

                player.spigot().sendMessage(
                        ChatMessageType.ACTION_BAR,
                        TextComponent.fromLegacyText(message)
                );
            } else {
                String message = this.getPlugin().getLangYml().getMessage("on-cooldown").replace("%seconds%", String.valueOf(cooldown));
                player.sendMessage(message);
            }
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 0.5f);

            return false;
        } else {
            tracker.remove(player.getUniqueId());
            tracker.put(player.getUniqueId(), System.currentTimeMillis() + (long) ((weapon.getCooldownTime(this, triggerType) * 1000L)));
            return true;
        }
    }
}
