package com.willfp.ecoweapons.weapons.conditions

import com.willfp.eco.core.config.interfaces.Config
import com.willfp.ecoweapons.weapons.WeaponUtils
import com.willfp.ecoweapons.weapons.Weapons
import com.willfp.libreforge.ConfigViolation
import com.willfp.libreforge.conditions.Condition
import org.bukkit.entity.Player

class ConditionHasFuel: Condition("has_fuel") {
    override fun isConditionMet(player: Player, config: Config): Boolean {
        val fuel = Weapons.getByID(config.getString("fuel")) ?: return true
        return WeaponUtils.hasFuelFor(player, fuel)
    }

    override fun validateConfig(config: Config): List<ConfigViolation> {
        val violations = mutableListOf<ConfigViolation>()

        config.getStringOrNull("fuel")
            ?: violations.add(
                ConfigViolation(
                    "fuel",
                    "You must specify the fuel required! (The fuel from which weapon, eg reaper_scythe)"
                )
            )

        return violations
    }
}