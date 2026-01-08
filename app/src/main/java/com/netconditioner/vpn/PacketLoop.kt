package com.netconditioner.vpn

import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.random.Random

class PacketLoop(
    private val configFlow: ConfigSnapshot,
    private val metricsCollector: MetricsCollector,
) {
    private val pool = ByteBufferPool(bufferSize = 32 * 1024, maxPoolSize = 128)
    private val flowTable = FlowTable()
    private val uploadBucket = TokenBucket()
    private val downloadBucket = TokenBucket()

    fun start(scope: CoroutineScope, tunInterface: ParcelFileDescriptor) {
        val channel = Channel<ByteBuffer>(capacity = Channel.BUFFERED)

        scope.launch(Dispatchers.IO) {
            val input = FileInputStream(tunInterface.fileDescriptor)
            while (isActive) {
                val buffer = pool.acquire()
                val read = input.channel.read(buffer)
                if (read <= 0) {
                    pool.release(buffer)
                    continue
                }
                buffer.flip()
                metricsCollector.recordIn(read)
                channel.trySend(buffer)
            }
        }

        scope.launch(Dispatchers.IO) {
            for (buffer in channel) {
                val config = configFlow.current()
                val delayMs = calculateDelay(config)
                val jitterMs = (delayMs - config.latencyMs).toLong()

                if (shouldDrop(config.lossPercent)) {
                    metricsCollector.recordDrop()
                    pool.release(buffer)
                    continue
                }

                applyThrottle(config, buffer.remaining())
                delay(delayMs)

                // TODO: Implement TCP/UDP relay using non-blocking sockets and a flow table.
                // Parsing flow key now keeps pipeline ready for extension.
                parseFlowKey(buffer)?.let { flowTable.getOrCreate(it) }

                metricsCollector.recordLatency(delayMs, jitterMs)
                metricsCollector.recordOut(buffer.remaining())
                pool.release(buffer)
            }
        }

        scope.launch {
            while (isActive) {
                delay(5_000)
                flowTable.sweep()
            }
        }
    }

    private fun calculateDelay(config: UltraConfig): Long {
        val jitterRange = -config.jitterMs..config.jitterMs
        val jitter = jitterRange.random()
        return max(0, config.latencyMs + jitter).toLong()
    }

    private fun shouldDrop(lossPercent: Int): Boolean {
        return Random.nextInt(0, 100) < lossPercent
    }

    private suspend fun applyThrottle(config: UltraConfig, packetBytes: Int) {
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

class ConfigSnapshot(private val stateProvider: () -> UltraConfig) {
    fun current(): UltraConfig = stateProvider()
}

private fun parseFlowKey(buffer: ByteBuffer): FlowKey? {
    if (buffer.remaining() < 20) return null
    val position = buffer.position()
    val versionIhl = buffer.get(position).toInt()
    val version = versionIhl shr 4
    if (version != 4) return null

    val protocol = buffer.get(position + 9).toInt() and 0xFF
    val srcIp = listOf(12, 13, 14, 15).joinToString(".") { (buffer.get(position + it).toInt() and 0xFF).toString() }
    val dstIp = listOf(16, 17, 18, 19).joinToString(".") { (buffer.get(position + it).toInt() and 0xFF).toString() }

    val ihl = (versionIhl and 0x0F) * 4
    if (buffer.remaining() < ihl + 4) return null
    val srcPort = buffer.getShort(position + ihl).toInt() and 0xFFFF
    val dstPort = buffer.getShort(position + ihl + 2).toInt() and 0xFFFF

    return FlowKey(srcIp, dstIp, srcPort, dstPort, protocol)
}
