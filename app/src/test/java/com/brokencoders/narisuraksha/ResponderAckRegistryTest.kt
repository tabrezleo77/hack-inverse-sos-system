package com.brokencoders.narisuraksha

import com.brokencoders.narisuraksha.ble.ResponderAckRegistry
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the process-wide ResponderAckRegistry that survives activity
 * recreation: distinct responders increment the count, duplicates are
 * idempotent, and clear() resets for a new SOS epoch.
 */
class ResponderAckRegistryTest {

    @Before
    fun setUp() {
        // The registry is a process-wide singleton shared with MeshCore (which
        // records ACKs during its own tests). Clear before each test so state
        // from other test classes cannot leak in.
        ResponderAckRegistry.clear()
    }

    @After
    fun tearDown() {
        ResponderAckRegistry.clear()
    }

    @Test
    fun testSingleResponderCountedOnce() {
        assertTrue(ResponderAckRegistry.recordAck(101))
        assertEquals(1, ResponderAckRegistry.responderCount)
    }

    @Test
    fun testDuplicateAckIdempotent() {
        assertTrue(ResponderAckRegistry.recordAck(101))
        // Duplicate burst from the same responder must NOT inflate the count
        assertFalse(ResponderAckRegistry.recordAck(101))
        assertFalse(ResponderAckRegistry.recordAck(101))
        assertEquals(1, ResponderAckRegistry.responderCount)
    }

    @Test
    fun testMultipleDistinctRespondersAccumulate() {
        assertTrue(ResponderAckRegistry.recordAck(101))
        assertTrue(ResponderAckRegistry.recordAck(202))
        assertTrue(ResponderAckRegistry.recordAck((-303).toShort()))
        assertEquals(3, ResponderAckRegistry.responderCount)
        assertEquals(setOf(101.toShort(), 202.toShort(), (-303).toShort()), ResponderAckRegistry.responderIds)
    }

    @Test
    fun testClearResetsForNewSosEpoch() {
        ResponderAckRegistry.recordAck(101)
        ResponderAckRegistry.recordAck(202)
        assertEquals(2, ResponderAckRegistry.responderCount)

        ResponderAckRegistry.clear()
        assertEquals(0, ResponderAckRegistry.responderCount)
        assertTrue(ResponderAckRegistry.responderIds.isEmpty())

        // After clear, the same responder counts again (new SOS epoch)
        assertTrue(ResponderAckRegistry.recordAck(101))
        assertEquals(1, ResponderAckRegistry.responderCount)
    }

    @Test
    fun testSurvivesAcrossClearBoundaries() {
        // Simulates activity recreation: the registry is a process-wide singleton,
        // so a "new" consumer reading responderCount sees the same durable state.
        ResponderAckRegistry.recordAck(404)
        assertEquals(1, ResponderAckRegistry.responderCount)

        // A duplicate read (new ViewModel instance) is still idempotent
        assertFalse(ResponderAckRegistry.recordAck(404))
        assertEquals(1, ResponderAckRegistry.responderCount)
    }
}
