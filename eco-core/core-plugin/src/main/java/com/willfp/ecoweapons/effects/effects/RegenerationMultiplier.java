package com.willfp.ecoweapons.effects.effects;

import com.willfp.ecoweapons.effects.Effect;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.jetbrains.annotations.NotNull;

public class RegenerationMultiplier extends Effect<Double> {
    public RegenerationMultiplier() {
        super("regeneration-multiplier", Double.class);
    }

    @EventHandler
    public void listener(@NotNull final EntityRegainHealthEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();

        this.applyIfEnabled(player, multiplier -> event.setAmount(event.getAmount() * multiplier));
    }
}
