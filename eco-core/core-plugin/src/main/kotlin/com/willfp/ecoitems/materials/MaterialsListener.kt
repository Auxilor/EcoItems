package com.willfp.ecoitems.materials

import com.willfp.eco.core.drops.DropQueue
import com.willfp.ecoitems.items.ecoItem
import com.willfp.ecoitems.plugin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.inventory.PrepareSmithingEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.AnvilInventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.view.AnvilView
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Runs after eco's workstation and crafting listeners, which set the plain recipe output, and
 * rebuilds that output with the materials of its inputs.
 */
object MaterialsListener : Listener {
    private const val ANVIL_RESULT_SLOT = 2

    private val swappedOutParts = ConcurrentHashMap<UUID, ItemStack>()

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPrepareAnvil(event: PrepareAnvilEvent) {
        val player = event.view.player
        swappedOutParts.remove(player.uniqueId)

        val left = event.inventory.getItem(0)
        val right = event.inventory.getItem(1)
        val result = event.result
        val part = result?.ecoItem?.part

        if (part != null) {
            event.result = Materials.forItem(right)
                ?.takeIf { right != null && right.amount >= part.cost }
                ?.let { result.madeFrom(it) }
            (event.view as? AnvilView)?.repairItemCountCost = part.cost
            return
        }

        val swap = swapResult(item = left, part = right) ?: return
        event.result = swap.first
        swappedOutParts[player.uniqueId] = swap.second
        (event.view as? AnvilView)?.let { view ->
            view.repairCost = 1
            view.repairItemCountCost = 1
        }
    }

    private fun swapResult(item: ItemStack?, part: ItemStack?): Pair<ItemStack, ItemStack>? {
        val modular = item?.ecoItem?.modular?.takeIf { it.isSwappable }
        val shape = part?.ecoItem?.part?.shape
        val newMaterial = part?.partMaterial

        if (modular == null || shape == null || newMaterial == null) {
            return null
        }

        val materials = item.modularMaterials
        val slotId = modular.slots.entries
            .firstOrNull { (slotId, slotShape) -> slotShape == shape && materials[slotId] != newMaterial }
            ?.key
        val oldPart = slotId?.let { materials[it] }?.let { oldPartOf(shape, it) }

        return if (slotId == null || oldPart == null) {
            null
        } else {
            item.builtFrom(materials + (slotId to newMaterial)).asOne() to oldPart
        }
    }

    private fun oldPartOf(shape: String, material: Material): ItemStack? =
        partItemForShape(shape)?.let { partOf(it, material) }

    private fun ItemStack.asOne(): ItemStack = also { it.amount = 1 }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onTakeSwap(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val inventory = event.inventory as? AnvilInventory
        val oldPart = swappedOutParts[player.uniqueId]

        if (inventory == null || oldPart == null || event.rawSlot != ANVIL_RESULT_SLOT) {
            return
        }

        plugin.scheduler.on(player).runLater(1) {
            if (inventory.getItem(0) == null && swappedOutParts.remove(player.uniqueId, oldPart)) {
                DropQueue(player).addItem(oldPart).forceTelekinesis().push()
            }
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        swappedOutParts.remove(event.player.uniqueId)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPrepareSmithing(event: PrepareSmithingEvent) {
        val result = event.result
        if (result?.ecoItem?.part != null) {
            event.result = Materials.forItem(event.inventory.inputMineral)?.let { result.madeFrom(it) }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPrepareCraft(event: PrepareItemCraftEvent) {
        val result = event.inventory.result
        if (result?.ecoItem?.modular != null) {
            event.inventory.result = assembledFrom(result, event.inventory.matrix)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onCraft(event: CraftItemEvent) {
        val result = event.inventory.result
        if (result?.ecoItem?.modular != null) {
            event.inventory.result = assembledFrom(result, event.inventory.matrix)
        }
    }

    private fun assembledFrom(result: ItemStack, matrix: Array<ItemStack?>): ItemStack? {
        val modular = result.ecoItem?.modular ?: return null
        val parts = matrix.filterNotNull().filter { it.ecoItem?.part != null }.toMutableList()

        val materials = modular.slots.mapNotNull { (slotId, shape) ->
            parts.firstOrNull { it.ecoItem?.part?.shape == shape }
                ?.also { parts.remove(it) }
                ?.partMaterial
                ?.let { slotId to it }
        }.toMap()

        return if (materials.size == modular.slots.size) result.builtFrom(materials) else null
    }
}
