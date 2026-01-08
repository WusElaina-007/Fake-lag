package com.netconditioner.vpn

import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue

class ByteBufferPool(
    private val bufferSize: Int,
    private val maxPoolSize: Int,
) {
    private val pool = ConcurrentLinkedQueue<ByteBuffer>()

    fun acquire(): ByteBuffer {
        val buffer = pool.poll() ?: ByteBuffer.allocateDirect(bufferSize)
        buffer.clear()
        return buffer
    }

    fun release(buffer: ByteBuffer) {
        if (pool.size < maxPoolSize) {
            pool.offer(buffer)
        }
    }
}
