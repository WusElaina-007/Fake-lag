package com.netconditioner.vpn

import java.util.LinkedHashMap

class FlowTable(
    private val maxSize: Int = 1024,
    private val idleTimeoutMs: Long = 60_000,
) {
    private val table = object : LinkedHashMap<FlowKey, FlowEntry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<FlowKey, FlowEntry>?): Boolean {
            return size > maxSize
        }
    }

    @Synchronized
    fun getOrCreate(key: FlowKey): FlowEntry {
        val now = System.currentTimeMillis()
        val entry = table[key] ?: FlowEntry(key, now).also { table[key] = it }
        entry.lastSeen = now
        return entry
    }

    @Synchronized
    fun sweep() {
        val now = System.currentTimeMillis()
        val iterator = table.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next().value
            if (now - entry.lastSeen > idleTimeoutMs) {
                iterator.remove()
            }
        }
    }
}

data class FlowEntry(
    val key: FlowKey,
    var lastSeen: Long,
)

data class FlowKey(
    val srcIp: String,
    val dstIp: String,
    val srcPort: Int,
    val dstPort: Int,
    val protocol: Int,
)
