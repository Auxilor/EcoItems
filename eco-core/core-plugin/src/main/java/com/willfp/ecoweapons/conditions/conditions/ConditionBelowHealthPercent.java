package com.willfp.ecoweapons.conditions.conditions;

import com.willfp.ecoweapons.conditions.Condition;
import com.willfp.ecoweapons.sets.Weapon;
import com.willfp.ecoweapons.sets.util.WeaponUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.jetbrains.annotations.NotNull;

public class ConditionBelowHealthPercent extends Condition<Double> {
    public ConditionBelowHealthPercent() {
        super("below-health-percent", Double.class);
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void listener(@NotNull final EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        Weapon weapon = WeaponUtils.getWeaponFromItem(player.getInventory().getItemInMainHand());

        if (weapon == null) {
            return;
        }

        Double value = weapon.getConditionValue(this);

        if (value == null) {
            return;
        }

        evaluateEffects(player, value, weapon);
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void listener(@NotNull final EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        Weapon weapon = WeaponUtils.getWeaponFromItem(player.getInventory().getItemInMainHand());

        if (weapon == null) {
            return;
        }

        Double value = weapon.getConditionValue(this);

        if (value == null) {
            return;
        }

        evaluateEffects(player, value, weapon);
    }

    @Override
    public boolean isConditionMet(@NotNull final Player player,
                                  @NotNull final Double value) {
        double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        double health = player.getHealth();

        return (health / maxHealth) * 100 < value;
    }
}
