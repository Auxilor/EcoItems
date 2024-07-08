package com.willfp.ecoitems.items

import com.willfp.eco.core.EcoPlugin
import com.willfp.ecoitems.compat.ModernCompatibilityProxy
import com.willfp.ecoitems.compat.ifModern
import com.willfp.libreforge.toDispatcher
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
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
        val items = EcoItemFinder.toHolderProvider().provide(player.toDispatcher()).map { it.holder }

        val damageInst = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) ?: return
        val speedInst = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED) ?: return

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

            var isFromEcoItems = false

            ifModern {
                useProxy<ModifierHelper> {
                    isFromEcoItems = isFromEcoItems(this@isFromEcoItems)
                }
            }

            return isFromEcoItems
        }

    private fun AttributeInstance.addCompatibleModifier(
        attributeType: String,
        amount: Double,
        offset: Int
    ) {
        ifModern {
            useProxy<ModifierHelper> {
                addModifier(
                    createModifier(plugin, attributeType, amount, offset)
                )
            }
        }.otherwise {
            addModifier(
                AttributeModifier(
                    UUID.nameUUIDFromBytes("ecoitems_$offset".toByteArray()),
                    "EcoItems $attributeType $offset",
                    amount,
                    AttributeModifier.Operation.ADD_NUMBER
                )
            )
        }
    }
}

@ModernCompatibilityProxy("ModifierHelperImpl")
interface ModifierHelper {
    fun createModifier(
        plugin: EcoPlugin,
        attributeType: String,
        amount: Double,
        offset: Int
    ): AttributeModifier

    fun isFromEcoItems(modifier: AttributeModifier): Boolean
}
