package com.willfp.ecoweapons.weapons;

import com.willfp.eco.core.EcoPlugin;
import com.willfp.eco.core.config.interfaces.JSONConfig;
import com.willfp.eco.core.display.Display;
import com.willfp.eco.core.items.CustomItem;
import com.willfp.eco.core.items.builder.ItemBuilder;
import com.willfp.eco.core.items.builder.ItemStackBuilder;
import com.willfp.eco.core.recipe.Recipes;
import com.willfp.eco.util.NumberUtils;
import com.willfp.ecoweapons.conditions.Condition;
import com.willfp.ecoweapons.conditions.Conditions;
import com.willfp.ecoweapons.effects.Effect;
import com.willfp.ecoweapons.effects.Effects;
import com.willfp.ecoweapons.effects.TriggerType;
import com.willfp.ecoweapons.weapons.util.WeaponUtils;
import lombok.AccessLevel;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
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
    private final Map<TriggerType, Map<Effect, JSONConfig>> effects = new HashMap<>();

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
            Effect effect = Effects.getByName(cfg.getString("id"));
            JSONConfig value = (JSONConfig) cfg.getSubsection("args");
            TriggerType triggerType = TriggerType.getByName(cfg.getString("trigger"));
            effects.computeIfAbsent(triggerType, k -> new HashMap<>());
            Map<Effect, JSONConfig> triggerEffects = effects.get(triggerType);
            triggerEffects.put(effect, value);
            effects.put(triggerType, triggerEffects);
        }

        item = construct((JSONConfig) this.getConfig().getSubsection("item"));
    }

    private ItemStack construct(@NotNull final JSONConfig itemConfig) {
        Material material = Material.getMaterial(itemConfig.getString("material").toUpperCase());

        assert material != null;

        ItemBuilder builder = new ItemStackBuilder(material);

        builder.setDisplayName(itemConfig.getString("displayName"))
                .addItemFlag(
                        itemConfig.getStrings("flags").stream()
                                .map(s -> ItemFlag.valueOf(s.toUpperCase()))
                                .toArray(ItemFlag[]::new)
                )
                .setUnbreakable(itemConfig.getBool("unbreakable"))
                .addLoreLines(itemConfig.getStrings("lore").stream().map(s -> Display.PREFIX + s).collect(Collectors.toList()))
                .setCustomModelData(() -> {
                    int data = itemConfig.getInt("customModelData");
                    return data != -1 ? data : null;
                })
                .writeMetaKey(
                        this.getPlugin().getNamespacedKeyFactory().create("weapon"),
                        PersistentDataType.STRING,
                        name
                );

        Map<Enchantment, Integer> enchants = new HashMap<>();

        for (JSONConfig enchantSection : itemConfig.getSubsections("enchants")) {
            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantSection.getString("id")));
            int level = enchantSection.getInt("level");
            enchants.put(enchantment, level);
        }

        enchants.forEach(builder::addEnchantment);

        ItemStack itemStack = builder.build();

        ItemMeta meta = itemStack.getItemMeta();
        assert meta != null;

        for (JSONConfig attribute : itemConfig.getSubsections("attributes")) {
            meta.addAttributeModifier(
                    Attribute.valueOf(attribute.getString("id").toUpperCase()),
                    new AttributeModifier(
                            UUID.randomUUID(),
                            String.valueOf(NumberUtils.randInt(0, 10000000)),
                            Double.parseDouble(attribute.getString("amount")),
                            AttributeModifier.Operation.valueOf(attribute.getString("operation").toUpperCase())
                    )
            );
        }

        itemStack.setItemMeta(meta);

        new CustomItem(this.getPlugin().getNamespacedKeyFactory().create(name.toLowerCase()), test -> Objects.equals(this, WeaponUtils.getWeaponFromItem(test)), itemStack).register();

        if (itemConfig.getBool("craftable")) {
            Recipes.createAndRegisterRecipe(
                    this.getPlugin(),
                    this.getName(),
                    item,
                    itemConfig.getStrings("recipe")
            );
        }

        return itemStack;
    }

    /**
     * Get effect strength of effect.
     *
     * @param effect      The effect to query.
     * @param triggerType The trigger type.
     * @return The strength.
     */
    public JSONConfig getEffectArgs(@NotNull final Effect effect,
                                    @NotNull final TriggerType triggerType) {
        return effects.get(triggerType).get(effect);
    }

    /**
     * Get all effects for a trigger type.
     *
     * @param triggerType The type.
     * @return The effects.
     */
    public Set<Effect> getEffects(@NotNull final TriggerType triggerType) {
        return effects.get(triggerType).keySet();
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
