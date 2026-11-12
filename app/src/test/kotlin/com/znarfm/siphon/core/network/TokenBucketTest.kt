package com.znarfm.siphon.core.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TokenBucketTest {

    @Test
    fun testUnthrottledWhenRateZero() = runBlocking {
        val bucket = TokenBucket(0L)
        assertEquals(0L, bucket.rateBytesPerSec)
        // Should return immediately without blocking
        bucket.consume(1024 * 1024)
    }

    @Test
    fun testFrequentSmallConsumptionsDoNotEvaporateTokens() = runBlocking {
        // 100 KB/s rate limit
        val bucket = TokenBucket(100_000L)

        // Initial burst: bucket should have 100,000 tokens available
        // Consume 50 KB in 50 small 1KB chunks
        for (i in 0 until 50) {
            bucket.consume(1024)
        }
        // Successfully consumed 50KB without stalling
    }

    @Test
    fun testUpdateRateLimit() = runBlocking {
        val bucket = TokenBucket(50_000L)
        assertEquals(50_000L, bucket.rateBytesPerSec)

        bucket.updateRate(200_000L)
        assertEquals(200_000L, bucket.rateBytesPerSec)

        bucket.updateRate(-10L)
        assertEquals(0L, bucket.rateBytesPerSec)
    }
}
