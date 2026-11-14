package com.willfp.ecoitems.libreforge

import com.willfp.ecoitems.plugin
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Player
import wiki.ecohub.config.Config
import wiki.ecohub.config.ViolationContext
import wiki.ecohub.game.entity.toDispatcher
import wiki.ecohub.rules.effect.Effects
import wiki.ecohub.rules.trigger.TriggerData

/** Player interactions with placed blocks and furniture. */
enum class ContentEvent(val key: String) {
    PUNCH("punch"),
    SHIFT_PUNCH("shift-punch"),
    RIGHT_CLICK("right-click"),
    SHIFT_RIGHT_CLICK("shift-right-click"),
    PLACE("place"),
    BREAK("break"),
    SIT("sit")
}

/**
 * Effects that run when a player interacts with placed content, configured
 * per event (like EcoMobs' mob events):
 *
 * ```yaml
 * block:
 *   effects:
 *     right-click:
 *       - id: send_message
 *         args:
 *           message: "Hello!"
 * ```
 */
class ContentEffects(owner: String, config: Config) {
    private val chains = ContentEvent.entries.mapNotNull { event ->
        val sections = config.getSubsections("effects.${event.key}")
        if (sections.isEmpty()) {
            return@mapNotNull null
        }

        val chain = Effects.compileChain(
            sections,
            ViolationContext(plugin, "$owner effects ${event.key}")
        ) ?: return@mapNotNull null

        event to chain
    }.toMap()

    fun dispatch(event: ContentEvent, player: Player, location: Location, block: Block? = null) {
        val chain = chains[event] ?: return

        val data = TriggerData(
            player = player,
            location = location,
            block = block,
            item = player.inventory.itemInMainHand
        )

        chain.trigger(data.dispatch(player.toDispatcher()))
    }

    /** The punch/right-click pair for the sneak state. */
    fun clickEvent(rightClick: Boolean, sneaking: Boolean): ContentEvent = when {
        rightClick && sneaking -> ContentEvent.SHIFT_RIGHT_CLICK
        rightClick -> ContentEvent.RIGHT_CLICK
        sneaking -> ContentEvent.SHIFT_PUNCH
        else -> ContentEvent.PUNCH
    }
}
