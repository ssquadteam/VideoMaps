package com.github.ssquadteam.videomaps

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class AnimationController(
    private val display: MapDisplay,
    private val frameProvider: FrameProvider,
    private val frameCount: Int,
    private val fps: Int,
    private val loop: Boolean
) {
    companion object {
        private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(2) { r ->
            Thread(r, "VideoMaps-Animation").apply { isDaemon = true }
        }
    }

    private val currentFrame = AtomicInteger(0)
    private val running = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)

    private var scheduledTask: ScheduledFuture<*>? = null

    val isRunning: Boolean get() = running.get()
    val isPaused: Boolean get() = paused.get()
    val frame: Int get() = currentFrame.get()

    fun start() {
        if (running.getAndSet(true)) return

        val intervalMs = 1000L / fps.coerceIn(1, 60)

        scheduledTask = executor.scheduleAtFixedRate(
            { tick() },
            0,
            intervalMs,
            TimeUnit.MILLISECONDS
        )
    }

    fun stop() {
        running.set(false)
        paused.set(false)
        scheduledTask?.cancel(false)
        scheduledTask = null
        currentFrame.set(0)
    }

    fun pause(): Boolean {
        if (!running.get()) return false
        paused.set(true)
        return true
    }

    fun resume(): Boolean {
        if (!running.get()) return false
        paused.set(false)
        return true
    }

    fun seekTo(frame: Int) {
        val targetFrame = if (frameCount > 0) {
            frame.coerceIn(0, frameCount - 1)
        } else {
            frame.coerceAtLeast(0)
        }
        currentFrame.set(targetFrame)
    }

    private fun tick() {
        if (!running.get() || paused.get()) return
        if (display.getViewers().isEmpty()) return

        try {
            val frameIndex = currentFrame.get()
            val pixels = frameProvider.getFrame(frameIndex, display.pixelWidth, display.pixelHeight)

            if (pixels != null && pixels.size == display.totalPixels) {
                display.sendFrame(pixels)
            }

            val nextFrame = frameIndex + 1

            if (frameCount > 0 && nextFrame >= frameCount) {
                if (loop) {
                    currentFrame.set(0)
                } else {
                    stop()
                }
            } else {
                currentFrame.set(nextFrame)
            }
        } catch (e: Exception) {
            VideoMapsPlugin.instance.warn("Animation error on display ${display.id}: ${e.message}")
        }
    }
}
