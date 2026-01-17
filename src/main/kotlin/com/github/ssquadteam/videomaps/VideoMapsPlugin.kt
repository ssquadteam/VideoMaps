package com.github.ssquadteam.videomaps

import com.github.ssquadteam.talelib.TalePlugin
import com.github.ssquadteam.videomaps.command.VideoCommand
import com.github.ssquadteam.videomaps.command.VideoMapCommand
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import java.io.File
import java.nio.file.Files

class VideoMapsPlugin(init: JavaPluginInit) : TalePlugin(init) {

    companion object {
        lateinit var instance: VideoMapsPlugin
            private set
    }

    val dataFolder: File by lazy {
        pluginDataPath.toFile().also { it.mkdirs() }
    }

    override fun onSetup() {
        instance = this
        info("VideoMaps setting up...")

        // Create videos directory
        val videosDir = File(dataFolder, "videos")
        if (!videosDir.exists()) {
            videosDir.mkdirs()
            info("Created videos directory: ${videosDir.absolutePath}")
        }
    }

    override fun onStart() {
        info("VideoMaps started!")

        // Register commands
        taleCommands.register(VideoMapCommand())
        taleCommands.register(VideoCommand())

        info("VideoMaps ready!")
        info("Commands: /videomap, /video")
        info("Videos folder: ${File(dataFolder, "videos").absolutePath}")
    }

    override fun onShutdown() {
        // Stop all active displays
        MapDisplayManager.stopAll()
        info("VideoMaps disabled!")
    }
}
