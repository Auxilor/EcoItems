package com.willfp.ecoweapons.weapons

import com.willfp.eco.core.EcoPlugin
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemHeldEvent
import java.util.UUID

class WeaponModifierListener(private val plugin: EcoPlugin) : Listener {
    @EventHandler
    fun handle(event: PlayerItemHeldEvent) {
        if (event.isCancelled) {
            return
        }

        apply(event.player)
        plugin.scheduler.run { apply(event.player) }
    }

    private fun apply(player: Player) {
        val weapon = WeaponUtils.getWeaponOnPlayer(player)

        val damageInst = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) ?: return

        damageInst.removeModifier(
            AttributeModifier(
                UUID.nameUUIDFromBytes("ecoweapons_ad".toByteArray()),
                "EcoWeapons Calibration",
                1.0, // Irrelevant
                AttributeModifier.Operation.ADD_NUMBER
            )
        )

        if (weapon != null) {
            damageInst.addModifier(
                AttributeModifier(
                    UUID.nameUUIDFromBytes("ecoweapons_ad".toByteArray()),
                    "EcoWeapons Calibration",
                    weapon.baseDamage - player.inventory.itemInMainHand.type.getBaseDamage(),
                    AttributeModifier.Operation.ADD_NUMBER
                )
            )
        }
    }
}