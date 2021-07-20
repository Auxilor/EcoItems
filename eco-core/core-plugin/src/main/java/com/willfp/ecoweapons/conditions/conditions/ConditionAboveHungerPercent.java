package com.willfp.ecoweapons.conditions.conditions;

import com.willfp.ecoweapons.conditions.Condition;
import com.willfp.ecoweapons.sets.Weapon;
import com.willfp.ecoweapons.sets.util.WeaponUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.jetbrains.annotations.NotNull;

public class ConditionAboveHungerPercent extends Condition<Double> {
    public ConditionAboveHungerPercent() {
        super("above-hunger-percent", Double.class);
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void listener(@NotNull final FoodLevelChangeEvent event) {
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
        double maxFood = 20;
        double food = player.getFoodLevel();

        return (food / maxFood) * 100 >= value;
    }
}
