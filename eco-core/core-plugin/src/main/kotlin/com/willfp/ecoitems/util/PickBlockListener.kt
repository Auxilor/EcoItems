package com.willfp.ecoitems.util

import com.willfp.ecoitems.blocks.EcoBlocks
import com.willfp.ecoitems.furniture.PlacedFurniture
import com.willfp.ecoitems.items.EcoItems
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryCreativeEvent

/**
 * Creative pick-block on custom content grabs the backing block's item
 * (note block, string, barrier...) - swap the cursor for the real item.
 */
object PickBlockListener : Listener {
    private val PICKABLE = setOf(
        Material.NOTE_BLOCK, Material.STRING, Material.TRIPWIRE,
        Material.CHORUS_PLANT, Material.BARRIER
    )

    @EventHandler(ignoreCancelled = true)
    fun onPick(event: InventoryCreativeEvent) {
        if (event.click != ClickType.CREATIVE || event.cursor.type !in PICKABLE) {
            return
        }

        val player = event.whoClicked as? Player ?: return
        val range = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)?.value ?: 4.5
        val target = player.rayTraceBlocks(range)?.hitBlock ?: return

        val replacement = if (event.cursor.type == Material.BARRIER) {
            PlacedFurniture.atBarrier(target)?.item?.itemStack
        } else {
            EcoBlocks.at(target)?.let { EcoItems.getByID(it.block.id)?.itemStack }
        } ?: return

        event.cursor = replacement
    }
}
