package com.willfp.ecoweapons.effects.effects;

import com.willfp.eco.util.NumberUtils;
import com.willfp.ecoweapons.effects.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class WarpChance extends Effect<Double> {
    public WarpChance() {
        super("warp-chance", Double.class);
    }

    @EventHandler
    public void listener(@NotNull final EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        Player player = (Player) event.getDamager();
        LivingEntity victim = (LivingEntity) event.getEntity();

        Double chance = this.getStrengthForPlayer(player);

        if (chance == null) {
            return;
        }

        if (NumberUtils.randFloat(0, 100) > chance) {
            return;
        }

        Vector between = victim.getLocation().subtract(player.getLocation()).toVector();
        Location behind = victim.getLocation().add(between);

        behind.setYaw(player.getLocation().getYaw() + 180);

        Block head = behind.add(0, 1.4, 0).getBlock();

        if (!head.getType().isAir()) {
            return;
        }

        player.getLocation().setYaw(player.getLocation().getYaw() + 180);
        player.teleport(behind);
    }
}
