package com.github.ssquadteam.videomaps

import com.github.ssquadteam.talelib.worldmap.MapColors
import kotlin.math.sin

object FrameProviders {

    fun solidColor(color: Int): FrameProvider = FrameProvider { _, width, height ->
        IntArray(width * height) { color }
    }

    fun colorCycle(speed: Float = 0.1f): FrameProvider = FrameProvider { frame, width, height ->
        val hue = (frame * speed) % 360f
        val color = hsvToRgb(hue, 1f, 1f)
        IntArray(width * height) { color }
    }

    fun horizontalGradient(color1: Int, color2: Int): FrameProvider = FrameProvider { _, width, height ->
        IntArray(width * height) { i ->
            val x = i % width
            val t = x.toFloat() / width
            lerpColor(color1, color2, t)
        }
    }

    fun verticalGradient(color1: Int, color2: Int): FrameProvider = FrameProvider { _, width, height ->
        IntArray(width * height) { i ->
            val y = i / width
            val t = y.toFloat() / height
            lerpColor(color1, color2, t)
        }
    }

    fun plasma(speed: Float = 0.05f): FrameProvider = FrameProvider { frame, width, height ->
        val time = frame * speed
        IntArray(width * height) { i ->
            val x = i % width
            val y = i / width
            val fx = x.toFloat() / width
            val fy = y.toFloat() / height

            val v1 = sin(fx * 10 + time)
            val v2 = sin(10 * (fx * sin(time / 2) + fy * kotlin.math.cos(time / 3)) + time)
            val v3 = sin(kotlin.math.sqrt((fx - 0.5).toDouble() * (fx - 0.5) + (fy - 0.5) * (fy - 0.5)) * 10 + time)
            val v = (v1 + v2 + v3) / 3

            val hue = ((v + 1) * 180).toFloat()
            hsvToRgb(hue, 1f, 1f)
        }
    }

    fun checkerboard(
        color1: Int = MapColors.BLACK,
        color2: Int = MapColors.WHITE,
        squareSize: Int = 4
    ): FrameProvider = FrameProvider { _, width, height ->
        IntArray(width * height) { i ->
            val x = i % width
            val y = i / width
            val cx = x / squareSize
            val cy = y / squareSize
            if ((cx + cy) % 2 == 0) color1 else color2
        }
    }

    fun scrollingStripes(
        color1: Int = MapColors.RED,
        color2: Int = MapColors.BLUE,
        stripeWidth: Int = 8,
        speed: Int = 1
    ): FrameProvider = FrameProvider { frame, width, height ->
        val offset = (frame * speed) % (stripeWidth * 2)
        IntArray(width * height) { i ->
            val x = (i % width + offset) % (stripeWidth * 2)
            if (x < stripeWidth) color1 else color2
        }
    }

    fun noise(): FrameProvider = FrameProvider { _, width, height ->
        IntArray(width * height) {
            val gray = (Math.random() * 255).toInt()
            MapColors.rgb(gray, gray, gray)
        }
    }

    fun bouncingBall(
        ballColor: Int = MapColors.RED,
        bgColor: Int = MapColors.BLACK,
        ballRadius: Int = 5
    ): FrameProvider {
        var posX = 0.0
        var posY = 0.0
        var velX = 2.0
        var velY = 1.5

        return FrameProvider { _, width, height ->
            // Update position
            posX += velX
            posY += velY

            // Bounce off walls
            if (posX <= ballRadius || posX >= width - ballRadius) velX = -velX
            if (posY <= ballRadius || posY >= height - ballRadius) velY = -velY

            posX = posX.coerceIn(ballRadius.toDouble(), (width - ballRadius).toDouble())
            posY = posY.coerceIn(ballRadius.toDouble(), (height - ballRadius).toDouble())

            // Render
            IntArray(width * height) { i ->
                val x = i % width
                val y = i / width
                val dx = x - posX
                val dy = y - posY
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                if (dist <= ballRadius) ballColor else bgColor
            }
        }
    }

    fun fromFrames(frames: List<IntArray>): FrameProvider = FrameProvider { frameIndex, _, _ ->
        if (frames.isEmpty()) null
        else frames[frameIndex % frames.size]
    }

    private fun hsvToRgb(h: Float, s: Float, v: Float): Int {
        val c = v * s
        val x = c * (1 - kotlin.math.abs((h / 60) % 2 - 1))
        val m = v - c

        val (r1, g1, b1) = when {
            h < 60 -> Triple(c, x, 0f)
            h < 120 -> Triple(x, c, 0f)
            h < 180 -> Triple(0f, c, x)
            h < 240 -> Triple(0f, x, c)
            h < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return MapColors.rgb(
            ((r1 + m) * 255).toInt(),
            ((g1 + m) * 255).toInt(),
            ((b1 + m) * 255).toInt()
        )
    }

    private fun lerpColor(c1: Int, c2: Int, t: Float): Int {
        val a1 = MapColors.alpha(c1)
        val r1 = MapColors.red(c1)
        val g1 = MapColors.green(c1)
        val b1 = MapColors.blue(c1)

        val a2 = MapColors.alpha(c2)
        val r2 = MapColors.red(c2)
        val g2 = MapColors.green(c2)
        val b2 = MapColors.blue(c2)

        return MapColors.rgba(
            (r1 + (r2 - r1) * t).toInt(),
            (g1 + (g2 - g1) * t).toInt(),
            (b1 + (b2 - b1) * t).toInt(),
            (a1 + (a2 - a1) * t).toInt()
        )
    }
}
