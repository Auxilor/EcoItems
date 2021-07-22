package com.willfp.ecoweapons.conditions.conditions;

import com.willfp.ecoweapons.conditions.Condition;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ConditionBelowXPLevel extends Condition<Integer> {
    public ConditionBelowXPLevel() {
        super("below-xp-level", Integer.class);
    }

    @Override
    public boolean isConditionMet(@NotNull final Player player,
                                  @NotNull final Integer value) {
        return player.getLevel() < value;
    }
}
