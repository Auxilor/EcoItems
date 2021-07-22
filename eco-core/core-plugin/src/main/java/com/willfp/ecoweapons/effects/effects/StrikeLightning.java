package com.willfp.ecoweapons.effects.effects;

import com.willfp.eco.util.LightningUtils;
import com.willfp.ecoweapons.effects.Effect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@SuppressWarnings("unchecked")
public class StrikeLightning extends Effect {
    public StrikeLightning() {
        super("speed-multiplier");
    }

    @Override
    public void handleMeleeAttack(@NotNull final Player player,
                                  @NotNull final LivingEntity victim,
                                  @NotNull final EntityDamageByEntityEvent event,
                                  @NotNull final Object args) {
        Map<String, Integer> argMap = (Map<String, Integer>) args;

        for (int i = 0; i < argMap.get("amount"); i++) {
            LightningUtils.strike(victim, argMap.get("damage"));
        }
    }
}
