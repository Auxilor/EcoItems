package com.willfp.ecoitems.items

import com.willfp.eco.core.EcoPlugin
import com.willfp.libreforge.toDispatcher
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.inventory.EquipmentSlotGroup

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
        val items = EcoItemFinder.toHolderProvider().provide(player.toDispatcher()).map { it.holder }

        val damageInst = player.getAttribute(Attribute.ATTACK_DAMAGE) ?: return
        val speedInst = player.getAttribute(Attribute.ATTACK_SPEED) ?: return

        damageInst.modifiers.filter { it.isFromEcoItems }.forEach {
            damageInst.removeModifier(it)
        }

        speedInst.modifiers.filter { it.isFromEcoItems }.forEach {
            speedInst.removeModifier(it)
        }

        for ((offset, item) in items.withIndex()) {
            if (item.baseDamage != null) {
                damageInst.addCompatibleModifier(
                    "Damage",
                    item.baseDamage - player.inventory.itemInMainHand.type.baseDamage,
                    offset
                )
            }

            if (item.baseAttackSpeed != null) {
                speedInst.addCompatibleModifier(
                    "Speed",
                    item.baseAttackSpeed - player.inventory.itemInMainHand.type.baseAttackSpeed,
                    offset
                )
            }
        }
    }

    private val AttributeModifier.isFromEcoItems: Boolean
        get() {
            if (this.name.startsWith("EcoItems Damage", true)
                || this.name.startsWith("EcoItems Speed", true)
            ) {
                return true
            }

            return this.key.namespace == "ecoitems" && (this.key.key.startsWith("damage")
                    || this.key.key.startsWith("speed"))
        }

    private fun AttributeInstance.addCompatibleModifier(
        attributeType: String,
        amount: Double,
        offset: Int
    ) {
        addModifier(
            AttributeModifier(
                plugin.createNamespacedKey("${attributeType.lowercase()}_$offset"),
                amount,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.ANY
            )
        )
    }
}
