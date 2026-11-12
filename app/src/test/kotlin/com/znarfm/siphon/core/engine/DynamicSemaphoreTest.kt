package com.znarfm.siphon.core.engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class DynamicSemaphoreTest {

    @Test
    fun testAcquireAndRelease() = runBlocking {
        val sem = DynamicSemaphore(2)

        var entered1 = false
        var entered2 = false

        sem.withPermit {
            entered1 = true
        }
        sem.withPermit {
            entered2 = true
        }

        assertTrue(entered1)
        assertTrue(entered2)
    }

    @Test
    fun testPermitsExhaustionAndRelease() = runBlocking {
        val sem = DynamicSemaphore(1)
        val activeCount = AtomicInteger(0)
        val maxSimultaneous = AtomicInteger(0)

        val j1 = launch {
            sem.withPermit {
                val cur = activeCount.incrementAndGet()
                maxSimultaneous.updateAndGet { maxOf(it, cur) }
                delay(100)
                activeCount.decrementAndGet()
            }
        }

        val j2 = launch {
            sem.withPermit {
                val cur = activeCount.incrementAndGet()
                maxSimultaneous.updateAndGet { maxOf(it, cur) }
                delay(100)
                activeCount.decrementAndGet()
            }
        }

        j1.join()
        j2.join()

        assertEquals(1, maxSimultaneous.get())
        assertEquals(0, activeCount.get())
    }

    @Test
    fun testUpdateMaxPermitsWakesUpWaiters() = runBlocking {
        val sem = DynamicSemaphore(1)
        val job2Entered = AtomicBoolean(false)

        val j1 = launch {
            sem.withPermit {
                // Hold permit 1
                delay(200)
            }
        }

        delay(30) // Wait for j1 to acquire

        val j2 = launch {
            sem.withPermit {
                job2Entered.set(true)
            }
        }

        delay(30)
        // j2 should be waiting because initial permit was 1
        assertFalse(job2Entered.get())

        // Scale max permits from 1 to 2
        sem.updateMaxPermits(2)

        delay(30)
        // j2 should have been woken up immediately and entered!
        assertTrue(job2Entered.get())

        j1.join()
        j2.join()
    }

    @Test
    fun testCancellationRemovesWaiterCleanly() = runBlocking {
        val sem = DynamicSemaphore(1)

        val holder = launch {
            sem.withPermit {
                delay(300)
            }
        }

        delay(30)

        val waiter = launch {
            sem.withPermit {
                // Should not reach here
            }
        }

        delay(30)
        waiter.cancel() // Cancel waiter before holder finishes
        waiter.join()

        holder.join()

        // Verify next acquire succeeds without being blocked by dead waiter
        var nextEntered = false
        sem.withPermit {
            nextEntered = true
        }
        assertTrue(nextEntered)
    }
}
