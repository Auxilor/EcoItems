package com.willfp.ecoitems.crops

import com.willfp.eco.core.integrations.antigrief.AntigriefManager
import com.willfp.ecoitems.blocks.BlockListener
import com.willfp.ecoitems.blocks.EcoBlocks
import com.willfp.ecoitems.items.ecoItem
import com.willfp.ecoitems.libreforge.ContentEvent
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.SoundCategory
import org.bukkit.block.BlockFace
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

/**
 * Planting, bonemealing, and cleanup for custom crops. Growth itself runs in
 * [CropTracker]; breaking goes through the normal custom-block break path
 * (which routes crop drops by stage).
 */
object CropListener : Listener {
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlant(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK || event.hand != EquipmentSlot.HAND) {
            return
        }
        if (event.useItemInHand() == Event.Result.DENY) {
            return
        }

        val item = event.item ?: return
        val crop = item.ecoItem?.crop ?: return
        val against = event.clickedBlock ?: return
        val player = event.player

        if (!EcoBlocks.enabledIn(against.world)) {
            return
        }

        // Seeds never place their base item.
        event.isCancelled = true

        if (event.blockFace != BlockFace.UP) {
            return
        }

        val supported = if (crop.requiresFarmland) {
            against.type == Material.FARMLAND
        } else {
            against.type.isSolid
        }
        if (!supported) {
            return
        }

        val target = against.getRelative(BlockFace.UP)
        if (!BlockListener.isReplaceable(target)) {
            return
        }

        if (!AntigriefManager.canPlaceBlock(player, target)) {
            return
        }
        if (!BlockListener.passesPlacementCooldown(player)) {
            return
        }

        val data = EcoBlocks.blockData(crop.block, 0) ?: return

        val previousState = target.state
        target.setBlockData(data, false)

        if (!BlockListener.callPlaceEvent(
                BlockPlaceEvent(target, previousState, against, item, player, true, event.hand!!)
            )
        ) {
            previousState.update(true, false)
            return
        }

        if (player.gameMode != GameMode.CREATIVE) {
            item.amount -= 1
        }
        player.swingMainHand()

        crop.block.effects.dispatch(ContentEvent.PLACE, player, target.location.add(0.5, 0.5, 0.5), target)
        target.world.playSound(
            target.location.add(0.5, 0.5, 0.5),
            crop.block.sounds?.place ?: "minecraft:item.crop.plant",
            SoundCategory.BLOCKS,
            1.0f,
            1.0f
        )

        CropTracker.add(target)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onBonemeal(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK || event.hand != EquipmentSlot.HAND) {
            return
        }

        val item = event.item ?: return
        if (item.type != Material.BONE_MEAL) {
            return
        }

        val block = event.clickedBlock ?: return
        val placed = EcoBlocks.at(block) ?: return
        val crop = placed.block.crop ?: return

        event.isCancelled = true

        if (!crop.bonemeal || placed.orientation >= crop.stages.lastIndex) {
            return
        }

        EcoBlocks.blockData(placed.block, placed.orientation + 1)?.let { block.setBlockData(it, false) }
        CropTracker.resetTimer(block)

        if (event.player.gameMode != GameMode.CREATIVE) {
            item.amount -= 1
        }
        event.player.swingMainHand()
        block.world.spawnParticle(Particle.HAPPY_VILLAGER, block.location.add(0.5, 0.5, 0.5), 12, 0.3, 0.3, 0.3)
        block.world.playSound(
            block.location.add(0.5, 0.5, 0.5),
            "minecraft:item.bone_meal.use",
            SoundCategory.BLOCKS,
            1.0f,
            1.0f
        )
    }

    /** Player breaks are handled by the block path; just forget the crop. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val placed = EcoBlocks.at(event.block) ?: return
        if (placed.block.crop != null) {
            CropTracker.remove(event.block)
        }
    }
}
