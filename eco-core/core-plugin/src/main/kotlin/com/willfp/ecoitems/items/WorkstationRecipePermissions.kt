package com.willfp.ecoitems.items

import com.willfp.eco.core.recipe.workstation.WorkstationRecipe
import com.willfp.ecoitems.plugin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack

/**
 * Enforces `permission` on workstation recipes.
 *
 * Eco stores the permission on the recipe but never checks it for workstations
 * (only crafting tables are gated, inside eco itself), so EcoItems enforces it here.
 *
 * The check happens when the player takes the finished item out of the result slot,
 * not when they load the inputs. Gating the inputs would break vanilla for anyone
 * without the permission - if a recipe smelts iron ore, blocking iron ore going into
 * a furnace would stop them smelting iron at all. Gating the result only ever blocks
 * the custom item itself.
 */
object WorkstationRecipePermissions : Listener {
    /** Slots a finished item can be taken from, per workstation. */
    private val resultSlots = mapOf(
        InventoryType.STONECUTTER to setOf(1),
        InventoryType.SMITHING to setOf(3),
        InventoryType.ANVIL to setOf(2),
        InventoryType.FURNACE to setOf(2),
        InventoryType.BLAST_FURNACE to setOf(2),
        InventoryType.SMOKER to setOf(2),
        // A brewing stand brews in place, so the bottle slots are the result slots.
        InventoryType.BREWING to setOf(0, 1, 2)
    )

    /** Workstation recipes that carry a permission. Rebuilt on every reload. */
    private val gated = mutableListOf<WorkstationRecipe>()

    fun clear() {
        gated.clear()
    }

    fun track(recipe: WorkstationRecipe) {
        if (recipe.permission != null) {
            gated += recipe
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onTakeResult(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val top = event.view.topInventory

        val slots = resultSlots[top.type] ?: return
        if (event.rawSlot !in slots) return
        if (event.clickedInventory !== top) return

        if (!isDenied(player, top.getItem(event.rawSlot))) return

        event.isCancelled = true

        // The client has already drawn the item into the cursor, so force it back.
        plugin.scheduler.run { player.updateInventory() }
    }

    private fun isDenied(player: Player, item: ItemStack?): Boolean {
        if (item == null || item.type.isAir) {
            return false
        }

        return gated.any { recipe ->
            recipe.output?.isSimilar(item) == true && !player.hasPermission(recipe.permission!!)
        }
    }
}
