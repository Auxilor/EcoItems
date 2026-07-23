package com.willfp.ecoitems.util

import com.willfp.eco.core.Prerequisite
import com.willfp.ecoitems.blocks.EcoBlocks
import com.willfp.ecoitems.furniture.PlacedFurniture
import com.willfp.ecoitems.items.EcoItems
import com.willfp.ecoitems.items.ecoItem
import io.papermc.paper.event.player.PlayerPickBlockEvent
import io.papermc.paper.event.player.PlayerPickEntityEvent
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack

/**
 * Creative pick-block on custom content grabs the backing block's item
 * (note block, string, barrier...) - hand over the real item instead.
 *
 * Only instantiate on Paper - registering this on Spigot would fail
 * reflection. Pick block is resolved server-side, so there is no
 * platform-neutral event to hook.
 */
class PickBlockListener : Listener {
    /** Custom blocks, and furniture picked by its barriers. */
    @EventHandler(ignoreCancelled = true)
    fun onPickBlock(event: PlayerPickBlockEvent) {
        val block = event.block

        val item = EcoBlocks.at(block)?.let { EcoItems.getByID(it.block.id)?.itemStack }
            ?: PlacedFurniture.atBarrier(block)?.item?.itemStack
            ?: return

        event.isCancelled = true
        pick(event.player, item, event.targetSlot)
    }

    /** Furniture picked by its display or hitbox entity. */
    @EventHandler(ignoreCancelled = true)
    fun onPickEntity(event: PlayerPickEntityEvent) {
        val item = PlacedFurniture.fromEntity(event.entity)?.item?.itemStack ?: return

        event.isCancelled = true
        pick(event.player, item, event.targetSlot)
    }

    /**
     * The vanilla pick behaviour the cancelled event would have run: select
     * the item where it already is, pull it into the hotbar when it's further
     * back, and conjure one in creative when the player has none.
     */
    private fun pick(player: Player, item: ItemStack, targetSlot: Int) {
        val inventory = player.inventory
        val ecoItem = item.ecoItem

        val existing = if (ecoItem == null) null else {
            (0 until inventory.storageContents.size)
                .firstOrNull { inventory.getItem(it)?.ecoItem == ecoItem }
        }

        if (existing != null && existing < 9) {
            inventory.heldItemSlot = existing
            return
        }

        val slot = targetSlot.coerceIn(0, 8)

        if (existing != null) {
            val held = inventory.getItem(slot)
            inventory.setItem(slot, inventory.getItem(existing))
            inventory.setItem(existing, held)
        } else {
            if (player.gameMode != GameMode.CREATIVE) {
                return
            }
            inventory.setItem(slot, item)
        }

        inventory.heldItemSlot = slot
    }

    companion object {
        fun createIfSupported(): Listener? =
            if (Prerequisite.HAS_PAPER.isMet) PickBlockListener() else null
    }
}
