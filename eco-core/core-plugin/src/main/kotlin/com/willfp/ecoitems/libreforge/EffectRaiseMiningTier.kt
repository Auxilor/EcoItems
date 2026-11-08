package com.willfp.ecoitems.libreforge

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoitems.BuildConfig
import com.willfp.ecoitems.ext.minimumMiningTier
import com.willfp.ecoitems.ext.withMaterials
import com.willfp.ecoitems.items.ecoItem
import com.willfp.ecoitems.materials.MiningTiers
import com.willfp.ecoitems.premium.PremiumFeature
import com.willfp.ecoitems.premium.PremiumFeatures
import com.willfp.libreforge.ArgType
import com.willfp.libreforge.NoCompileData
import com.willfp.libreforge.ViolationContext
import com.willfp.libreforge.arguments
import com.willfp.libreforge.effects.Effect
import com.willfp.libreforge.triggers.TriggerData

object EffectRaiseMiningTier : Effect<NoCompileData>("raise_mining_tier") {
    override val description = "Raises the mining tier of the found modular item to at least the given tier."

    override val categories = setOf("inventory")

    override val additionalInfo = listOf(
        "Only changes modular items that mine. The tier stays when parts are swapped, and a lower tier does nothing.",
        "Requires the paid version of EcoItems."
    )

    override val isPermanent = false

    override val arguments = arguments {
        require(
            name = "tier",
            message = "You must specify a valid tier! (wooden, stone, iron, diamond or netherite)",
            getter = Config::getString,
            predicate = { MiningTiers.isKnown(it) }
        )
        describe(
            name = "tier",
            description = "The lowest tier the item mines at: wooden, stone, iron, diamond or netherite.",
            type = ArgType.STRING,
            choices = listOf("wooden", "stone", "iron", "diamond", "netherite"),
            example = "diamond"
        )
    }

    override fun makeCompileData(config: Config, context: ViolationContext): NoCompileData {
        PremiumFeatures.report(PremiumFeature.RAISE_MINING_TIER, context.toString().removeSuffix(" -> args"))
        return NoCompileData
    }

    override fun onTrigger(config: Config, data: TriggerData, compileData: NoCompileData): Boolean {
        val item = data.foundItem
            ?.takeIf { !BuildConfig.FREE_VERSION && it.ecoItem?.modular?.miningBlocks != null }
            ?: return false

        item.minimumMiningTier = MiningTiers.higher(first = item.minimumMiningTier, second = config.getString("tier"))
        item.itemMeta = item.withMaterials().itemMeta
        return true
    }
}
