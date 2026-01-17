package com.github.ssquadteam.videomaps.command

import com.github.ssquadteam.talelib.command.TaleCommand
import com.github.ssquadteam.talelib.command.TaleContext
import com.github.ssquadteam.talelib.message.*
import com.github.ssquadteam.talelib.worldmap.MapColors
import com.github.ssquadteam.videomaps.FrameProviders
import com.github.ssquadteam.videomaps.MapDisplayManager
import com.github.ssquadteam.videomaps.mapDisplay

class VideoMapCommand : TaleCommand("videomap", "Control map video displays") {

    init {
        aliases("vmap", "mapdisplay")

        subCommand(CreateCommand())
        subCommand(RemoveCommand())
        subCommand(ListCommand())
        subCommand(JoinCommand())
        subCommand(LeaveCommand())
        subCommand(PlayCommand())
        subCommand(StopCommand())
        subCommand(DemoCommand())
    }

    override fun onExecute(ctx: TaleContext) {
        ctx.reply("VideoMaps Commands:".info())
        ctx.reply("  /videomap create <id> <x> <z> <width> <height>".muted())
        ctx.reply("  /videomap remove <id>".muted())
        ctx.reply("  /videomap list".muted())
        ctx.reply("  /videomap join <id>".muted())
        ctx.reply("  /videomap leave <id>".muted())
        ctx.reply("  /videomap demo <id> <effect>".muted())
        ctx.reply("  /videomap stop <id>".muted())
    }

    class CreateCommand : TaleCommand("create", "Create a new map display") {
        private val idArg = stringArg("id", "Display ID")
        private val xArg = intArg("x", "Start chunk X")
        private val zArg = intArg("z", "Start chunk Z")
        private val widthArg = intArg("width", "Width in chunks")
        private val heightArg = intArg("height", "Height in chunks")

        override fun onExecute(ctx: TaleContext) {
            val id = ctx.get(idArg)
            val x = ctx.get(xArg)
            val z = ctx.get(zArg)
            val width = ctx.get(widthArg)
            val height = ctx.get(heightArg)

            if (MapDisplayManager.exists(id)) {
                ctx.reply("Display '$id' already exists!".error())
                return
            }

            val display = MapDisplayManager.create {
                this.id = id
                startChunkX = x
                startChunkZ = z
                widthChunks = width
                heightChunks = height
            }

            ctx.reply("Created display '$id' at ($x, $z) - ${display.pixelWidth}x${display.pixelHeight} pixels".success())
        }
    }

    class RemoveCommand : TaleCommand("remove", "Remove a map display") {
        private val idArg = stringArg("id", "Display ID")

        override fun onExecute(ctx: TaleContext) {
            val id = ctx.get(idArg)

            if (MapDisplayManager.remove(id) != null) {
                ctx.reply("Removed display '$id'".success())
            } else {
                ctx.reply("Display '$id' not found!".error())
            }
        }
    }

    class ListCommand : TaleCommand("list", "List all map displays") {
        override fun onExecute(ctx: TaleContext) {
            val displays = MapDisplayManager.getAll()

            if (displays.isEmpty()) {
                ctx.reply("No displays created.".muted())
                return
            }

            ctx.reply("Map Displays (${displays.size}):".info())
            for (display in displays) {
                val animating = if (MapDisplayManager.isAnimating(display.id)) " [PLAYING]" else ""
                ctx.reply("  ${display.id}: ${display.pixelWidth}x${display.pixelHeight}px, ${display.getViewers().size} viewers$animating".muted())
            }
        }
    }

    class JoinCommand : TaleCommand("join", "Join a display as viewer") {
        private val idArg = stringArg("id", "Display ID")

        override fun onExecute(ctx: TaleContext) {
            val player = ctx.requirePlayer() ?: return
            val id = ctx.get(idArg)

            if (MapDisplayManager.addViewer(id, player)) {
                ctx.reply("Joined display '$id'. Open your map (M) to see it!".success())
            } else {
                ctx.reply("Display '$id' not found!".error())
            }
        }
    }

    class LeaveCommand : TaleCommand("leave", "Leave a display") {
        private val idArg = stringArg("id", "Display ID")

        override fun onExecute(ctx: TaleContext) {
            val player = ctx.requirePlayer() ?: return
            val id = ctx.get(idArg)

            if (MapDisplayManager.removeViewer(id, player)) {
                ctx.reply("Left display '$id'".success())
            } else {
                ctx.reply("Display '$id' not found!".error())
            }
        }
    }

    class StopCommand : TaleCommand("stop", "Stop animation on a display") {
        private val idArg = stringArg("id", "Display ID")

        override fun onExecute(ctx: TaleContext) {
            val id = ctx.get(idArg)

            if (MapDisplayManager.stopAnimation(id)) {
                ctx.reply("Stopped animation on '$id'".success())
            } else {
                ctx.reply("No animation running on '$id' or display not found!".error())
            }
        }
    }

    class PlayCommand : TaleCommand("play", "Play an animation on a display") {
        private val idArg = stringArg("id", "Display ID")

        override fun onExecute(ctx: TaleContext) {
            ctx.reply("Use /videomap demo <id> <effect> to play built-in effects".info())
            ctx.reply("Effects: plasma, noise, colorCycle, checkerboard, stripes, ball".muted())
        }
    }

    class DemoCommand : TaleCommand("demo", "Play a demo animation") {
        private val idArg = stringArg("id", "Display ID")
        private val effectArg = optionalString("effect", "Effect name")

        override fun onExecute(ctx: TaleContext) {
            val id = ctx.get(idArg)
            val effect = ctx.get(effectArg) ?: "plasma"

            val display = MapDisplayManager.get(id)
            if (display == null) {
                ctx.reply("Display '$id' not found!".error())
                return
            }

            if (display.getViewers().isEmpty()) {
                ctx.reply("No viewers on '$id'. Use /videomap join $id first!".warning())
                return
            }

            val provider = when (effect.lowercase()) {
                "plasma" -> FrameProviders.plasma()
                "noise", "static" -> FrameProviders.noise()
                "colorcycle", "rainbow" -> FrameProviders.colorCycle()
                "checkerboard", "checker" -> FrameProviders.checkerboard()
                "stripes" -> FrameProviders.scrollingStripes()
                "ball", "bounce" -> FrameProviders.bouncingBall()
                "gradient" -> FrameProviders.horizontalGradient(MapColors.BLUE, MapColors.RED)
                else -> {
                    ctx.reply("Unknown effect '$effect'. Available: plasma, noise, colorCycle, checkerboard, stripes, ball, gradient".error())
                    return
                }
            }

            MapDisplayManager.startAnimation(id, provider, frameCount = 0, fps = 24, loop = true)
            ctx.reply("Playing '$effect' on display '$id' at 24 FPS".success())
        }
    }
}
