package com.willfp.ecoweapons.effects.effects;

import com.willfp.ecoweapons.effects.Effect;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpeedMultiplier extends Effect<Double> {
    public SpeedMultiplier() {
        super("speed-multiplier", Double.class);
    }

    @Override
    protected void onEnable(@NotNull final Player player) {
        AttributeInstance movementSpeed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        assert movementSpeed != null;

        Double strength = this.getStrengthForPlayer(player);

        if (strength == null) {
            return;
        }

        AttributeModifier modifier = new AttributeModifier(this.getUuid(), "speed-multiplier", strength - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        if (movementSpeed.getModifiers().stream().noneMatch(attributeModifier -> attributeModifier.getUniqueId().equals(modifier.getUniqueId()))) {
            movementSpeed.addModifier(modifier);
        }
    }

    @Override
    protected void onDisable(@NotNull final Player player) {
        AttributeInstance movementSpeed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        assert movementSpeed != null;

        movementSpeed.removeModifier(new AttributeModifier(this.getUuid(), "speed-multiplier", 0, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
    }
}
