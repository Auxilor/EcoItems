package com.willfp.ecoitems.proxy.v26_1_2

import com.willfp.eco.core.packet.PacketEvent
import com.willfp.ecoitems.nms.ActionBarDetection as ActionBarCallback
import com.willfp.ecoitems.nms.ActionBarDetectionProxy
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket

class ActionBarDetection : ActionBarDetectionProxy {
    override fun onSend(event: PacketEvent) {
        val packet = event.packet.handle

        if (packet is ClientboundSystemChatPacket && packet.overlay) {
            ActionBarCallback.onDetect?.invoke(event.player)
        }
    }
}
