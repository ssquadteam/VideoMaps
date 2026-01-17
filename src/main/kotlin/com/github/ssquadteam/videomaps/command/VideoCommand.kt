package com.github.ssquadteam.videomaps.command

import com.github.ssquadteam.talelib.command.TaleCommand
import com.github.ssquadteam.talelib.command.TaleContext
import com.github.ssquadteam.talelib.message.*
import com.github.ssquadteam.videomaps.MapDisplayManager
import com.github.ssquadteam.videomaps.VideoMapsPlugin
import com.github.ssquadteam.videomaps.video.ImageSequenceProvider
import java.nio.file.Files
import java.nio.file.Path

class VideoCommand : TaleCommand("video", "Play video on a map display") {

    init {
        aliases("playvideo", "mapvideo")
        subCommand(PlayFramesCommand())
        subCommand(ListVideosCommand())
        subCommand(PreloadCommand())
    }

    override fun onExecute(ctx: TaleContext) {
        ctx.reply("Video Commands:".info())
        ctx.reply("  /video frames <displayId> <folder> [fps]".muted())
        ctx.reply("  /video list".muted())
        ctx.reply("  /video preload <displayId> <folder>".muted())
        ctx.reply("".toMessage())
        ctx.reply("Place frame folders in: mods/SSquadTeam_VideoMaps/videos/".muted())
        ctx.reply("Extract frames: ffmpeg -i video.mp4 -vf \"scale=320:192\" folder/frame_%04d.png".muted())
    }

    class PlayFramesCommand : TaleCommand("frames", "Play from image sequence") {
        private val displayIdArg = stringArg("displayId", "Display ID")
        private val folderArg = stringArg("folder", "Frames folder name")
        private val fpsArg = optionalInt("fps", "Frames per second")

        override fun onExecute(ctx: TaleContext) {
            val displayId = ctx.get(displayIdArg)
            val folderName = ctx.get(folderArg)
            val fps = ctx.get(fpsArg) ?: 30

            val display = MapDisplayManager.get(displayId)
            if (display == null) {
                ctx.reply("Display '$displayId' not found! Create one first with /videomap create".error())
                return
            }

            if (display.getViewers().isEmpty()) {
                ctx.reply("No viewers on '$displayId'. Use /videomap join $displayId first!".warning())
                return
            }

            val videosDir = getVideosDirectory()
            val framesDir = videosDir.resolve(folderName)

            if (!Files.isDirectory(framesDir)) {
                ctx.reply("Folder '$folderName' not found!".error())
                ctx.reply("Expected at: $framesDir".muted())
                ctx.reply("Place frame images there (frame_0000.png, frame_0001.png, etc.)".muted())
                return
            }

            if (!ImageSequenceProvider.isValidFrameDirectory(framesDir)) {
                ctx.reply("No image files found in '$folderName'!".error())
                ctx.reply("Extract frames: ffmpeg -i video.mp4 -vf \"scale=${display.pixelWidth}:${display.pixelHeight}\" $framesDir/frame_%04d.png".muted())
                return
            }

            try {
                val provider = ImageSequenceProvider(framesDir, cacheFrames = true, loop = true)
                ctx.reply("Loading ${provider.frameCount} frames...".info())

                MapDisplayManager.startAnimation(
                    displayId = displayId,
                    frameProvider = provider,
                    frameCount = provider.frameCount,
                    fps = fps,
                    loop = true
                )

                ctx.reply("Playing '$folderName' on '$displayId' at $fps FPS (${provider.frameCount} frames)".success())
            } catch (e: Exception) {
                ctx.reply("Failed to load frames: ${e.message}".error())
            }
        }
    }

    class ListVideosCommand : TaleCommand("list", "List available videos") {
        override fun onExecute(ctx: TaleContext) {
            val videosDir = getVideosDirectory()

            if (!Files.exists(videosDir)) {
                ctx.reply("No videos directory found.".muted())
                ctx.reply("Create: $videosDir".muted())
                return
            }

            val items = Files.list(videosDir).toList()
            if (items.isEmpty()) {
                ctx.reply("No videos found in $videosDir".muted())
                return
            }

            ctx.reply("Available Videos:".info())
            for (item in items) {
                val name = item.fileName.toString()
                if (Files.isDirectory(item)) {
                    val frameCount = Files.list(item)
                        .filter { it.toString().lowercase().let { n -> n.endsWith(".png") || n.endsWith(".jpg") } }
                        .count()
                    ctx.reply("  [FRAMES] $name ($frameCount frames)".muted())
                }
            }
        }
    }

    class PreloadCommand : TaleCommand("preload", "Preload frames into memory") {
        private val displayIdArg = stringArg("displayId", "Display ID")
        private val folderArg = stringArg("folder", "Frames folder name")

        override fun onExecute(ctx: TaleContext) {
            val displayId = ctx.get(displayIdArg)
            val folderName = ctx.get(folderArg)

            val display = MapDisplayManager.get(displayId)
            if (display == null) {
                ctx.reply("Display '$displayId' not found!".error())
                return
            }

            val videosDir = getVideosDirectory()
            val framesDir = videosDir.resolve(folderName)

            if (!Files.isDirectory(framesDir)) {
                ctx.reply("Folder '$folderName' not found!".error())
                return
            }

            try {
                ctx.reply("Preloading frames from '$folderName'... This may take a while.".info())

                val provider = ImageSequenceProvider(framesDir, cacheFrames = true, loop = true)
                provider.preloadAll(display.pixelWidth, display.pixelHeight)

                ctx.reply("Preloaded ${provider.frameCount} frames!".success())
                ctx.reply("Use /video frames $displayId $folderName to play".muted())
            } catch (e: Exception) {
                ctx.reply("Failed to preload: ${e.message}".error())
            }
        }
    }

    companion object {
        fun getVideosDirectory(): Path {
            val pluginDir = VideoMapsPlugin.instance.dataFolder.toPath()
            val videosDir = pluginDir.resolve("videos")
            if (!Files.exists(videosDir)) {
                Files.createDirectories(videosDir)
            }
            return videosDir
        }
    }
}
