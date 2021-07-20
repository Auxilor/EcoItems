package com.willfp.ecoweapons.effects.effects;

import com.willfp.ecoweapons.effects.Effect;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AttackSpeedMultiplier extends Effect<Double> {
    public AttackSpeedMultiplier() {
        super("attack-speed-multiplier", Double.class);
    }

    @Override
    protected void onEnable(@NotNull final Player player) {
        AttributeInstance attackSpeed = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        assert attackSpeed != null;

        Double multiplier = this.getStrengthForPlayer(player);

        if (multiplier == null) {
            return;
        }

        AttributeModifier modifier = new AttributeModifier(this.getUuid(), "attack-speed-multiplier", 1 - multiplier, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
        if (attackSpeed.getModifiers().stream().noneMatch(attributeModifier -> attributeModifier.getUniqueId().equals(modifier.getUniqueId()))) {
            attackSpeed.addModifier(modifier);
        }
    }

    @Override
    protected void onDisable(@NotNull final Player player) {
        AttributeInstance attackSpeed = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        assert attackSpeed != null;

        attackSpeed.removeModifier(new AttributeModifier(this.getUuid(), "attack-speed-multiplier", 0, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
    }
}
