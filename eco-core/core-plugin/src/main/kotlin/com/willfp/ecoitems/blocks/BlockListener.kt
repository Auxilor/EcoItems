package com.willfp.ecoitems.blocks

import com.willfp.eco.core.drops.DropQueue
import com.willfp.eco.core.integrations.antigrief.AntigriefManager
import com.willfp.eco.core.items.Items
import com.willfp.ecoitems.libreforge.ContentEvent
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.items.ecoItem
import com.willfp.ecoitems.plugin
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.SoundCategory
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.util.BoundingBox
import kotlin.random.Random

/**
 * Placing and breaking custom blocks. The item in hand is a normal item -
 * placement intercepts the interact, writes the assigned blockstate, and runs
 * a real BlockPlaceEvent so protection plugins have their say.
 */
object BlockListener : Listener {
    /** True while our own synthetic BlockPlaceEvent is being dispatched. */
    internal var placing = false
        private set

    /**
     * Right-clicking a custom note block would tune it (and string blocks
     * would connect hooks) - deny the vanilla block use. Item use still
     * proceeds, so placing against custom blocks keeps working.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    fun onInteractCustomBlock(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val block = event.clickedBlock ?: return
        if (EcoBlocks.at(block) != null) {
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY)
        }
    }

    /** Punch / right-click effects on placed custom blocks. */
    @EventHandler
    fun onClickEffects(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) {
            return
        }

        val rightClick = when (event.action) {
            Action.RIGHT_CLICK_BLOCK -> true
            Action.LEFT_CLICK_BLOCK -> false
            else -> return
        }

        // Placing something is not a click on the block underneath.
        val holding = event.item?.ecoItem
        if (rightClick && (holding?.block != null || holding?.furniture != null)) {
            return
        }

        val block = event.clickedBlock ?: return
        val placed = EcoBlocks.at(block) ?: return

