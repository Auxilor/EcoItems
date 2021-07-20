package com.willfp.ecoweapons.conditions.conditions;

import com.willfp.ecoweapons.conditions.Condition;
import com.willfp.ecoweapons.sets.Weapon;
import com.willfp.ecoweapons.sets.util.WeaponUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ConditionHasPermission extends Condition<String> implements Runnable {
    public ConditionHasPermission() {
        super("has-permission", String.class);
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Weapon weapon = WeaponUtils.getWeaponFromItem(player.getInventory().getItemInMainHand());

            if (weapon == null) {
                return;
            }

            String value = weapon.getConditionValue(this);

            if (value == null) {
                return;
            }

            evaluateEffects(player, value, weapon);
        }
    }

    @Override
    public boolean isConditionMet(@NotNull final Player player,
                                  @NotNull final String value) {
        return player.hasPermission(value);
    }
}
