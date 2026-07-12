package com.willfp.ecoitems.nms

import com.willfp.eco.core.packet.PacketListener
import org.bukkit.entity.Player

/**
 * Detects action bar messages sent by other plugins, so HUDs can yield
 * instead of stomping them.
 */
interface ActionBarDetectionProxy : PacketListener

/**
 * The callback seam: NMS modules are part of the free jar, so they can't
 * reference the paid HUD system directly. The paid feature installs the
 * callback; in free builds it stays null and detection is a no-op.
 */
object ActionBarDetection {
    var onDetect: ((Player) -> Unit)? = null
}
