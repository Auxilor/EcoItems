package com.willfp.ecoweapons.weapons.util;

import com.willfp.ecoweapons.EcoWeaponsPlugin;
import com.willfp.ecoweapons.conditions.Condition;
import com.willfp.ecoweapons.weapons.Weapon;
import com.willfp.ecoweapons.weapons.Weapons;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@UtilityClass
public class WeaponUtils {
    /**
     * Instance of EcoWeapons.
     */
    private static final EcoWeaponsPlugin PLUGIN = EcoWeaponsPlugin.getInstance();

    /**
     * Get weapon from an item.
     *
     * @param itemStack The itemStack to check.
     * @return The weapon, or null if no weapon is found.
     */
    @Nullable
    public Weapon getWeaponFromItem(@Nullable final ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }

        ItemMeta meta = itemStack.getItemMeta();

        if (meta == null) {
            return null;
        }

        return getWeaponFromItem(meta);
    }

    /**
     * Get weapon on an item.
     *
     * @param meta The itemStack to check.
     * @return The weapon, or null if no weapon is found.
     */
    @Nullable
    public Weapon getWeaponFromItem(@NotNull final ItemMeta meta) {
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String weaponName = container.get(PLUGIN.getNamespacedKeyFactory().create("weapon"), PersistentDataType.STRING);

        if (weaponName == null) {
            return null;
        }

        return Weapons.getByName(weaponName);
    }

    /**
     * Get if all conditions are met for a player.
     *
     * @param player The player.
     * @param weapon The weapon.
     * @return If conditions are met.
     */
    public boolean areConditionsMet(@NotNull final Player player,
                                    @NotNull final Weapon weapon) {
        for (Map.Entry<Condition<?>, Object> entry : weapon.getConditions().entrySet()) {
            if (!entry.getKey().isMet(player, entry.getValue())) {
                return false;
            }
        }

        return true;
    }
}
