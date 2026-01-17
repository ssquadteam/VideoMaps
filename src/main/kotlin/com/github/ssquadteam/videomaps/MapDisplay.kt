package com.github.ssquadteam.videomaps

import com.hypixel.hytale.protocol.packets.worldmap.MapChunk
import com.hypixel.hytale.protocol.packets.worldmap.MapImage
import com.hypixel.hytale.protocol.packets.worldmap.UpdateWorldMap
import com.hypixel.hytale.server.core.entity.entities.Player

class MapDisplay(
    val id: String,
    val startChunkX: Int,
    val startChunkZ: Int,
    val widthChunks: Int,
    val heightChunks: Int
) {
    val pixelWidth: Int = widthChunks * CHUNK_SIZE
    val pixelHeight: Int = heightChunks * CHUNK_SIZE
    val totalPixels: Int = pixelWidth * pixelHeight
    private val viewers = mutableSetOf<Player>()

    companion object {
        const val CHUNK_SIZE = 32
        const val IMAGE_SCALE = 3
        const val IMAGE_SIZE = 96
    }

    fun addViewer(player: Player) = viewers.add(player)
    fun removeViewer(player: Player) = viewers.remove(player)
    fun getViewers(): Set<Player> = viewers.toSet()
    fun hasViewer(player: Player): Boolean = player in viewers
    fun clearViewers() = viewers.clear()

    fun sendFrame(framePixels: IntArray) {
        require(framePixels.size == totalPixels) {
            "Frame size mismatch: expected $totalPixels pixels, got ${framePixels.size}"
        }

        val chunks = buildChunks(framePixels)
        val packet = UpdateWorldMap(chunks, null, null)

        for (viewer in viewers) {
            viewer.playerRef?.packetHandler?.write(packet)
        }
    }

    fun sendFrameTo(player: Player, framePixels: IntArray) {
        require(framePixels.size == totalPixels) {
            "Frame size mismatch: expected $totalPixels pixels, got ${framePixels.size}"
        }

        val chunks = buildChunks(framePixels)
        val packet = UpdateWorldMap(chunks, null, null)
        player.playerRef?.packetHandler?.write(packet)
    }

    fun fill(color: Int) {
        val pixels = IntArray(totalPixels) { color }
        sendFrame(pixels)
    }

    fun clear() {
        fill(0x00000000)
    }

    private fun buildChunks(framePixels: IntArray): Array<MapChunk> {
        val chunks = mutableListOf<MapChunk>()

        for (cz in 0 until heightChunks) {
            for (cx in 0 until widthChunks) {
                val chunkPixels = extractChunkPixelsScaled(framePixels, cx, cz)
                val image = MapImage(IMAGE_SIZE, IMAGE_SIZE, chunkPixels)
                chunks.add(MapChunk(startChunkX + cx, startChunkZ + cz, image))
            }
        }

        return chunks.toTypedArray()
    }

    private fun extractChunkPixelsScaled(framePixels: IntArray, chunkX: Int, chunkZ: Int): IntArray {
        val chunk = IntArray(IMAGE_SIZE * IMAGE_SIZE)
        val startX = chunkX * CHUNK_SIZE
        val startZ = chunkZ * CHUNK_SIZE

        for (z in 0 until IMAGE_SIZE) {
            for (x in 0 until IMAGE_SIZE) {
                val srcX = (startX + x / IMAGE_SCALE).coerceIn(0, pixelWidth - 1)
                val srcZ = (startZ + z / IMAGE_SCALE).coerceIn(0, pixelHeight - 1)
                val srcIndex = srcZ * pixelWidth + srcX
                val dstIndex = z * IMAGE_SIZE + x
                chunk[dstIndex] = framePixels[srcIndex]
            }
        }

        return chunk
    }

    private fun extractChunkPixels(framePixels: IntArray, chunkX: Int, chunkZ: Int): IntArray {
        val chunk = IntArray(CHUNK_SIZE * CHUNK_SIZE)
        val startX = chunkX * CHUNK_SIZE
        val startZ = chunkZ * CHUNK_SIZE

        for (z in 0 until CHUNK_SIZE) {
            for (x in 0 until CHUNK_SIZE) {
                val srcX = startX + x
                val srcZ = startZ + z
                val srcIndex = srcZ * pixelWidth + srcX
                val dstIndex = z * CHUNK_SIZE + x
                chunk[dstIndex] = framePixels[srcIndex]
            }
        }

        return chunk
    }

    override fun toString(): String {
        return "MapDisplay(id='$id', chunks=${widthChunks}x${heightChunks}, " +
                "pixels=${pixelWidth}x${pixelHeight}, viewers=${viewers.size})"
    }
}

class MapDisplayBuilder {
    var id: String = "display_${System.currentTimeMillis()}"
    var startChunkX: Int = 0
    var startChunkZ: Int = 0
    var widthChunks: Int = 10
    var heightChunks: Int = 6

    fun size(width: Int, height: Int) = apply {
        this.widthChunks = width
        this.heightChunks = height
    }

    fun position(chunkX: Int, chunkZ: Int) = apply {
        this.startChunkX = chunkX
        this.startChunkZ = chunkZ
    }

    fun pixelSize(width: Int, height: Int) = apply {
        this.widthChunks = (width + 31) / 32
        this.heightChunks = (height + 31) / 32
    }

    fun build() = MapDisplay(id, startChunkX, startChunkZ, widthChunks, heightChunks)
}

fun mapDisplay(block: MapDisplayBuilder.() -> Unit) = MapDisplayBuilder().apply(block).build()
