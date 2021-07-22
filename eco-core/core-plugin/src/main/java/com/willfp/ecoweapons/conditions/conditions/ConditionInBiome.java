package com.willfp.ecoweapons.conditions.conditions;

import com.willfp.ecoweapons.conditions.Condition;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class ConditionInBiome extends Condition<String> {
    public ConditionInBiome() {
        super("in-biome", String.class);
    }

    @Override
    public boolean isConditionMet(@NotNull final Player player,
                                  @NotNull final String value) {
        List<String> biomeNames = Arrays.asList(value.toLowerCase().split(" "));
        Biome biome = player.getLocation().getWorld().getBiome(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
        return biomeNames.contains(biome.name().toLowerCase());
    }
}
