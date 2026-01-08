package com.netconditioner.vpn

import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min

class TokenBucket(
    private var capacityBytes: Int = 64 * 1024,
) {
    private var tokens: Double = capacityBytes.toDouble()
    private var lastRefillTimeMs: Long = System.currentTimeMillis()
    private var rateBytesPerSec: Double = 1024.0

    fun updateRate(kbps: Int) {
        rateBytesPerSec = max(1.0, kbps.toDouble() * 1024 / 8)
    }

    suspend fun consumeAndDelay(bytes: Int): Long {
        refill()
        if (tokens >= bytes) {
            tokens -= bytes
            return 0
        }
        val missing = bytes - tokens
        val waitTimeMs = (missing / rateBytesPerSec * 1000).toLong()
        tokens = 0.0
        if (waitTimeMs > 0) {
            delay(waitTimeMs)
        }
        return waitTimeMs
    }

    private fun refill() {
        val now = System.currentTimeMillis()
        val elapsedMs = now - lastRefillTimeMs
        if (elapsedMs <= 0) return

        val refillTokens = elapsedMs / 1000.0 * rateBytesPerSec
        tokens = min(capacityBytes.toDouble(), tokens + refillTokens)
        lastRefillTimeMs = now
    }
}
