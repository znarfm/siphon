package com.znarfm.siphon.core.hls

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer

class HlsQueueEngineTest {

    @Test
    fun testExplicitHexIvParsing() {
        val explicitHex = "0x0123456789abcdef0123456789abcdef"
        val iv = HlsQueueEngine.parseOrGenerateIv(explicitHex, 42L)

        val expected = byteArrayOf(
            0x01, 0x23, 0x45, 0x67, 0x89.toByte(), 0xab.toByte(), 0xcd.toByte(), 0xef.toByte(),
            0x01, 0x23, 0x45, 0x67, 0x89.toByte(), 0xab.toByte(), 0xcd.toByte(), 0xef.toByte()
        )
        assertArrayEquals(expected, iv)
    }

    @Test
    fun testShortHexIvPaddedTo16Bytes() {
        // A short hex value like 0x1 or 0x01 must be padded from the left to 16 bytes
        val iv = HlsQueueEngine.parseOrGenerateIv("0x1", 0L)
        val expected = ByteArray(16).apply { this[15] = 0x01 }
        assertArrayEquals(expected, iv)
    }

    @Test
    fun testRfc8216SequenceNumberBigEndianIvFallback() {
        // When IV is null, sequence number must be placed in the bottom 8 bytes of 16-byte big-endian IV
        val iv0 = HlsQueueEngine.parseOrGenerateIv(null, 0L)
        assertArrayEquals(ByteArray(16), iv0)

        val iv1 = HlsQueueEngine.parseOrGenerateIv(null, 1L)
        val expected1 = ByteBuffer.allocate(16).putLong(8, 1L).array()
        assertArrayEquals(expected1, iv1)
        assertEquals(1.toByte(), iv1[15])
        assertEquals(0.toByte(), iv1[0])

        val ivSeq = HlsQueueEngine.parseOrGenerateIv(null, 0x1234567890ABCDEFL)
        val expectedSeq = ByteBuffer.allocate(16).putLong(8, 0x1234567890ABCDEFL).array()
        assertArrayEquals(expectedSeq, ivSeq)
    }

    @Test
    fun testHttpStatusException403And410() {
        val ex403 = com.znarfm.siphon.core.network.HttpStatusException(403, "Forbidden")
        assertEquals(403, ex403.code)
        val ex410 = com.znarfm.siphon.core.network.HttpStatusException(410, "Gone")
        assertEquals(410, ex410.code)
    }

    @Test
    fun testFindHttpStatusExceptionDeepHierarchy() {
        val ex403 = com.znarfm.siphon.core.network.HttpStatusException(403, "Forbidden")
        val direct = HlsQueueEngine.findHttpStatusException(ex403)
        assertEquals(403, direct?.code)

        val wrappedOnce = java.io.IOException("Network error", ex403)
        val unwrapped1 = HlsQueueEngine.findHttpStatusException(wrappedOnce)
        assertEquals(403, unwrapped1?.code)

        val wrappedTwice = RuntimeException("Wrapper", java.util.concurrent.ExecutionException(ex403))
        val unwrapped2 = HlsQueueEngine.findHttpStatusException(wrappedTwice)
        assertEquals(403, unwrapped2?.code)

        val noHttp = RuntimeException("Other error", java.io.IOException("Disk full"))
        val unwrappedNone = HlsQueueEngine.findHttpStatusException(noHttp)
        org.junit.Assert.assertNull(unwrappedNone)
    }
}
