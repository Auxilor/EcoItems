package com.willfp.ecoitems.libreforge

import com.willfp.ecoitems.items.EcoItem
import com.willfp.libreforge.getHoldersOfType
import org.bukkit.entity.LivingEntity
import wiki.ecohub.config.ArgType
import wiki.ecohub.config.Config
import wiki.ecohub.config.arguments
import wiki.ecohub.core.registry.NoCompileData
import wiki.ecohub.game.entity.toDispatcher
import wiki.ecohub.rules.condition.Condition
import wiki.ecohub.rules.holder.Dispatcher
import wiki.ecohub.rules.holder.ProvidedHolder
import wiki.ecohub.rules.holder.get
import wiki.ecohub.ui.text.containsIgnoreCase

object ConditionHasEcoItem : Condition<NoCompileData>("has_ecoitem") {
    override val description = "Passes when the player has the specified EcoItem in a slot the item is active in."

    override val categories = setOf("inventory")

    override val arguments = arguments {
        require(
            "item",
            "You must specify the item!",
            description = "The ID of the EcoItem to check for.",
            type = ArgType.STRING
        )
    }

    override fun isMet(
        dispatcher: Dispatcher<*>,
        config: Config,
        holder: ProvidedHolder,
        compileData: NoCompileData
    ): Boolean {
        val entity = dispatcher.get<LivingEntity>() ?: return false

        return entity.toDispatcher().getHoldersOfType<EcoItem>()
            .map { it.id.key }
            .containsIgnoreCase(config.getString("item"))
    }
}
