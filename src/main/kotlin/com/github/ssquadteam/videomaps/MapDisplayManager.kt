package com.github.ssquadteam.videomaps

import com.hypixel.hytale.server.core.entity.entities.Player
import java.util.concurrent.ConcurrentHashMap

object MapDisplayManager {

    private val displays = ConcurrentHashMap<String, MapDisplay>()
    private val activeAnimations = ConcurrentHashMap<String, AnimationController>()

    fun create(
        id: String,
        startChunkX: Int,
        startChunkZ: Int,
        widthChunks: Int,
        heightChunks: Int
    ): MapDisplay {
        val display = MapDisplay(id, startChunkX, startChunkZ, widthChunks, heightChunks)
        displays[id] = display
        return display
    }

    fun create(block: MapDisplayBuilder.() -> Unit): MapDisplay {
        val display = mapDisplay(block)
        displays[display.id] = display
        return display
    }

    fun get(id: String): MapDisplay? = displays[id]

    fun getAll(): Collection<MapDisplay> = displays.values.toList()

    fun remove(id: String): MapDisplay? {
        stopAnimation(id)
        return displays.remove(id)
    }

    fun removeAll() {
        stopAll()
        displays.clear()
    }

    fun count(): Int = displays.size

    fun exists(id: String): Boolean = displays.containsKey(id)

    fun startAnimation(
        displayId: String,
        frameProvider: FrameProvider,
        frameCount: Int = 0,
        fps: Int = 24,
        loop: Boolean = true
    ): Boolean {
        val display = displays[displayId] ?: return false

        // Stop any existing animation
        stopAnimation(displayId)

        val controller = AnimationController(display, frameProvider, frameCount, fps, loop)
        activeAnimations[displayId] = controller
        controller.start()

        return true
    }

    fun stopAnimation(displayId: String): Boolean {
        val controller = activeAnimations.remove(displayId)
        controller?.stop()
        return controller != null
    }

    fun pauseAnimation(displayId: String) = activeAnimations[displayId]?.pause() ?: false

    fun resumeAnimation(displayId: String) = activeAnimations[displayId]?.resume() ?: false

    fun isAnimating(displayId: String) = activeAnimations[displayId]?.isRunning ?: false

    fun stopAll() {
        activeAnimations.values.forEach { it.stop() }
        activeAnimations.clear()
    }

    fun addViewer(displayId: String, player: Player): Boolean {
        val display = displays[displayId] ?: return false
        display.addViewer(player)
        return true
    }

    fun removeViewer(displayId: String, player: Player): Boolean {
        val display = displays[displayId] ?: return false
        display.removeViewer(player)
        return true
    }

    fun removeViewerFromAll(player: Player) {
        displays.values.forEach { it.removeViewer(player) }
    }
}

fun interface FrameProvider {
    fun getFrame(frameIndex: Int, width: Int, height: Int): IntArray?
}
