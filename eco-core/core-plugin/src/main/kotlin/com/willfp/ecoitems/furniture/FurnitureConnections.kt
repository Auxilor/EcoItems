package com.willfp.ecoitems.furniture

import org.bukkit.entity.ItemDisplay
import org.bukkit.util.BoundingBox
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Connectable furniture: pieces re-model themselves as row ends, middles,
 * or corners by looking at matching neighbors (same furniture id) one block
 * away, and neighbors get re-checked when a piece is placed or removed.
 *
 * Rows connect along a piece's lateral axis when facings match; corners
 * connect a lateral row to a perpendicular one.
 */
object FurnitureConnections {
    /** Re-models a piece from its neighbors, optionally rippling to them. */
    fun update(placed: PlacedFurniture, includeNeighbors: Boolean = true) {
        val connectable = placed.furniture?.connectable ?: return

        val left = neighbor(placed, -1, 0, perpendicular = false)
        val right = neighbor(placed, 1, 0, perpendicular = false)
        val front = neighbor(placed, 0, -1, perpendicular = true)
        val back = neighbor(placed, 0, 1, perpendicular = true)

        val lateralSide = when {
            left != null && right == null -> -1
            right != null && left == null -> 1
            else -> 0
        }
        val axial = front ?: back

        val state = when {
            left != null && right != null -> connectable.middle

            // One row arriving sideways, one leaving at 90 degrees: a corner.
            lateralSide != 0 && axial != null -> {
                val depth = if (front != null) -1 else 1
                if (lateralSide * depth > 0) connectable.inner else connectable.outer
            }

            right != null -> connectable.left
            left != null -> connectable.right
            else -> null
        }
        placed.applyModel(state?.modelKey)

        if (includeNeighbors) {
            listOfNotNull(left, right, front, back).forEach { update(it, includeNeighbors = false) }
        }
    }

    /** Re-models the neighbors of a piece that is being removed. */
    fun updateNeighborsAfterRemoval(placed: PlacedFurniture) {
        val neighbors = listOfNotNull(
            neighbor(placed, -1, 0, perpendicular = false),
            neighbor(placed, 1, 0, perpendicular = false),
            neighbor(placed, 0, -1, perpendicular = true),
            neighbor(placed, 0, 1, perpendicular = true)
        )

        // Next tick: the removed piece must be gone before neighbors look.
        com.willfp.ecoitems.plugin.scheduler.run {
            neighbors.forEach { if (it.base.isValid) update(it, includeNeighbors = false) }
        }
    }

    /** The placement yaw (the display spawns rotated 180 from it). */
    private fun placementYaw(placed: PlacedFurniture): Float =
        placed.base.location.yaw - 180f

    /**
     * The matching neighbor at a local offset. Lateral neighbors must face
     * the same way; perpendicular ones must face 90 degrees off (either way).
     */
    private fun neighbor(placed: PlacedFurniture, localX: Int, localZ: Int, perpendicular: Boolean): PlacedFurniture? {
        val yaw = placementYaw(placed)
        val (x, z) = PlacedFurniture.rotate(localX.toDouble(), localZ.toDouble(), yaw)
        val block = placed.base.location.block.getRelative(x.roundToInt(), 0, z.roundToInt())

        for (entity in block.world.getNearbyEntities(BoundingBox.of(block)) { it is ItemDisplay }) {
            val other = PlacedFurniture.fromEntity(entity) ?: continue
            if (other.base.uniqueId == placed.base.uniqueId || other.id != placed.id) {
                continue
            }
            if (other.base.location.block != block) {
                continue
            }

            val delta = abs((other.base.location.yaw - placed.base.location.yaw + 540) % 360 - 180)
            val matches = if (perpendicular) abs(delta - 90) < 1f else delta < 1f
            if (matches) {
                return other
            }
        }

        return null
    }
}