        placed.block.effects.dispatch(
            placed.block.effects.clickEvent(rightClick, event.player.isSneaking),
            event.player,
            block.location.add(0.5, 0.5, 0.5),
            block
        )
    }

    // Not ignoreCancelled: denying block use (above) marks the event
    // cancelled, but item use - placing - must still run.
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlace(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK || event.hand != EquipmentSlot.HAND) {
            return
        }
        if (event.useItemInHand() == org.bukkit.event.Event.Result.DENY) {
            return
        }

        val item = event.item ?: return
        val block = item.ecoItem?.block ?: return
        val against = event.clickedBlock ?: return
        val player = event.player

        // Let chests/doors/etc open unless sneaking - but custom blocks only
        // look interactable (their backing is), so they don't count.
        if (!player.isSneaking && against.type.isInteractable && EcoBlocks.at(against) == null) {
            return
        }

        val target = if (isReplaceable(against)) against else against.getRelative(event.blockFace)
        if (!isReplaceable(target)) {
            return
        }

        event.setCancelled(true)

        val orientation = block.directional
            ?.orientationFor(event.blockFace, player)
            ?: 0
        val data = EcoBlocks.blockData(block, orientation) ?: return

        // Don't place inside the player.
        if (player.boundingBox.overlaps(BoundingBox.of(target))) {
            return
        }

        // Claim plugins integrate two ways: eco's antigrief wrappers and
        // listening to the (synthetic) place event below.
        if (!AntigriefManager.canPlaceBlock(player, target)) {
            return
        }

        val previousState = target.state
        target.setBlockData(data, false)

        val placeEvent = BlockPlaceEvent(target, previousState, against, item, player, true, event.hand!!)
        placing = true
        try {
            plugin.server.pluginManager.callEvent(placeEvent)
        } finally {
            placing = false
        }

        if (placeEvent.isCancelled || !placeEvent.canBuild()) {
            previousState.update(true, false)
            return
        }

        if (player.gameMode != GameMode.CREATIVE) {
            item.amount -= 1
        }
        player.swingMainHand()

        block.effects.dispatch(ContentEvent.PLACE, player, target.location.add(0.5, 0.5, 0.5), target)

        val sound = block.sounds?.place ?: "minecraft:block.wood.place"
        target.world.playSound(
            target.location.add(0.5, 0.5, 0.5),
            remapSilencedSound(sound),
            SoundCategory.BLOCKS,
            block.sounds?.volume?.toFloat() ?: 1.0f,
            block.sounds?.pitch?.toFloat() ?: 0.8f
        )
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBreak(event: BlockBreakEvent) {
        val placed = EcoBlocks.at(event.block) ?: return

        event.isDropItems = false
        event.expToDrop = 0

        val block = placed.block
        block.effects.dispatch(ContentEvent.BREAK, event.player, event.block.location.add(0.5, 0.5, 0.5), event.block)
        val sound = block.sounds?.breakSound ?: "minecraft:block.wood.break"
        event.block.world.playSound(
            event.block.location.add(0.5, 0.5, 0.5),
            remapSilencedSound(sound),
            SoundCategory.BLOCKS,
            block.sounds?.volume?.toFloat() ?: 1.0f,
            block.sounds?.pitch?.toFloat() ?: 0.8f
        )

        if (event.player.gameMode == GameMode.CREATIVE) {
            return
        }

        val tool = event.player.inventory.itemInMainHand
        dropItems(block, event.block, tool, event.player)
    }

    /**
     * Drops for a broken custom block, honoring tool/tier/silk/fortune.
     * Player breaks push through eco's DropQueue (telekinesis and the rest
     * of the eco series hook it); environmental breaks drop in-world.
     */
    fun dropItems(block: EcoBlock, worldBlock: Block, tool: ItemStack?, player: Player? = null) {
        if (!canHarvest(block, tool)) {
            return
        }

        spawnDrops(
            block.drops,
            EcoItems.getByID(block.id)?.itemStack,
            worldBlock.location.add(0.5, 0.5, 0.5),
            tool,
            player
        ) { plugin.logger.warning("Block ${block.id} drop '$it' is not a valid item") }
    }

    /** Shared drop pipeline for blocks and furniture. */
    fun spawnDrops(
        drops: BlockDrops?,
        self: ItemStack?,
        location: org.bukkit.Location,
        tool: ItemStack?,
        player: Player?,
        onInvalid: (String) -> Unit = {}
    ) {
        val items = mutableListOf<ItemStack>()
        var xp = 0

        val silk = drops != null && drops.silkTouch &&
            tool != null && tool.containsEnchantment(Enchantment.SILK_TOUCH)

        if (drops == null || silk) {
            self?.let { items += it }
        } else {
            val fortune = if (drops.fortune) {
                tool?.getEnchantmentLevel(Enchantment.FORTUNE) ?: 0
            } else {
                0
            }

            for (drop in drops.items) {
                if (Random.nextDouble() > drop.chance) {
                    continue
                }

                val stack = Items.lookup(drop.item).item
                if (stack.type == Material.AIR) {
                    onInvalid(drop.item)
                    continue
                }

                var amount = if (drop.amount.isEmpty()) 1 else drop.amount.random()
                if (fortune > 0) {
                    amount *= Random.nextInt(1, fortune + 2)
                }

                stack.amount = amount.coerceIn(1, stack.maxStackSize)
                items += stack
            }

            if (!drops.xp.isEmpty()) {
                xp = drops.xp.random().coerceAtLeast(0)
            }
        }

        if (player != null) {
            DropQueue(player)
                .addItems(items)
                .addXP(xp)
                // eco's queue centers the location itself; hand it the corner.
                .setLocation(location.block.location)
                .push()
            return
        }

        val world = location.world ?: return
        items.forEach { world.dropItemNaturally(location, it) }
        if (xp > 0) {
            world.spawn(location, org.bukkit.entity.ExperienceOrb::class.java).experience = xp
        }
    }

    private fun canHarvest(block: EcoBlock, tool: ItemStack?): Boolean {
        if (block.correctTools.isEmpty()) {
            return true
        }

        // Environmental destruction (explosions, water) bypasses tool
        // requirements, like vanilla TNT mining.
        val name = tool?.type?.name ?: return true
        if (block.correctTools.none { name.endsWith("_$it") || name == it }) {
            return false
        }

        val minimum = block.minimumTier ?: return true
        val tier = TIERS.entries.firstOrNull { name.startsWith(it.key.uppercase()) }?.value ?: -1
        return tier >= (TIERS[minimum] ?: 0)
    }

    // Gold mines fast but harvests at wood tier, like vanilla.
    private val TIERS = mapOf(
        "wooden" to 0, "golden" to 0, "stone" to 1,
        "iron" to 2, "diamond" to 3, "netherite" to 4
    )

    private fun isReplaceable(block: Block): Boolean =
        block.type.isAir || block.isLiquid || REPLACEABLE.contains(block.type)

    private val REPLACEABLE = setOf(
        Material.SHORT_GRASS, Material.TALL_GRASS, Material.FERN, Material.LARGE_FERN,
        Material.SEAGRASS, Material.TALL_SEAGRASS, Material.SNOW, Material.VINE,
        Material.DEAD_BUSH, Material.LIGHT, Material.FIRE, Material.SOUL_FIRE,
        Material.BUBBLE_COLUMN, Material.HANGING_ROOTS, Material.GLOW_LICHEN
    )

    /**
     * The pack silences the vanilla wood/stone events the backings would
     * play; configs written with those names get routed to the replayable
     * copies the pack defines instead.
     */
    fun remapSilencedSound(sound: String): String {
        if (!BlockSoundState.remapActive) {
            return sound
        }

        val plain = sound.removePrefix("minecraft:")
        return when {
            plain.startsWith("block.wood.") -> "ecoitems:required.wood.${plain.substringAfterLast('.')}"
            plain.startsWith("block.stone.") -> "ecoitems:required.stone.${plain.substringAfterLast('.')}"
            else -> sound
        }
    }
}

/** Picks the orientation index for a directional block being placed. */
fun DirectionalType.orientationFor(face: BlockFace, player: Player): Int {
    val key = when (this) {
        DirectionalType.LOG -> when (face) {
            BlockFace.UP, BlockFace.DOWN -> "y"
            BlockFace.EAST, BlockFace.WEST -> "x"
            else -> "z"
        }

        DirectionalType.FURNACE -> player.horizontalFacing().oppositeFace.name.lowercase()

        DirectionalType.DROPPER -> when {
            player.location.pitch <= -45 -> "up"
            player.location.pitch >= 45 -> "down"
            else -> player.horizontalFacing().oppositeFace.name.lowercase()
        }
    }

    return orientations.indexOf(key).coerceAtLeast(0)
}

private fun Player.horizontalFacing(): BlockFace {
    val yaw = ((location.yaw % 360) + 360) % 360
    return when {
        yaw >= 315 || yaw < 45 -> BlockFace.SOUTH
        yaw < 135 -> BlockFace.WEST
        yaw < 225 -> BlockFace.NORTH
        else -> BlockFace.EAST
    }
}
