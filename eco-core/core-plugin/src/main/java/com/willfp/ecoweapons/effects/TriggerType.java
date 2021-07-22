package com.willfp.ecoweapons.effects;

import org.jetbrains.annotations.NotNull;

public enum TriggerType {
    /**
     * Secondary click.
     */
    ALT_CLICK,

    /**
     * Melee attack.
     */
    MELEE_ATTACK,

    /**
     * Projectile hit.
     */
    PROJECTILE_HIT,

    /**
     * Shift Secondary Click.
     */
    SHIFT_ALT_CLICK,

    /**
     * Projectile damage entity.
     */
    PROJECTILE_HIT_ENTITY;

    /**
     * Get trigger type by name.
     *
     * @param name The name.
     * @return The found type, or null if not found.
     */
    public static TriggerType getByName(@NotNull final String name) {
        return switch (name.toLowerCase()) {
            case "alt_click" -> ALT_CLICK;
            case "melee_attack" -> MELEE_ATTACK;
            case "projectile_hit" -> PROJECTILE_HIT;
            case "shift_alt_click" -> SHIFT_ALT_CLICK;
            case "projectile_hit_entity" -> PROJECTILE_HIT_ENTITY;
            default -> null;
        };
    }
}
