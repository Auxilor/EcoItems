package com.willfp.ecoitems.nms

import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.Vector

/**
 * Runs the vanilla use of a held item against a block face, skipping the
 * clicked block's own use. Placements fire the usual BlockPlaceEvent.
 */
interface ItemUseProxy {
    /** True if the item use did something. */
    fun useItemOn(player: Player, hand: EquipmentSlot, block: Block, face: BlockFace, clickedPosition: Vector?): Boolean
}
