package com.willfp.ecoweapons.effects.effects;

import com.willfp.eco.core.events.NaturalExpGainEvent;
import com.willfp.ecoweapons.effects.Effect;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;

public class ExperienceMultiplier extends Effect<Double> {
    public ExperienceMultiplier() {
        super("experience-multiplier", Double.class);
    }

    @EventHandler
    public void listener(@NotNull final NaturalExpGainEvent event) {
        Player player = event.getExpChangeEvent().getPlayer();

        if (event.getExpChangeEvent().getAmount() < 0) {
            return;
        }

        this.applyIfEnabled(player, multiplier -> event.getExpChangeEvent().setAmount((int) Math.ceil(event.getExpChangeEvent().getAmount() * multiplier)));
    }
}
