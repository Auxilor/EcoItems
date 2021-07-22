package com.willfp.ecoweapons.effects.util;

import com.willfp.ecoweapons.effects.Effect;
import com.willfp.ecoweapons.effects.TriggerType;
import com.willfp.ecoweapons.weapons.Weapon;
import com.willfp.ecoweapons.weapons.util.WeaponUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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

        if (WeaponUtils.areConditionsMet(player)) {
            return;
        }

        for (Effect effect : weapon.getEffects(TriggerType.MELEE_ATTACK)) {
            effect.handleMeleeAttack(player, victim, event, weapon.getEffectStrength(effect, TriggerType.MELEE_ATTACK));
        }
    }
}
