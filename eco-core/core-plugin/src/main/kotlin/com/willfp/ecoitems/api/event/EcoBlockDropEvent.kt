package com.willfp.ecoitems.api.event

import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.inventory.ItemStack

/**
 * Called when an EcoItems block or crop is broken and its drops are rolled,
 * before anything is given or dropped.
 *
 * [player] is null for a break with no player: an explosion, water, or a plugin
 * such as a minion rolling the drops through eco's `TestableBlock.getDrops`.
 * Player breaks then continue through libreforge's drop pipeline and eco's
 * `DropQueuePushEvent`.
 *
 * Change [items] or [xp] to change the drops. Cancelling drops nothing.
 */
class EcoBlockDropEvent(
    val block: Block,
    /** The EcoItems block id. */
    val blockId: String,
    val player: Player?,
    val tool: ItemStack?,
    var items: MutableList<ItemStack>,
    var xp: Int
) : Event(), Cancellable {
    private var cancelled = false

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun getHandlers(): HandlerList {
        return handlerList
    }

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
