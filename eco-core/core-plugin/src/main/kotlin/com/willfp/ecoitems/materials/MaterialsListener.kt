package com.willfp.ecoitems.materials

import com.willfp.eco.core.drops.DropQueue
import com.willfp.ecoitems.ext.madeFrom
import com.willfp.ecoitems.items.ecoItem
import com.willfp.ecoitems.plugin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
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
 * rebuilds that output with the materials of its inputs through [ModularItemService].
 */
object MaterialsListener : Listener {
    private const val ANVIL_RESULT_SLOT = 2

    private val swappedOutParts = ConcurrentHashMap<UUID, ItemStack>()

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPrepareAnvil(event: PrepareAnvilEvent) {
        val player = event.view.player
        swappedOutParts.remove(player.uniqueId)

        val result = event.result
        val part = result?.ecoItem?.part

        if (part != null) {
            event.result = ModularItemService.partResult(result, part, event.inventory.getItem(1))
            (event.view as? AnvilView)?.repairItemCountCost = part.cost
            return
        }

        val swap = ModularItemService.swap(
            item = event.inventory.getItem(0),
            part = event.inventory.getItem(1)
        ) ?: return
        event.result = swap.result
        swap.returnedPart?.let { swappedOutParts[player.uniqueId] = it }
        (event.view as? AnvilView)?.let { view ->
            view.repairCost = swap.cost
            view.repairItemCountCost = 1
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onTakeSwap(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val inventory = event.inventory as? AnvilInventory
        val oldPart = swappedOutParts[player.uniqueId]

        if (inventory == null || oldPart == null || event.rawSlot != ANVIL_RESULT_SLOT) {
            return
        }

        swappedOutParts.remove(player.uniqueId)
        plugin.scheduler.on(player).runLater(1) {
            if (inventory.getItem(0) == null) {
                DropQueue(player).addItem(oldPart).forceTelekinesis().push()
            } else {
                swappedOutParts.putIfAbsent(player.uniqueId, oldPart)
            }
        }
    }

    @EventHandler
    fun onCloseAnvil(event: InventoryCloseEvent) {
        if (event.inventory is AnvilInventory) {
            swappedOutParts.remove(event.player.uniqueId)
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
            event.inventory.result = ModularItemService.assembledFrom(result, event.inventory.matrix)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onCraft(event: CraftItemEvent) {
        val result = event.inventory.result
        if (result?.ecoItem?.modular != null) {
            event.inventory.result = ModularItemService.assembledFrom(result, event.inventory.matrix)
        }
    }
}
