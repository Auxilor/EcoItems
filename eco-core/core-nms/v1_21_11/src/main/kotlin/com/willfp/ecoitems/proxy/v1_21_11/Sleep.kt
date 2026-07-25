package com.willfp.ecoitems.proxy.v1_21_11

import com.willfp.ecoitems.nms.SleepProxy
import net.minecraft.core.BlockPos
import org.bukkit.Location
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player

class Sleep : SleepProxy {
    override fun sleep(player: Player, location: Location) {
        (player as CraftPlayer).handle.startSleeping(
            BlockPos(location.blockX, location.blockY, location.blockZ)
        )
    }

    override fun wake(player: Player) {
        val handle = (player as CraftPlayer).handle
        if (handle.isSleeping) {
            handle.stopSleeping()
        }
    }
}
