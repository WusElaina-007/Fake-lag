package com.netconditioner.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicLong

class MetricsCollector {
    private val packetsIn = AtomicLong(0)
    private val packetsOut = AtomicLong(0)
    private val packetsDropped = AtomicLong(0)
    private val latencySum = AtomicLong(0)
    private val jitterSum = AtomicLong(0)
    private val uploadBytes = AtomicLong(0)
    private val downloadBytes = AtomicLong(0)

    private val _snapshot = MutableStateFlow(MetricsSnapshot())
    val snapshot: StateFlow<MetricsSnapshot> = _snapshot

    fun recordIn(bytes: Int) {
        packetsIn.incrementAndGet()
        downloadBytes.addAndGet(bytes.toLong())
    }

    fun recordOut(bytes: Int) {
        packetsOut.incrementAndGet()
        uploadBytes.addAndGet(bytes.toLong())
    }

    fun recordDrop() {
        packetsDropped.incrementAndGet()
    }

    fun recordLatency(latencyMs: Long, jitterMs: Long) {
        latencySum.addAndGet(latencyMs)
        jitterSum.addAndGet(jitterMs)
    }

    fun snapshotAndReset(intervalMs: Long) {
        val inPackets = packetsIn.getAndSet(0)
        val outPackets = packetsOut.getAndSet(0)
        val dropped = packetsDropped.getAndSet(0)
        val latencyTotal = latencySum.getAndSet(0)
        val jitterTotal = jitterSum.getAndSet(0)
        val upBytes = uploadBytes.getAndSet(0)
        val downBytes = downloadBytes.getAndSet(0)

        val latencyAvg = if (inPackets + outPackets > 0) {
            latencyTotal / maxOf(1, inPackets + outPackets)
        } else {
            0
        }
        val jitterAvg = if (inPackets + outPackets > 0) {
            jitterTotal / maxOf(1, inPackets + outPackets)
        } else {
            0
        }

        val intervalSeconds = intervalMs.coerceAtLeast(1) / 1000f
        val uploadKbps = (upBytes * 8 / 1000f / intervalSeconds).toInt()
        val downloadKbps = (downBytes * 8 / 1000f / intervalSeconds).toInt()

        _snapshot.value = MetricsSnapshot(
            packetsIn = inPackets,
            packetsOut = outPackets,
            packetsDropped = dropped,
            avgLatencyMs = latencyAvg,
            avgJitterMs = jitterAvg,
            uploadKbps = uploadKbps,
            downloadKbps = downloadKbps,
        )
    }
}

data class MetricsSnapshot(
    val packetsIn: Long = 0,
    val packetsOut: Long = 0,
    val packetsDropped: Long = 0,
    val avgLatencyMs: Long = 0,
    val avgJitterMs: Long = 0,
    val uploadKbps: Int = 0,
    val downloadKbps: Int = 0,
)

object MetricsBus {
    private val _shared = MutableStateFlow(MetricsSnapshot())
    val shared: StateFlow<MetricsSnapshot> = _shared

    fun publish(snapshot: MetricsSnapshot) {
        _shared.value = snapshot
    }
}
