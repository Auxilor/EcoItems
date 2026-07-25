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

    // gui.vsh runs before resource packs exist, so - like vanilla's own copy -
    // it can't #moj_import: the uniform blocks are inlined instead. 1.21.8 is
    // GLSL 150 and carries an extra LineWidth field in DynamicTransforms;
    // 1.21.9 onwards is GLSL 330 without it.
    private fun template(version: Int, lineWidth: Boolean) = """#version $version

// Copied from dynamictransforms.glsl and projection.glsl, which can't be
// imported into a shader used during startup.
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;${if (lineWidth) "\n    float LineWidth;" else ""}
};
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec4 Color;

out vec4 vertexColor;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    vertexColor = Color;
%PATCHES%
}
"""

    /**
     * True when the pack needs the shader overlays declared in pack.mcmeta:
     * the interface tweaks ship a per-version gui shader.
     */
    fun isEnabled(settings: PackSettings) =
        settings.hideScoreboardBackground || settings.hideTablistBackground

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

        val modern = template(330, lineWidth = false).replace("%PATCHES%", patches).encodeToByteArray()
        val legacy = template(150, lineWidth = true).replace("%PATCHES%", patches).encodeToByteArray()

        for (prefix in listOf("", "overlay_pre_26/", "overlay_26/", "overlay_26_2/")) {
            entries["${prefix}assets/minecraft/shaders/core/gui.vsh"] = modern
        }

        entries["overlay_1_21_8/assets/minecraft/shaders/core/gui.vsh"] = legacy
    }
}
