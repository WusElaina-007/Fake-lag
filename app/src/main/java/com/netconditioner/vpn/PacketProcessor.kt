package com.netconditioner.vpn

import android.os.ParcelFileDescriptor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.random.Random

class PacketProcessor(
    private val configFlow: StateFlow<NetworkConfig>,
) {
    private val uploadBucket = TokenBucket()
    private val downloadBucket = TokenBucket()

    suspend fun start(tunInterface: ParcelFileDescriptor) {
        val input = FileInputStream(tunInterface.fileDescriptor)
        val buffer = ByteArray(32 * 1024)

        while (true) {
            val length = input.read(buffer)
            if (length <= 0) continue

            val config = configFlow.value
            val delayMs = calculateDelay(config)

            if (shouldDrop(config.packetLossPercent)) {
                continue
            }

            applyThrottle(config, length)
            delay(delayMs)

            // TODO: Implement full TCP/UDP forwarding via user-space sockets.
            // This sample focuses on conditioning logic and VPN scaffolding.
        }
    }

    private fun calculateDelay(config: NetworkConfig): Long {
        val jitterRange = -config.jitterMs..config.jitterMs
        val jitter = jitterRange.random()
        return max(0, config.baseLatencyMs + jitter).toLong()
    }

    private fun shouldDrop(lossPercent: Int): Boolean {
        return Random.nextInt(0, 100) < lossPercent
    }

    private suspend fun applyThrottle(config: NetworkConfig, packetBytes: Int) {
        uploadBucket.updateRate(config.uploadKbps)
        downloadBucket.updateRate(config.downloadKbps)

        val uploadDelay = uploadBucket.consumeAndDelay(packetBytes)
        val downloadDelay = downloadBucket.consumeAndDelay(packetBytes)
        val maxDelay = max(uploadDelay, downloadDelay)
        if (maxDelay > 0) {
            delay(maxDelay)
        }
    }
}

// 5-tuple flow table helper for future extensions.
data class FlowKey(
    val srcIp: String,
    val dstIp: String,
    val srcPort: Int,
    val dstPort: Int,
    val protocol: Int,
)

fun parseFlowKey(packet: ByteArray, length: Int): FlowKey? {
    if (length < 20) return null
    val buffer = ByteBuffer.wrap(packet, 0, length).order(ByteOrder.BIG_ENDIAN)
    val versionIhl = buffer.get(0).toInt()
    val version = versionIhl shr 4
    if (version != 4) return null

    val protocol = buffer.get(9).toInt() and 0xFF
    val srcIp = listOf(12, 13, 14, 15).joinToString(".") { (buffer.get(it).toInt() and 0xFF).toString() }
    val dstIp = listOf(16, 17, 18, 19).joinToString(".") { (buffer.get(it).toInt() and 0xFF).toString() }

    val ihl = (versionIhl and 0x0F) * 4
    if (length < ihl + 4) return null
    buffer.position(ihl)
    val srcPort = buffer.short.toInt() and 0xFFFF
    val dstPort = buffer.short.toInt() and 0xFFFF

    return FlowKey(srcIp, dstIp, srcPort, dstPort, protocol)
}
