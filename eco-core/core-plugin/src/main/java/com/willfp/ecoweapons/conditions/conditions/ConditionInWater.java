package com.willfp.ecoweapons.conditions.conditions;

import com.willfp.ecoweapons.conditions.Condition;
import com.willfp.ecoweapons.sets.Weapon;
import com.willfp.ecoweapons.sets.util.WeaponUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

public class ConditionInWater extends Condition<Boolean> {
    public ConditionInWater() {
        super("in-water", Boolean.class);
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void listener(@NotNull final PlayerMoveEvent event) {
        Player player = event.getPlayer();

        Weapon weapon = WeaponUtils.getWeaponFromItem(player.getInventory().getItemInMainHand());

        if (weapon == null) {
            return;
        }

        Boolean value = weapon.getConditionValue(this);

        if (value == null) {
            return;
        }

        evaluateEffects(player, value, weapon);
    }

    @Override
    public boolean isConditionMet(@NotNull final Player player,
                                  @NotNull final Boolean value) {
        return (player.getLocation().getBlock().getType() == Material.WATER) == value;
    }
}
