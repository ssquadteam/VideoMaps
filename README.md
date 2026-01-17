# VideoMaps

Play videos, animations, and custom graphics on the Hytale world map!

VideoMaps is a TaleLib-based plugin that allows you to stream pixel data directly to the world map, enabling video playback, animations, or custom map overlays.

## Features

- **Map Displays** - Define rectangular areas on the map for custom rendering
- **Pixel Streaming** - Send arbitrary pixel data to map chunks at up to 60 FPS
- **Built-in Effects** - Plasma, noise, color cycling, bouncing ball, and more
- **Multi-viewer Support** - Broadcast to multiple players simultaneously
- **Animation Controller** - Play, pause, stop, and seek through animations

## Quick Start

### In-Game Commands

```
# Create a 10x6 chunk display (320x192 pixels) at chunk position (0, 0)
/videomap create mydisplay 0 0 10 6

# Join the display as a viewer
/videomap join mydisplay

# Play a demo effect (open your map with M to see it!)
/videomap demo mydisplay plasma

# Stop the animation
/videomap stop mydisplay

# List all displays
/videomap list

# Remove the display
/videomap remove mydisplay

# Play the Video
/video frames mydisplay videoname --fps=30
```

### Available Demo Effects

| Effect | Description |
|--------|-------------|
| `plasma` | Psychedelic plasma waves |
| `noise` | Random static/noise |
| `colorCycle` | Smooth rainbow color cycling |
| `checkerboard` | Black and white checkerboard |
| `stripes` | Scrolling colored stripes |
| `ball` | Bouncing ball animation |
| `gradient` | Horizontal gradient |

## Programmatic Usage

### Creating a Display

```kotlin
import com.github.ssquadteam.videomaps.*

// Create a display using DSL
val display = MapDisplayManager.create {
    id = "my_screen"
    startChunkX = 0
    startChunkZ = 0
    widthChunks = 10   // 320 pixels wide
    heightChunks = 6   // 192 pixels tall
}

// Add a viewer
display.addViewer(player)
```

### Sending Custom Frames

```kotlin
// Create pixel data (ARGB format)
val pixels = IntArray(display.totalPixels) { index ->
    val x = index % display.pixelWidth
    val y = index / display.pixelWidth
    // Your pixel logic here
    MapColors.rgb(x % 256, y % 256, 128)
}

// Send to all viewers
display.sendFrame(pixels)

// Or fill with a solid color
display.fill(MapColors.RED)
```

### Playing Animations

```kotlin
// Use a built-in effect
MapDisplayManager.startAnimation(
    displayId = "my_screen",
    frameProvider = FrameProviders.plasma(),
    fps = 24,
    loop = true
)

// Or create your own frame provider
val customProvider = FrameProvider { frameIndex, width, height ->
    IntArray(width * height) { i ->
        // Generate pixel at index i for frame frameIndex
        val x = i % width
        val y = i / width
        // Your animation logic
        MapColors.rgb((x + frameIndex) % 256, y % 256, 128)
    }
}

MapDisplayManager.startAnimation("my_screen", customProvider, fps = 30)

// Control playback
MapDisplayManager.pauseAnimation("my_screen")
MapDisplayManager.resumeAnimation("my_screen")
MapDisplayManager.stopAnimation("my_screen")
```

### Custom Frame Provider for Video

```kotlin
// Example: Loading video frames (you'd need an external video decoder)
class VideoFrameProvider(private val videoDecoder: YourVideoDecoder) : FrameProvider {
    override fun getFrame(frameIndex: Int, width: Int, height: Int): IntArray? {
        val frame = videoDecoder.getFrame(frameIndex) ?: return null

        // Convert video frame to ARGB IntArray
        // Scale/crop to match width x height
        return convertFrameToPixels(frame, width, height)
    }
}

// Usage
val videoProvider = VideoFrameProvider(myDecoder)
MapDisplayManager.startAnimation("screen", videoProvider, frameCount = totalFrames, fps = 24)
```

## Resolution Guide

| Chunks | Pixels | Use Case |
|--------|--------|----------|
| 4x3 | 128x96 | Tiny icon/indicator |
| 8x6 | 256x192 | Small display |
| 10x6 | 320x192 | Standard display |
| 16x9 | 512x288 | Widescreen |
| 20x12 | 640x384 | Large display |
| 32x18 | 1024x576 | HD-ish |

**Note:** Larger displays = more bandwidth. A 320x192 display at 24 FPS sends ~7 MB/s uncompressed.

## Dependencies

- [TaleLib](../TaleLib) - Required

## Installation

1. Build the plugin: `./gradlew build`
2. Copy `build/libs/VideoMaps-1.0.0.jar` to your server's plugins folder
3. Ensure TaleLib is also installed
4. Restart the server

## How It Works

VideoMaps exploits Hytale's `UpdateWorldMap` packet system to send custom `MapChunk` data with arbitrary pixel arrays. Each chunk is 32x32 pixels. The plugin:

1. Creates `MapImage` objects with your pixel data
2. Wraps them in `MapChunk` objects with world coordinates
3. Sends `UpdateWorldMap` packets directly to player connections
4. Repeats at your desired frame rate

This effectively turns a portion of the world map into a programmable display!

## License

MIT License
