package com.siphon.app.core.network

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min

/**
 * Thread-safe token bucket rate limiter for bytes per second.
 * [bytesPerSecond] <= 0 means unthrottled.
 */
class TokenBucket(bytesPerSecond: Long = 0L) {
    @Volatile
    var rateBytesPerSec: Long = bytesPerSecond.coerceAtLeast(0L)
        private set

    private val availableTokens = AtomicLong(0L)
    private val lastRefillTimeNanos = AtomicLong(System.nanoTime())

    fun updateRate(bytesPerSecond: Long) {
        rateBytesPerSec = bytesPerSecond.coerceAtLeast(0L)
        availableTokens.set(rateBytesPerSec)
        lastRefillTimeNanos.set(System.nanoTime())
    }

    suspend fun consume(bytes: Int) {
        val rate = rateBytesPerSec
        if (rate <= 0L || bytes <= 0) return

        while (true) {
            refill(rate)
            val current = availableTokens.get()
            if (current >= bytes) {
                if (availableTokens.compareAndSet(current, current - bytes)) {
                    return
                }
            } else {
                val needed = bytes - current
                val waitNanos = (needed * 1_000_000_000L) / rate
                val waitMs = (waitNanos / 1_000_000L).coerceIn(10L, 500L)
                delay(waitMs)
            }
        }
    }

    private fun refill(rate: Long) {
        val now = System.nanoTime()
        val last = lastRefillTimeNanos.get()
        val elapsedNanos = now - last
        if (elapsedNanos <= 0) return

        if (lastRefillTimeNanos.compareAndSet(last, now)) {
            val newTokens = (elapsedNanos * rate) / 1_000_000_000L
            if (newTokens > 0) {
                availableTokens.updateAndGet { cur ->
                    min(rate, cur + newTokens)
                }
            }
        }
    }
}
