package com.willfp.ecoweapons.effects.util;

import com.willfp.eco.core.config.interfaces.JSONConfig;
import com.willfp.eco.core.integrations.antigrief.AntigriefManager;
import com.willfp.eco.util.ArrowUtils;
import com.willfp.eco.util.NumberUtils;
import com.willfp.ecoweapons.effects.Effect;
import com.willfp.ecoweapons.effects.TriggerType;
import com.willfp.ecoweapons.weapons.Weapon;
import com.willfp.ecoweapons.weapons.util.WeaponUtils;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EffectListener implements Listener {
    /**
     * Items that must be left-clicked to activate spells for.
     */
    private static final List<Material> LEFT_CLICK_ITEMS = Arrays.asList(
            Material.FISHING_ROD,
            Material.BOW,
            Material.CROSSBOW,
            Material.TRIDENT
    );

    /**
     * Items that don't cause spells to activate when right clicked.
     */
    private static final List<Material> BLACKLIST_CLICKED_BLOCKS = new ArrayList<>(Arrays.asList(
            Material.CRAFTING_TABLE,
            Material.GRINDSTONE,
            Material.ENCHANTING_TABLE,
            Material.FURNACE,
            Material.SMITHING_TABLE,
            Material.LEVER,
            Material.REPEATER,
            Material.COMPARATOR,
            Material.RESPAWN_ANCHOR,
            Material.NOTE_BLOCK,
            Material.ITEM_FRAME,
            Material.CHEST,
            Material.BARREL,
            Material.BEACON,
            Material.LECTERN,
            Material.FLETCHING_TABLE,
            Material.SMITHING_TABLE,
            Material.STONECUTTER,
            Material.SMOKER,
            Material.BLAST_FURNACE,
            Material.BREWING_STAND,
            Material.DISPENSER,
            Material.DROPPER
    ));

    static {
        BLACKLIST_CLICKED_BLOCKS.addAll(Tag.BUTTONS.getValues());
        BLACKLIST_CLICKED_BLOCKS.addAll(Tag.BEDS.getValues());
        BLACKLIST_CLICKED_BLOCKS.addAll(Tag.DOORS.getValues());
        BLACKLIST_CLICKED_BLOCKS.addAll(Tag.FENCE_GATES.getValues());
        BLACKLIST_CLICKED_BLOCKS.addAll(Tag.TRAPDOORS.getValues());
        BLACKLIST_CLICKED_BLOCKS.addAll(Tag.ANVIL.getValues());
        BLACKLIST_CLICKED_BLOCKS.addAll(Tag.SHULKER_BOXES.getValues());
    }

    /**
     * Handle {@link TriggerType#MELEE_ATTACK}.
     *
     * @param event The event.
     */
    @EventHandler(
            ignoreCancelled = true
    )
    public void meleeAttackListener(@NotNull final EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }

        Weapon weapon = WeaponUtils.getWeaponFromItem(player.getInventory().getItemInMainHand());
        if (weapon == null) {
            return;
        }

        if (!WeaponUtils.areConditionsMet(player, weapon)) {
            return;
        }


        for (Effect effect : weapon.getEffects(TriggerType.MELEE_ATTACK)) {
            if (AntigriefManager.canInjure(player, victim)) {
                if (effect.isCooldownOver(player, weapon, TriggerType.MELEE_ATTACK)) {
                    JSONConfig args = weapon.getEffectArgs(effect, TriggerType.MELEE_ATTACK);
                    Double chance = args.getDoubleOrNull("chance");

                    if (chance != null) {
                        if (NumberUtils.randInt(0, 100) > chance) {
                            continue;
                        }
                    }

                    effect.handleMeleeAttack(player, victim, event, args);
                }
            }
        }
    }

    /**
     * Handle {@link TriggerType#PROJECTILE_HIT} and {@link TriggerType#PROJECTILE_HIT_ENTITY}.
     *
     * @param event The event.
     */
    @EventHandler(
            ignoreCancelled = true
    )
    public void projectileHitListener(@NotNull final ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident || event.getEntity() instanceof Arrow)) {
            return;
        }

        ItemStack item;

        if (event.getEntity() instanceof Trident trident) {
            item = trident.getItem();
        } else {
            item = ArrowUtils.getBow((Arrow) event.getEntity());
        }

        if (item == null) {
            return;
        }

        if (!(event.getEntity().getShooter() instanceof Player player)) {
            return;
        }

        Weapon weapon = WeaponUtils.getWeaponFromItem(item);
        if (weapon == null) {
            return;
        }

        if (!WeaponUtils.areConditionsMet(player, weapon)) {
            return;
        }

        if (event.getHitEntity() == null) {
            for (Effect effect : weapon.getEffects(TriggerType.PROJECTILE_HIT)) {
                if (effect.isCooldownOver(player, weapon, TriggerType.PROJECTILE_HIT)) {
                    JSONConfig args = weapon.getEffectArgs(effect, TriggerType.PROJECTILE_HIT);
                    Double chance = args.getDoubleOrNull("chance");

                    if (chance != null) {
                        if (NumberUtils.randInt(0, 100) > chance) {
                            continue;
                        }
                    }
                    effect.handleProjectileHit(player, event.getEntity(), event, args);
                }
            }
        } else {
            if (event.getHitEntity() instanceof LivingEntity victim) {
                for (Effect effect : weapon.getEffects(TriggerType.PROJECTILE_HIT_ENTITY)) {
                    if (AntigriefManager.canInjure(player, victim)) {
                        if (effect.isCooldownOver(player, weapon, TriggerType.PROJECTILE_HIT_ENTITY)) {
                            JSONConfig args = weapon.getEffectArgs(effect, TriggerType.PROJECTILE_HIT_ENTITY);
                            Double chance = args.getDoubleOrNull("chance");

                            if (chance != null) {
                                if (NumberUtils.randInt(0, 100) > chance) {
                                    continue;
                                }
                            }
                            effect.handleProjectileHitEntity(player, victim, event.getEntity(), event, args);
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle {@link TriggerType#ALT_CLICK} and {@link TriggerType#SHIFT_ALT_CLICK}.
     *
     * @param event The event.
     */
    @EventHandler
    public void altClickListener(@NotNull final PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = player.getInventory().getItemInMainHand();

        Weapon weapon = WeaponUtils.getWeaponFromItem(itemStack);
        if (weapon == null) {
            return;
        }

        if (LEFT_CLICK_ITEMS.contains(itemStack.getType())) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                return;
            }
        } else {
            if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
                return;
            }
        }

        if (event.getClickedBlock() != null) {
            if (BLACKLIST_CLICKED_BLOCKS.contains(event.getClickedBlock().getType())) {
                return;
            }
        }

        if (!WeaponUtils.areConditionsMet(player, weapon)) {
            return;
        }

        RayTraceResult result = player.rayTraceBlocks(50, FluidCollisionMode.NEVER);
        if (result == null) {
            return;
        }

        World world = player.getLocation().getWorld();
        assert world != null;

        RayTraceResult entityResult = world.rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                50,
                3,
                entity -> entity instanceof LivingEntity
        );

        if (player.isSneaking()) {
            for (Effect effect : weapon.getEffects(TriggerType.SHIFT_ALT_CLICK)) {
                if (effect.isCooldownOver(player, weapon, TriggerType.SHIFT_ALT_CLICK)) {
                    JSONConfig args = weapon.getEffectArgs(effect, TriggerType.SHIFT_ALT_CLICK);
                    Double chance = args.getDoubleOrNull("chance");

                    if (chance != null) {
                        if (NumberUtils.randInt(0, 100) > chance) {
                            continue;
                        }
                    }
                    effect.handleAltClick(player, result, entityResult, event, args);
                }
            }
        } else {
            for (Effect effect : weapon.getEffects(TriggerType.ALT_CLICK)) {
                if (effect.isCooldownOver(player, weapon, TriggerType.ALT_CLICK)) {
                    JSONConfig args = weapon.getEffectArgs(effect, TriggerType.ALT_CLICK);
                    Double chance = args.getDoubleOrNull("chance");

                    if (chance != null) {
                        if (NumberUtils.randInt(0, 100) > chance) {
                            continue;
                        }
                    }
                    effect.handleAltClick(player, result, entityResult, event, args);
                }
            }
        }
    }
}
