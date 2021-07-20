package com.willfp.ecoweapons.effects;

import com.willfp.ecoweapons.EcoWeaponsPlugin;
import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class Effect<T> implements Listener {
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
     * UUID of the effect, used in attribute modifiers.
     */
    @Getter
    private final UUID uuid;

    /**
     * If the effect is enabled.
     */
    @Getter
    private boolean enabled;

    /**
     * The class of the config getter type.
     */
    @Getter
    private final Class<T> typeClass;

    /**
     * Players that the effect is currently enabled for.
     */
    private final Map<UUID, T> enabledPlayers = new HashMap<>();

    /**
     * Create a new effect.
     *
     * @param name      The effect name.
     * @param typeClass The class of the config type.
     */
    protected Effect(@NotNull final String name,
                     @NotNull final Class<T> typeClass) {
        this.name = name;
        this.typeClass = typeClass;
        this.uuid = UUID.nameUUIDFromBytes(name.getBytes());

        update();
        Effects.addNewEffect(this);
    }

    /**
     * Get the effect strength for a player.
     *
     * @param player The player.
     * @return The strength.
     */
    @Nullable
    public final T getStrengthForPlayer(@NotNull final Player player) {
        return enabledPlayers.get(player.getUniqueId());
    }

    /**
     * Apply effect if enabled for a player.
     *
     * @param player   The player.
     * @param consumer The effect function.
     */
    public void applyIfEnabled(@NotNull final Player player,
                               @NotNull final Consumer<T> consumer) {
        T strength = getStrengthForPlayer(player);
        if (strength != null) {
            consumer.accept(strength);
        }
    }

    /**
     * Enable the effect for a player.
     *
     * @param player The player.
     * @param value  The strength.
     */
    public final void enable(@NotNull final Player player,
                             @NotNull final Object value) {
        if (!this.isEnabled()) {
            return;
        }

        if (enabledPlayers.containsKey(player.getUniqueId())) {
            return;
        }

        enabledPlayers.put(player.getUniqueId(), typeClass.cast(value));

        this.onEnable(player);
    }

    /**
     * Disable the effect for a player.
     *
     * @param player The player.
     */
    public final void disable(@NotNull final Player player) {
        if (!this.isEnabled()) {
            return;
        }

        enabledPlayers.remove(player.getUniqueId());

        this.onDisable(player);
    }

    protected void onEnable(@NotNull final Player player) {
        // Empty by default
    }

    protected void onDisable(@NotNull final Player player) {
        // Empty by default
    }

    /**
     * Update if the effect is enabled.
     */
    public void update() {
        enabled = this.getPlugin().getConfigYml().getBool("effects." + name + ".enabled");
    }
}
