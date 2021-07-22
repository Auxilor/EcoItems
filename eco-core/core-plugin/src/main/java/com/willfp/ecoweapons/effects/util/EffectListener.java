package com.willfp.ecoweapons.effects.util;

import com.willfp.eco.util.ArrowUtils;
import com.willfp.ecoweapons.effects.Effect;
import com.willfp.ecoweapons.effects.TriggerType;
import com.willfp.ecoweapons.weapons.Weapon;
import com.willfp.ecoweapons.weapons.util.WeaponUtils;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class EffectListener implements Listener {
    /**
     * Handle {@link TriggerType#MELEE_ATTACK}.
     *
     * @param event The event.
     */
    @EventHandler(
            ignoreCancelled = true
    )
    public void meleeAttackListener(@NotNull final EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }

        Weapon weapon = WeaponUtils.getWeaponFromItem(player.getInventory().getItemInMainHand());
        if (weapon == null) {
            return;
        }

        if (!WeaponUtils.areConditionsMet(player, weapon)) {
            return;
        }

        for (Effect effect : weapon.getEffects(TriggerType.MELEE_ATTACK)) {
            effect.handleMeleeAttack(player, victim, event, weapon.getEffectStrength(effect, TriggerType.MELEE_ATTACK));
        }
    }

    /**
     * Handle {@link TriggerType#PROJECTILE_HIT} and {@link TriggerType#PROJECTILE_HIT_ENTITY}.
     *
     * @param event The event.
     */
    @EventHandler(
            ignoreCancelled = true
    )
    public void projectileHitListener(@NotNull final ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident || event.getEntity() instanceof Arrow)) {
            return;
        }

        ItemStack item;

        if (event.getEntity() instanceof Trident trident) {
            item = trident.getItem();
        } else {
            item = ArrowUtils.getBow((Arrow) event.getEntity());
        }

        if (item == null) {
            return;
        }

        if (!(event.getEntity().getShooter() instanceof Player player)) {
            return;
        }

        Weapon weapon = WeaponUtils.getWeaponFromItem(item);
        if (weapon == null) {
            return;
        }

        if (!WeaponUtils.areConditionsMet(player, weapon)) {
            return;
        }

        if (event.getHitEntity() == null) {
            for (Effect effect : weapon.getEffects(TriggerType.PROJECTILE_HIT)) {
                effect.handleProjectileHit(player, event.getEntity(), event, weapon.getEffectStrength(effect, TriggerType.PROJECTILE_HIT));
            }
        } else {
            if (event.getHitEntity() instanceof LivingEntity victim) {
                for (Effect effect : weapon.getEffects(TriggerType.PROJECTILE_HIT)) {
                    effect.handleProjectileHitEntity(player, victim, event.getEntity(), event, weapon.getEffectStrength(effect, TriggerType.PROJECTILE_HIT));
                }
            }
        }
    }
    /**
     * Handle {@link TriggerType#ALT_CLICK} and {@link TriggerType#SHIFT_ALT_CLICK}.
     *
     * @param event The event.
     */
    @EventHandler(
            ignoreCancelled = true
    )
    public void altClickListener(@NotNull final PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = event.getItem();

        Weapon weapon = WeaponUtils.getWeaponFromItem(itemStack);
        if (weapon == null) {
            return;
        }

        if (!WeaponUtils.areConditionsMet(player, weapon)) {
            return;
        }

        if (player.isSneaking()) {
            for (Effect effect : weapon.getEffects(TriggerType.SHIFT_ALT_CLICK)) {
                effect.handleAltClick(player, event, weapon.getEffectStrength(effect, TriggerType.SHIFT_ALT_CLICK));
            }
        } else {
            for (Effect effect : weapon.getEffects(TriggerType.ALT_CLICK)) {
                effect.handleAltClick(player, event, weapon.getEffectStrength(effect, TriggerType.ALT_CLICK));
            }
        }
    }
}
