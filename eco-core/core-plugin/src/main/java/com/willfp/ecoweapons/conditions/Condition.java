package com.willfp.ecoweapons.conditions;

import com.willfp.ecoweapons.EcoWeaponsPlugin;
import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class Condition<T> {
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
     * The class of the config getter type.
     */
    @Getter
    private final Class<T> typeClass;

    /**
     * Create a new condition.
     *
     * @param name      The condition name.
     * @param typeClass The class of the config type.
     */
    protected Condition(@NotNull final String name,
                        @NotNull final Class<T> typeClass) {
        this.name = name;
        this.typeClass = typeClass;

        Conditions.addNewCondition(this);
    }

    /**
     * Get if condition is met for a player.
     *
     * @param player The player.
     * @param value  The value of the condition.
     * @return If met.
     */
    public final boolean isMet(@NotNull final Player player,
                               @NotNull final Object value) {
        return isConditionMet(player, typeClass.cast(value));
    }

    protected abstract boolean isConditionMet(@NotNull Player player,
                                              @NotNull T value);
}
