package com.willfp.ecoitems.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.eco.util.containsIgnoreCase
import com.willfp.ecoitems.items.EcoItem
import com.willfp.libreforge.Dispatcher
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.ProvidedHolder
import com.willfp.libreforge.arguments
import com.willfp.libreforge.conditions.Condition
import com.willfp.libreforge.get
import com.willfp.libreforge.getHoldersOfType
import com.willfp.libreforge.holders
import com.willfp.libreforge.toDispatcher
import org.bukkit.entity.LivingEntity

object ConditionHasEcoItem : Condition<NoCompileData>("has_ecoitem") {
    override val arguments = arguments {
        require("item", "You must specify the item!")
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
