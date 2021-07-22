package com.willfp.ecoweapons.conditions.conditions;

import com.willfp.ecoweapons.conditions.Condition;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ConditionHasPermission extends Condition<String> {
    public ConditionHasPermission() {
        super("has-permission", String.class);
    }

    @Override
    public boolean isConditionMet(@NotNull final Player player,
                                  @NotNull final String value) {
        return player.hasPermission(value);
    }
}
