package com.willfp.ecoweapons.display

import com.willfp.eco.core.EcoPlugin
import com.willfp.eco.core.display.DisplayModule
import com.willfp.eco.core.display.DisplayPriority
import org.bukkit.inventory.ItemStack
import com.willfp.ecoweapons.weapons.WeaponUtils

class WeaponsDisplay(plugin: EcoPlugin) : DisplayModule(plugin, DisplayPriority.LOWEST) {
    override fun display(
        itemStack: ItemStack,
        vararg args: Any
    ) {
        val meta = itemStack.itemMeta ?: return
        val weapon = WeaponUtils.getWeaponFromItem(meta) ?: return
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