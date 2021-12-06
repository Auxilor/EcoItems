package com.willfp.ecoweapons.display

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.display.DisplayModule
import com.willfp.eco.core.display.DisplayPriority
import com.willfp.ecoweapons.fuels.FuelUtils
import com.willfp.ecoweapons.weapons.WeaponUtils
import org.bukkit.inventory.ItemStack

class WeaponsDisplay(plugin: EcoPlugin) : DisplayModule(plugin, DisplayPriority.LOWEST) {
    override fun display(
        itemStack: ItemStack,
        vararg args: Any
    ) {
        val meta = itemStack.itemMeta ?: return
        val weapon = WeaponUtils.getWeaponFromItem(meta)
        val fuel = FuelUtils.getFuelFromItem(meta)

        if (fuel != null) {
            val fuelMeta = fuel.itemStack.itemMeta ?: return
            val lore: MutableList<String> = fuelMeta.lore?.toMutableList() ?: return

            if (meta.hasLore()) {
                lore.addAll(meta.lore ?: return)
            }

            meta.lore = lore
            meta.setDisplayName(fuelMeta.displayName)
            itemStack.itemMeta = meta
        }

        if (weapon != null) {
            val weaponMeta = weapon.itemStack.itemMeta ?: return
            val lore: MutableList<String> = weaponMeta.lore?.toMutableList() ?: return

            if (meta.hasLore()) {
                lore.addAll(meta.lore ?: return)
            }

            meta.lore = lore
            meta.setDisplayName(weaponMeta.displayName)
            meta.addItemFlags(*weaponMeta.itemFlags.toTypedArray())
            itemStack.itemMeta = meta
        }
    }
}