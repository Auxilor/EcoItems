package com.willfp.ecoweapons.conditions;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.willfp.ecoweapons.conditions.conditions.ConditionAboveHealthPercent;
import com.willfp.ecoweapons.conditions.conditions.ConditionAboveHungerPercent;
import com.willfp.ecoweapons.conditions.conditions.ConditionAboveXPLevel;
import com.willfp.ecoweapons.conditions.conditions.ConditionAboveY;
import com.willfp.ecoweapons.conditions.conditions.ConditionBelowHealthPercent;
import com.willfp.ecoweapons.conditions.conditions.ConditionBelowHungerPercent;
import com.willfp.ecoweapons.conditions.conditions.ConditionBelowXPLevel;
import com.willfp.ecoweapons.conditions.conditions.ConditionBelowY;
import com.willfp.ecoweapons.conditions.conditions.ConditionHasPermission;
import com.willfp.ecoweapons.conditions.conditions.ConditionInBiome;
import com.willfp.ecoweapons.conditions.conditions.ConditionInWater;
import com.willfp.ecoweapons.conditions.conditions.ConditionInWorld;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@UtilityClass
@SuppressWarnings("unused")
public class Conditions {
    /**
     * All registered effects.
     */
    private static final BiMap<String, Condition<?>> BY_NAME = HashBiMap.create();

    public static final Condition<?> BELOW_Y = new ConditionBelowY();
    public static final Condition<?> ABOVE_Y = new ConditionAboveY();
    public static final Condition<?> ABOVE_HEALTH_PERCENT = new ConditionAboveHealthPercent();
    public static final Condition<?> BELOW_HEALTH_PERCENT = new ConditionBelowHealthPercent();
    public static final Condition<?> IN_WATER = new ConditionInWater();
    public static final Condition<?> IN_WORLD = new ConditionInWorld();
    public static final Condition<?> ABOVE_XP_LEVEL = new ConditionAboveXPLevel();
    public static final Condition<?> BELOW_XP_LEVEL = new ConditionBelowXPLevel();
    public static final Condition<?> ABOVE_HUNGER_PERCENT = new ConditionAboveHungerPercent();
    public static final Condition<?> BELOW_HUNGER_PERCENT = new ConditionBelowHungerPercent();
    public static final Condition<?> IN_BIOME = new ConditionInBiome();
    public static final Condition<?> HAS_PERMISSION = new ConditionHasPermission();

    /**
     * Get condition matching name.s
     *
     * @param name The name to query.
     * @return The matching condition, or null if not found.
     */
    public static Condition<?> getByName(@NotNull final String name) {
        return BY_NAME.get(name);
    }

    /**
     * List of all registered conditions.
     *
     * @return The conditions.
     */
    public static List<Condition<?>> values() {
        return ImmutableList.copyOf(BY_NAME.values());
    }

    /**
     * Add new condition to EcoWeapons.
     *
     * @param condition The condition to add.
     */
    public static void addNewCondition(@NotNull final Condition<?> condition) {
        BY_NAME.remove(condition.getName());
        BY_NAME.put(condition.getName(), condition);
    }
}
