package com.brokencoders.narisuraksha

import com.brokencoders.narisuraksha.ble.AdvPriority
import com.brokencoders.narisuraksha.ble.BleAdvertiser
import com.brokencoders.narisuraksha.ble.BleDiagnosticsTracker
import com.brokencoders.narisuraksha.ble.HardwareAdvertiser
import com.brokencoders.narisuraksha.core.Constants
import com.brokencoders.narisuraksha.core.SosPacket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BleAdvertiserQueueTest {

    private class FakeHardwareAdvertiser : HardwareAdvertiser {
        var activePacket: SosPacket? = null
        var activePriority: AdvPriority? = null
        var startCount = 0
        var stopCount = 0
        var isAdvertisingActive = false
        var lastDurationMs: Long = 0L

        var nextErrorCode: Int? = null

        override fun startAdvertising(
            packet: SosPacket,
            priority: AdvPriority,
            durationMs: Long,
            onSuccess: () -> Unit,
            onFailure: (errorCode: Int) -> Unit
        ) {
            startCount++
            lastDurationMs = durationMs
            val err = nextErrorCode
            if (err != null) {
                nextErrorCode = null
                onFailure(err)
            } else {
                isAdvertisingActive = true
                activePacket = packet
                activePriority = priority
                onSuccess()
            }
        }

        override fun stopAdvertising() {
            stopCount++
            isAdvertisingActive = false
            activePacket = null
            activePriority = null
        }
    }

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var fakeHardware: FakeHardwareAdvertiser
    private lateinit var advertiser: BleAdvertiser

    private fun makePacket(
        senderId: Short,
        timestamp: Int = 1000,
        lat: Float = 12.9716f,
        lon: Float = 77.5946f,
        flags: Byte = SosPacket.FLAG_SOS.toByte(),
        ttl: Int = 4
    ): SosPacket = SosPacket(
        senderId = senderId,
        timestamp = timestamp,
        lat = lat,
        lon = lon,
        flags = flags,
        ttl = ttl
    )

    @Before
    fun setUp() {
        BleDiagnosticsTracker.clearLogs()
        fakeHardware = FakeHardwareAdvertiser()
        advertiser = BleAdvertiser(
            context = null,
            externalScope = testScope,
            customHardwareAdvertiser = fakeHardware
        )
    }

    @Test
    fun testOwnSosPreemptsActiveRelay() = testScope.runTest {
        val relayPacket = makePacket(senderId = 101.toShort(), timestamp = 1000, ttl = 3)
        val ownSosPacket = makePacket(senderId = 999.toShort(), timestamp = 1005, ttl = 4)

        // Start relay
        advertiser.queueRelay(relayPacket)
        runCurrent()

        assertEquals(101.toShort(), fakeHardware.activePacket?.senderId)
        assertEquals(AdvPriority.RELAY, fakeHardware.activePriority)
        assertTrue(advertiser.isAdvertising)

        // Own SOS arrives -> must preempt active relay
        advertiser.startAdvertising(ownSosPacket)
        runCurrent()

        // Verify preempted: active packet is now own SOS
        assertEquals(999.toShort(), fakeHardware.activePacket?.senderId)
        assertEquals(AdvPriority.OWN_SOS, fakeHardware.activePriority)
        assertTrue(fakeHardware.stopCount >= 1)
    }

    @Test
    fun testDedupReplacementInQueue() = testScope.runTest {
        val activePacket = makePacket(senderId = 50.toShort(), timestamp = 500, ttl = 4)
        val relay1 = makePacket(senderId = 100.toShort(), timestamp = 1000, ttl = 3)
        val relay2Updated = makePacket(senderId = 100.toShort(), timestamp = 1000, ttl = 2)

        // Occupy active slot
        advertiser.startAdvertising(activePacket)
        runCurrent()
        assertEquals(0, advertiser.getQueueSize())

        // Queue first relay
        advertiser.queueRelay(relay1)
        runCurrent()
        assertEquals(1, advertiser.getQueueSize())

        // Queue second relay with identical dedup key (senderId=100, timestamp=1000, SOS flag)
        advertiser.queueRelay(relay2Updated)
        runCurrent()

        // Queue size should remain 1, but updated with ttl=2
        assertEquals(1, advertiser.getQueueSize())
    }

    @Test
    fun testQueueAdvancesWhenSlotTimesOut() = testScope.runTest {
        val packetA = makePacket(senderId = 10.toShort(), timestamp = 100, ttl = 4)
        val packetB = makePacket(senderId = 20.toShort(), timestamp = 200, ttl = 3)

        advertiser.queueRelay(packetA)
        runCurrent()
        assertEquals(10.toShort(), fakeHardware.activePacket?.senderId)

        advertiser.queueRelay(packetB)
        runCurrent()
        assertEquals(1, advertiser.getQueueSize())

        // Advance time by slot timeout (10 seconds)
        advanceTimeBy(Constants.MESH_RELAY_ADVERTISE_MS + 10L)
        runCurrent()

        // Packet A timed out and Packet B is now advertising
        assertEquals(20.toShort(), fakeHardware.activePacket?.senderId)
        assertEquals(0, advertiser.getQueueSize())
    }

    @Test
    fun testBoundedQueueCapacity() = testScope.runTest {
        // Occupy active slot with own SOS (rank 1)
        val active = makePacket(senderId = 1.toShort(), timestamp = 1)
        advertiser.startAdvertising(active)
        runCurrent()

        // Enqueue 20 distinct ACK packets (rank 2, not subject to relay battery cap)
        for (i in 2..21) {
            val ack = makePacket(
                senderId = 1.toShort(),
                timestamp = i,
                flags = SosPacket.FLAG_ACK.toByte()
            )
            advertiser.startAdvertising(ack)
        }
        runCurrent()

        // Must be capped at MAX_QUEUE_CAPACITY (16)
        assertEquals(BleAdvertiser.MAX_QUEUE_CAPACITY, advertiser.getQueueSize())
    }

    @Test
    fun testBatteryRelayCapEnforced() = testScope.runTest {
        // Occupy slot with 1st relay
        advertiser.queueRelay(makePacket(senderId = 1.toShort(), timestamp = 1))
        runCurrent()

        // Enqueue up to MESH_MAX_RELAYS_PER_MIN (12)
        for (i in 2..Constants.MESH_MAX_RELAYS_PER_MIN) {
            advertiser.queueRelay(makePacket(senderId = i.toShort(), timestamp = i))
        }
        runCurrent()
        // 1 active + 11 queued = 12 total relays processed
        assertEquals(11, advertiser.getQueueSize())

        // 13th relay in same minute should be rejected by battery cap
        advertiser.queueRelay(makePacket(senderId = 99.toShort(), timestamp = 99))
        runCurrent()

        // Queue size should still be 11 (rejected)
        assertEquals(11, advertiser.getQueueSize())
    }

    @Test
    fun testTransientErrorRetriesOnce() = testScope.runTest {
        // Code 2 = ADVERTISE_FAILED_TOO_MANY_ADVERTISERS
        fakeHardware.nextErrorCode = 2

        val packet = makePacket(senderId = 42.toShort(), timestamp = 420)
        advertiser.startAdvertising(packet)
        runCurrent()

        // Initially failed, startCount = 1
        assertEquals(1, fakeHardware.startCount)
        assertFalse(advertiser.isAdvertising)

        // Advance 200ms for retry delay
        advanceTimeBy(250L)
        runCurrent()

        // Retried and succeeded
        assertEquals(2, fakeHardware.startCount)
        assertTrue(advertiser.isAdvertising)
        assertEquals(42.toShort(), fakeHardware.activePacket?.senderId)
    }

    @Test
    fun testStopAllClearsQueueAndStops() = testScope.runTest {
        advertiser.startAdvertising(makePacket(senderId = 1.toShort(), timestamp = 1))
        advertiser.queueRelay(makePacket(senderId = 2.toShort(), timestamp = 2))
        runCurrent()

        assertEquals(1, advertiser.getQueueSize())
        assertTrue(advertiser.isAdvertising)

        advertiser.stopAll(clearQueue = true)
        runCurrent()

        assertEquals(0, advertiser.getQueueSize())
        assertFalse(advertiser.isAdvertising)
    }

    @Test
    fun testAckRespectsCallerTimeout() = testScope.runTest {
        // The ACK burst path passes a short per-burst timeout so bursts spaced
        // 3.5s apart actually get airtime. The advertiser must respect it, NOT
        // override to the 10s relay duration (which queued bursts 2/3 behind
        // the 10s slot and defeated the burst-retry design).
        val ackPacket = SosPacket.create(
            senderId = 55,
            lat = null,
            lon = null,
            isAck = true,
            targetSenderId = 1234
        )
        advertiser.startAdvertising(ackPacket, timeoutMs = 3000L)
        runCurrent()

        assertEquals("ACK must use the caller's 3s timeout, not the 10s relay slot",
            3000L, fakeHardware.lastDurationMs)
        assertEquals(AdvPriority.OWN_ACK, fakeHardware.activePriority)
    }

    @Test
    fun testAckDefaultTimeoutFallsBackToRelayDuration() = testScope.runTest {
        // When no custom timeout is passed (default SOS_ADVERTISE_TIMEOUT_MS),
        // an ACK should still fall back to MESH_RELAY_ADVERTISE_MS for legacy
        // callers that don't use the burst path.
        val ackPacket = SosPacket.create(
            senderId = 55,
            lat = null,
            lon = null,
            isAck = true,
            targetSenderId = 1234
        )
        advertiser.startAdvertising(ackPacket) // default timeout
        runCurrent()

        assertEquals("ACK with default timeout falls back to relay duration",
            Constants.MESH_RELAY_ADVERTISE_MS, fakeHardware.lastDurationMs)
    }
}
