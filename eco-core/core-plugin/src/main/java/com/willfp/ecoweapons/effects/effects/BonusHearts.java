package com.willfp.ecoweapons.effects.effects;

import com.willfp.ecoweapons.effects.Effect;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BonusHearts extends Effect<Integer> {
    public BonusHearts() {
        super("bonus-hearts", Integer.class);
    }

    @Override
    protected void onEnable(@NotNull final Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        assert maxHealth != null;

        Integer bonus = this.getStrengthForPlayer(player);

        if (bonus == null) {
            return;
        }

        if (player.getHealth() >= maxHealth.getValue()) {
            this.getPlugin().getScheduler().runLater(() -> {
                player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            }, 1);
        }

        AttributeModifier modifier = new AttributeModifier(this.getUuid(), "bonus-hearts", bonus, AttributeModifier.Operation.ADD_NUMBER);
        if (maxHealth.getModifiers().stream().noneMatch(attributeModifier -> attributeModifier.getUniqueId().equals(modifier.getUniqueId()))) {
            maxHealth.addModifier(modifier);
        }
    }

    @Override
    protected void onDisable(@NotNull final Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        assert maxHealth != null;

        maxHealth.removeModifier(new AttributeModifier(this.getUuid(), "bonus-hearts", 0, AttributeModifier.Operation.ADD_NUMBER));
    }
}
