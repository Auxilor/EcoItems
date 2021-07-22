package com.willfp.ecoweapons.conditions.conditions;

import com.willfp.ecoweapons.conditions.Condition;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ConditionAboveHungerPercent extends Condition<Double> {
    public ConditionAboveHungerPercent() {
        super("above-hunger-percent", Double.class);
    }

    @Override
    public boolean isConditionMet(@NotNull final Player player,
                                  @NotNull final Double value) {
        double maxFood = 20;
        double food = player.getFoodLevel();

        return (food / maxFood) * 100 >= value;
    }
}
