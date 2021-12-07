package com.willfp.ecoweapons.fuels

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.ConfigViolation
import com.willfp.libreforge.conditions.Condition
import org.bukkit.entity.Player

class ConditionHasFuel: Condition("has_fuel") {
    override fun isConditionMet(player: Player, config: Config): Boolean {
        val fuel = Fuels.getByID(config.getString("fuel")) ?: return false
        return FuelUtils.hasFuel(player, fuel)
    }

    override fun validateConfig(config: Config): List<ConfigViolation> {
        val violations = mutableListOf<ConfigViolation>()

        config.getStringOrNull("fuel")
            ?: violations.add(
                ConfigViolation(
                    "fuel",
                    "You must specify the fuel required!"
                )
            )

        return violations
    }
}