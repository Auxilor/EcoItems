package com.willfp.ecoweapons.effects.effects;

import com.willfp.eco.util.NumberUtils;
import com.willfp.ecoweapons.effects.Effect;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.jetbrains.annotations.NotNull;

public class HungerLossMultiplier extends Effect<Double> {
    public HungerLossMultiplier() {
        super("hunger-loss-multiplier", Double.class);
    }

    @EventHandler
    public void listener(@NotNull final FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        Double multiplier = this.getStrengthForPlayer(player);

        if (multiplier == null) {
            return;
        }

        if (event.getFoodLevel() > player.getFoodLevel()) {
            return;
        }

        if (multiplier < 1) {
            if (NumberUtils.randFloat(0, 1) > multiplier) {
                event.setCancelled(true);
            }
        } else {
            int difference = player.getFoodLevel() - event.getFoodLevel();
            difference = (int) Math.ceil(difference * multiplier);
            event.setFoodLevel(player.getFoodLevel() - difference);
        }
    }
}
