package com.willfp.ecoitems.furniture

import org.bukkit.entity.ItemDisplay
import org.bukkit.util.BoundingBox
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Connectable furniture: pieces re-model themselves as row ends or middles
 * by looking for matching neighbors (same furniture id, same facing) one
 * block to each side, and neighbors get re-checked when a piece is placed
 * or removed.
 */
object FurnitureConnections {
    /** Re-models a piece from its neighbors, optionally rippling to them. */
    fun update(placed: PlacedFurniture, includeNeighbors: Boolean = true) {
        val connectable = placed.furniture?.connectable ?: return

        val left = neighbor(placed, -1)
        val right = neighbor(placed, 1)

        val state = when {
            left != null && right != null -> connectable.middle
            left == null && right != null -> connectable.left
            left != null -> connectable.right
            else -> null
        }
        placed.applyModel(state?.modelKey)

        if (includeNeighbors) {
            left?.let { update(it, includeNeighbors = false) }
            right?.let { update(it, includeNeighbors = false) }
        }
    }

    /** Re-models the neighbors of a piece that is being removed. */
    fun updateNeighborsAfterRemoval(placed: PlacedFurniture) {
        val neighbors = listOfNotNull(neighbor(placed, -1), neighbor(placed, 1))

        // Next tick: the removed piece must be gone before neighbors look.
        com.willfp.ecoitems.plugin.scheduler.run {
            neighbors.forEach { if (it.base.isValid) update(it, includeNeighbors = false) }
        }
    }

    /** The placement yaw (the display spawns rotated 180 from it). */
    private fun placementYaw(placed: PlacedFurniture): Float =
        placed.base.location.yaw - 180f

    private fun neighbor(placed: PlacedFurniture, side: Int): PlacedFurniture? {
        val yaw = placementYaw(placed)
        val (x, z) = PlacedFurniture.rotate(side.toDouble(), 0.0, yaw)
        val block = placed.base.location.block.getRelative(x.roundToInt(), 0, z.roundToInt())

        for (entity in block.world.getNearbyEntities(BoundingBox.of(block)) { it is ItemDisplay }) {
            val other = PlacedFurniture.fromEntity(entity) ?: continue
            if (other.base.uniqueId == placed.base.uniqueId || other.id != placed.id) {
                continue
            }
            if (other.base.location.block != block) {
                continue
            }

            // Same facing (mod 360) - rows don't connect around corners.
            val delta = abs((other.base.location.yaw - placed.base.location.yaw + 540) % 360 - 180)
            if (delta < 1f) {
                return other
            }
        }

        return null
    }
}
