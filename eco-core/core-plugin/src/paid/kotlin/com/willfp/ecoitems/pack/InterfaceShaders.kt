package com.willfp.ecoitems.pack

import com.willfp.ecoitems.EcoItemsPlugin

/**
 * Optional client-side UI tweaks patched into the gui core shader: hiding
 * the scoreboard sidebar background and/or the tab list background. The
 * quads are identified by their screen region and the GUI overlay z-window,
 * then made fully transparent.
 */
object InterfaceShaders {
    // Text drawn over the backgrounds is unaffected - it renders through the
    // text shaders, not gui.
    private const val SCOREBOARD = """
    // The sidebar: right edge, vertically centered.
    if (gl_Position.y > -0.5 && gl_Position.y < 0.4 && gl_Position.x > 0.0 && gl_Position.x <= 1.0 && Position.z > 5.0 && Position.z < 2750.0) {
        vertexColor.a = 0.0;
    }"""

    private const val TABLIST = """
    // The player list: top half of the screen.
    if (gl_Position.y > 0.4 && gl_Position.y < 2.0 && gl_Position.x > -1.0 && gl_Position.x <= 1.0 && Position.z > 5.0 && Position.z < 2750.0) {
        vertexColor.a = 0.0;
    }"""

    private const val TEMPLATE = """#version 150

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;

out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexColor = Color;
%PATCHES%
}
"""

    fun generate(plugin: EcoItemsPlugin, settings: PackSettings, entries: MutableMap<String, ByteArray>) {
        val patches = buildString {
            if (settings.hideScoreboardBackground) append(SCOREBOARD)
            if (settings.hideTablistBackground) append(TABLIST)
        }

        if (patches.isEmpty()) {
            return
        }

        if ("assets/minecraft/shaders/core/gui.vsh" in entries) {
            plugin.logger.warning(
                "An imported or user-supplied gui shader was replaced by the interface tweaks from pack.yml"
            )
        }

        val shader = TEMPLATE.replace("%PATCHES%", patches).encodeToByteArray()

        // The same GLSL works from 1.21.8 through the current clients, so it
        // goes in the base pack and every shader overlay.
        for (prefix in listOf("", "overlay_pre_26/", "overlay_26/", "overlay_26_2/")) {
            entries["${prefix}assets/minecraft/shaders/core/gui.vsh"] = shader
        }
    }
}
