package com.github.ssquadteam.videomaps.video

import com.github.ssquadteam.videomaps.FrameProvider
import com.github.ssquadteam.videomaps.VideoMapsPlugin
import java.awt.Image
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

class ImageSequenceProvider(
    private val framesDir: Path,
    private val cacheFrames: Boolean = true,
    private val loop: Boolean = true
) : FrameProvider {

    companion object {
        fun fromDirectory(dirPath: String, cache: Boolean = true, loop: Boolean = true) =
            ImageSequenceProvider(Path.of(dirPath), cache, loop)

        fun isValidFrameDirectory(dir: Path): Boolean {
            if (!Files.isDirectory(dir)) return false
            return Files.list(dir).anyMatch { path ->
                val name = path.fileName.toString().lowercase()
                name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
            }
        }
    }

    private val frameFiles: List<Path>
    private val frameCache = mutableMapOf<Int, IntArray>()

    val frameCount: Int get() = frameFiles.size

    init {
        frameFiles = Files.list(framesDir)
            .filter { path ->
                val name = path.fileName.toString().lowercase()
                name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg")
            }
            .sorted(compareBy { extractFrameNumber(it) })
            .toList()

        if (frameFiles.isEmpty()) {
            throw IllegalArgumentException("No image files found in $framesDir")
        }

        VideoMapsPlugin.instance.info("Loaded ${frameFiles.size} frames from $framesDir")
    }

    override fun getFrame(frameIndex: Int, width: Int, height: Int): IntArray? {
        if (frameFiles.isEmpty()) return null

        val idx = if (loop) {
            frameIndex % frameFiles.size
        } else {
            if (frameIndex >= frameFiles.size) return null
            frameIndex
        }

        // Check cache first
        if (cacheFrames && frameCache.containsKey(idx)) {
            return frameCache[idx]
        }

        // Load the frame
        val pixels = loadFrame(frameFiles[idx], width, height) ?: return null

        // Cache if enabled
        if (cacheFrames) {
            frameCache[idx] = pixels
        }

        return pixels
    }

    fun preloadAll(width: Int, height: Int) {
        VideoMapsPlugin.instance.info("Preloading ${frameFiles.size} frames...")
        for (i in frameFiles.indices) {
            val pixels = loadFrame(frameFiles[i], width, height)
            if (pixels != null) {
                frameCache[i] = pixels
            }
            if (i % 100 == 0) {
                VideoMapsPlugin.instance.info("Preloaded $i/${frameFiles.size} frames")
            }
        }
        VideoMapsPlugin.instance.info("Preload complete!")
    }

    fun clearCache() {
        frameCache.clear()
    }

    private fun loadFrame(path: Path, width: Int, height: Int): IntArray? {
        return try {
            val originalImage = ImageIO.read(path.toFile()) ?: return null
            val scaledImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val g2d = scaledImage.createGraphics()
            g2d.drawImage(originalImage.getScaledInstance(width, height, Image.SCALE_FAST), 0, 0, null)
            g2d.dispose()

            val dataBuffer = scaledImage.raster.dataBuffer
            if (dataBuffer is DataBufferInt) {
                dataBuffer.data.clone()
            } else {
                IntArray(width * height) { i -> scaledImage.getRGB(i % width, i / width) }
            }
        } catch (e: Exception) {
            VideoMapsPlugin.instance.warn("Failed to load frame $path: ${e.message}")
            null
        }
    }

    private fun extractFrameNumber(path: Path): Int {
        val name = path.fileName.toString()
        val match = Regex("\\d+").find(name)
        return match?.value?.toIntOrNull() ?: 0
    }
}
