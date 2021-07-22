package com.willfp.ecoweapons.conditions.conditions;

import com.willfp.ecoweapons.conditions.Condition;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ConditionInWater extends Condition<Boolean> {
    public ConditionInWater() {
        super("in-water", Boolean.class);
    }

    @Override
    public boolean isConditionMet(@NotNull final Player player,
                                  @NotNull final Boolean value) {
        return (player.getLocation().getBlock().getType() == Material.WATER) == value;
    }
}
