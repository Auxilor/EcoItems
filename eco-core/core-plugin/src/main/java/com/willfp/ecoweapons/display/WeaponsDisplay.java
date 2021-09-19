package com.willfp.ecoweapons.display;

import com.willfp.eco.core.EcoPlugin;
import com.willfp.eco.core.display.DisplayModule;
import com.willfp.eco.core.display.DisplayPriority;
import com.willfp.ecoweapons.weapons.Weapon;
import com.willfp.ecoweapons.weapons.util.WeaponUtils;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class WeaponsDisplay extends DisplayModule {
    /**
     * Create weapons display.
     *
     * @param plugin Instance of EcoWeapons.
     */
    public WeaponsDisplay(@NotNull final EcoPlugin plugin) {
        super(plugin, DisplayPriority.LOWEST);
    }

    @Override
    public void display(@NotNull final ItemStack itemStack,
                           @NotNull final Object... args) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        Weapon weapon = WeaponUtils.getWeaponFromItem(meta);

        if (weapon == null) {
            return;
        }

        ItemMeta weaponMeta = weapon.getItem().getItemMeta();
        assert weaponMeta != null;

        List<String> lore = new ArrayList<>(weaponMeta.getLore());

        if (meta.hasLore()) {
            lore.addAll(meta.getLore());
        }
        meta.setLore(lore);
        meta.setDisplayName(weaponMeta.getDisplayName());
        meta.addItemFlags(weaponMeta.getItemFlags().toArray(new ItemFlag[0]));

        itemStack.setItemMeta(meta);
    }
}
