package com.willfp.ecoweapons.weapons;

import com.willfp.eco.core.EcoPlugin;
import com.willfp.eco.core.config.interfaces.JSONConfig;
import com.willfp.eco.core.display.Display;
import com.willfp.eco.core.items.CustomItem;
import com.willfp.eco.core.items.builder.ItemBuilder;
import com.willfp.eco.core.items.builder.ItemStackBuilder;
import com.willfp.eco.core.recipe.Recipes;
import com.willfp.ecoweapons.conditions.Condition;
import com.willfp.ecoweapons.conditions.Conditions;
import com.willfp.ecoweapons.effects.Effect;
import com.willfp.ecoweapons.effects.Effects;
import com.willfp.ecoweapons.weapons.util.WeaponUtils;
import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class Weapon {
    /**
     * Instance of EcoWeapons.
     */
    @Getter(AccessLevel.PRIVATE)
    private final EcoPlugin plugin;

    /**
     * The config of the weapon.
     */
    @Getter(AccessLevel.PRIVATE)
    private final JSONConfig config;

    /**
     * The name of the weapon.
     */
    @Getter
    private final String name;

    /**
     * Conditions and their values.
     */
    @Getter
    private final Map<Condition<?>, Object> conditions = new HashMap<>();

    /**
     * Effects and their strengths.
     */
    @Getter
    private final Map<Effect<?>, Object> effects = new HashMap<>();

    /**
     * Weapon item.
     */
    @Getter
    private final ItemStack item;

    /**
     * Create a new weapon.
     *
     * @param config The weapons's config.
     * @param plugin Instance of EcoWeapons.
     */
    public Weapon(@NotNull final JSONConfig config,
                  @NotNull final EcoPlugin plugin) {
        this.config = config;
        this.plugin = plugin;
        this.name = config.getString("name");

        for (JSONConfig cfg : this.getConfig().getSubsections("conditions")) {
            Condition<?> effect = Conditions.getByName(cfg.getString("id"));
            Object value = cfg.get("args");
            conditions.put(effect, value);
        }

        for (JSONConfig cfg : this.getConfig().getSubsections("effects")) {
            Effect<?> effect = Effects.getByName(cfg.getString("id"));
            Object value = cfg.get("args");
            effects.put(effect, value);
        }

        item = construct();
    }

    private ItemStack construct() {
        Material material = Material.getMaterial(config.getString("material").toUpperCase());

        assert material != null;

        ItemBuilder builder = new ItemStackBuilder(material);

        builder.setDisplayName(config.getString("displayName"))
                .addItemFlag(
                        config.getStrings("flags").stream()
                                .map(s -> ItemFlag.valueOf(s.toUpperCase()))
                                .toArray(ItemFlag[]::new)
                )
                .setUnbreakable(config.getBool("unbreakable"))
                .addLoreLines(config.getStrings("lore").stream().map(s -> Display.PREFIX + s).collect(Collectors.toList()))
                .setCustomModelData(() -> {
                    int data = config.getInt("customModelData");
                    return data != -1 ? data : null;
                })
                .writeMetaKey(
                        this.getPlugin().getNamespacedKeyFactory().create("weapon"),
                        PersistentDataType.STRING,
                        name
                );

        Map<Enchantment, Integer> enchants = new HashMap<>();

        for (JSONConfig enchantSection : config.getSubsections("enchants")) {
            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantSection.getString("id")));
            int level = enchantSection.getInt("level");
            enchants.put(enchantment, level);
        }

        enchants.forEach(builder::addEnchantment);

        ItemStack itemStack = builder.build();

        new CustomItem(this.getPlugin().getNamespacedKeyFactory().create(name.toLowerCase()), test -> Objects.equals(this, WeaponUtils.getWeaponFromItem(test)), itemStack).register();

        if (config.getBool("craftable")) {
            Recipes.createAndRegisterRecipe(
                    this.getPlugin(),
                    this.getName(),
                    item,
                    config.getStrings("recipe")
            );
        }

        return itemStack;
    }

    /**
     * Get condition value of effect.
     *
     * @param condition The condition to query.
     * @param <T>       The type of the condition value.
     * @return The value.
     */
    @Nullable
    public <T> T getConditionValue(@NotNull final Condition<T> condition) {
        return (T) conditions.get(condition);
    }

    /**
     * Get effect strength of effect.
     *
     * @param effect The effect to query.
     * @param <T>    The type of the effect value.
     * @return The strength.
     */
    @Nullable
    public <T> T getEffectStrength(@NotNull final Effect<T> effect) {
        return (T) effects.get(effect);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Weapon set)) {
            return false;
        }

        return this.name.equals(set.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name);
    }

    @Override
    public String toString() {
        return "Weapon{"
                + this.name
                + "}";
    }
}
