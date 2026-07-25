package com.willfp.ecoitems.glyphs

import com.willfp.ecoitems.plugin
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.imageio.metadata.IIOMetadataNode
import kotlin.math.roundToInt

/**
 * GIF glyph support: a glyph whose texture has a .gif file (and no .png)
 * animates automatically - the frame count and speed come from the GIF, and
 * the pack build materializes it into the sprite-sheet png the animation
 * system renders.
 */
object GifTexture {
    data class Meta(val frames: Int, val fps: Int)

    /** The gif file for a texture reference ([ns:]path, extension ignored). */
    fun file(texture: String): File {
        val plain = texture.removeSuffix(".png").removeSuffix(".gif")
        val namespace = if (":" in plain) plain.substringBefore(':') else "ecoitems"
        val path = plain.substringAfter(':')
        return plugin.dataFolder.resolve("pack/assets/$namespace/textures/$path.gif")
    }

    /** Frame count and speed, or null when the file isn't a usable animation. */
    fun meta(file: File): Meta? {
        if (!file.isFile) {
            return null
        }

        return runCatching {
            openReader(file) { reader ->
                val frames = reader.getNumImages(true)
                if (frames < 2) {
                    return@openReader null
                }

                // Delay comes from the first frame's GraphicControlExtension,
                // in hundredths of a second; 0 conventionally means 10fps.
                val delay = graphicControl(reader.getImageMetadata(0))
                    ?.getAttribute("delayTime")?.toIntOrNull() ?: 0
                val fps = if (delay <= 0) 10 else (100.0 / delay).roundToInt()

                Meta(
                    frames.coerceAtMost(GlyphAnimation.MAX_FRAMES),
                    fps.coerceIn(1, GlyphAnimation.MAX_FPS)
                )
            }
        }.getOrNull()
    }

    /**
     * Decodes the first [frames] frames into a vertical sprite-sheet strip,
     * compositing partial frames onto the previous canvas like a GIF viewer.
     */
    fun sheet(file: File, frames: Int): BufferedImage? = runCatching {
        openReader(file) { reader ->
            val total = reader.getNumImages(true).coerceAtMost(frames)
            val width = reader.getWidth(0)
            val height = reader.getHeight(0)

            val sheet = BufferedImage(width, height * total, BufferedImage.TYPE_INT_ARGB)
            val canvas = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val graphics = canvas.createGraphics()

            for (index in 0 until total) {
                val frame = reader.read(index)
                val descriptor = descriptor(reader.getImageMetadata(index))
                val x = descriptor?.getAttribute("imageLeftPosition")?.toIntOrNull() ?: 0
                val y = descriptor?.getAttribute("imageTopPosition")?.toIntOrNull() ?: 0

                graphics.drawImage(frame, x, y, null)
                sheet.createGraphics().apply {
                    drawImage(canvas, 0, height * index, null)
                    dispose()
                }

                val disposal = graphicControl(reader.getImageMetadata(index))
                    ?.getAttribute("disposalMethod")
                if (disposal == "restoreToBackgroundColor") {
                    val clear = canvas.createGraphics()
                    clear.composite = java.awt.AlphaComposite.Clear
                    clear.fillRect(x, y, frame.width, frame.height)
                    clear.dispose()
                }
            }

            graphics.dispose()
            sheet
        }
    }.getOrNull()

    private fun <T> openReader(file: File, block: (javax.imageio.ImageReader) -> T?): T? {
        val reader = ImageIO.getImageReadersByFormatName("gif").next()
        return ImageIO.createImageInputStream(file).use { stream ->
            reader.input = stream
            try {
                block(reader)
            } finally {
                reader.dispose()
            }
        }
    }

    private fun graphicControl(metadata: javax.imageio.metadata.IIOMetadata): IIOMetadataNode? =
        node(metadata, "GraphicControlExtension")

    private fun descriptor(metadata: javax.imageio.metadata.IIOMetadata): IIOMetadataNode? =
        node(metadata, "ImageDescriptor")

    private fun node(metadata: javax.imageio.metadata.IIOMetadata, name: String): IIOMetadataNode? {
        val root = metadata.getAsTree("javax_imageio_gif_image_1.0") as IIOMetadataNode
        var child = root.firstChild
        while (child != null) {
            if (child.nodeName == name) {
                return child as IIOMetadataNode
            }
            child = child.nextSibling
        }
        return null
    }
}
