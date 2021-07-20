package com.willfp.ecoweapons.conditions.conditions;

import com.willfp.ecoweapons.conditions.Condition;
import com.willfp.ecoweapons.sets.Weapon;
import com.willfp.ecoweapons.sets.util.WeaponUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.jetbrains.annotations.NotNull;

public class ConditionAboveXPLevel extends Condition<Integer> {
    public ConditionAboveXPLevel() {
        super("above-xp-level", Integer.class);
    }

    @EventHandler(
            priority = EventPriority.MONITOR,
            ignoreCancelled = true
    )
    public void listener(@NotNull final PlayerExpChangeEvent event) {
        Player player = event.getPlayer();

        Weapon weapon = WeaponUtils.getWeaponFromItem(player.getInventory().getItemInMainHand());

        if (weapon == null) {
            return;
        }

        Integer value = weapon.getConditionValue(this);

        if (value == null) {
            return;
        }

        evaluateEffects(player, value, weapon);
    }

    @Override
    public boolean isConditionMet(@NotNull final Player player,
                                  @NotNull final Integer value) {
        return player.getLevel() >= value;
    }
}
