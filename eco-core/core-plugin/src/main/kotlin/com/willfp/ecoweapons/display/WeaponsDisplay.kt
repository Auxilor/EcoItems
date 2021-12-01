package com.willfp.ecoweapons.display

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.display.DisplayModule
import com.willfp.eco.core.display.DisplayPriority
import com.willfp.ecoweapons.weapons.WeaponUtils
import org.bukkit.inventory.ItemStack

class WeaponsDisplay(plugin: EcoPlugin) : DisplayModule(plugin, DisplayPriority.LOWEST) {
    override fun display(
        itemStack: ItemStack,
        vararg args: Any
    ) {
        val meta = itemStack.itemMeta ?: return
        val isFuel = WeaponUtils.getFuelFromItem(meta) != null
        val weapon = (
                if (isFuel) WeaponUtils.getFuelFromItem(meta)
                else WeaponUtils.getWeaponFromItem(meta)
                ) ?: return

        if (isFuel) {
            val fuelMeta = weapon.fuelItem.itemMeta ?: return
            val lore: MutableList<String> = fuelMeta.lore?.toMutableList() ?: return

            if (meta.hasLore()) {
                lore.addAll(meta.lore ?: return)
            }

            meta.lore = lore
            meta.setDisplayName(fuelMeta.displayName)
            itemStack.itemMeta = meta
        } else {
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