package com.willfp.ecoitems.items

import com.willfp.eco.core.EcoPlugin
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemHeldEvent
import java.util.UUID

class ItemAttributeListener(private val plugin: EcoPlugin) : Listener {
    @EventHandler
    fun handle(event: PlayerItemHeldEvent) {
        if (event.isCancelled) {
            return
        }

        apply(event.player)
        plugin.scheduler.run { apply(event.player) }
    }

    private fun apply(player: Player) {
        val items = player.ecoItems.map { it.holder as EcoItem }

        val damageInst = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) ?: return
        val speedInst = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED) ?: return

        // Legacy (Pre-3.123.2)
        damageInst.removeModifier(
            AttributeModifier(
                UUID.nameUUIDFromBytes("ecoitems_ad".toByteArray()),
                "EcoItems Damage",
                1.0, // Irrelevant
                AttributeModifier.Operation.ADD_NUMBER
            )
        )
        speedInst.removeModifier(
            AttributeModifier(
                UUID.nameUUIDFromBytes("ecoitems_as".toByteArray()),
                "EcoItems Speed",
                1.0, // Irrelevant
                AttributeModifier.Operation.ADD_NUMBER
            )
        )

        for (offset in items.indices) {
            damageInst.removeModifier(
                AttributeModifier(
                    UUID.nameUUIDFromBytes("ecoitems_ad_$offset".toByteArray()),
                    "EcoItems Damage $offset",
                    1.0, // Irrelevant
                    AttributeModifier.Operation.ADD_NUMBER
                )
            )
            speedInst.removeModifier(
                AttributeModifier(
                    UUID.nameUUIDFromBytes("ecoitems_as_$offset".toByteArray()),
                    "EcoItems Speed $offset",
                    1.0, // Irrelevant
                    AttributeModifier.Operation.ADD_NUMBER
                )
            )
        }

        for ((offset, item) in items.withIndex()) {
            if (item.baseDamage != null) {
                damageInst.addModifier(
                    AttributeModifier(
                        UUID.nameUUIDFromBytes("ecoitems_ad_$offset".toByteArray()),
                        "EcoItems Damage $offset",
                        item.baseDamage - player.inventory.itemInMainHand.type.baseDamage,
                        AttributeModifier.Operation.ADD_NUMBER
                    )
                )
            }

            if (item.baseAttackSpeed != null) {
                speedInst.addModifier(
                    AttributeModifier(
                        UUID.nameUUIDFromBytes("ecoitems_as_$offset".toByteArray()),
                        "EcoItems Speed $offset",
                        item.baseAttackSpeed - player.inventory.itemInMainHand.type.baseAttackSpeed,
                        AttributeModifier.Operation.ADD_NUMBER
                    )
                )
            }
        }
    }
}
