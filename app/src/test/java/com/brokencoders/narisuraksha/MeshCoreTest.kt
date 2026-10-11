package com.brokencoders.narisuraksha

import com.brokencoders.narisuraksha.ble.BleDiagnosticsTracker
import com.brokencoders.narisuraksha.ble.MeshCore
import com.brokencoders.narisuraksha.ble.MeshOutcome
import com.brokencoders.narisuraksha.ble.MeshTransport
import com.brokencoders.narisuraksha.ble.ReceivedSos
import com.brokencoders.narisuraksha.core.SosPacket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifies the shared transport-agnostic mesh pipeline: one dedup cache, TTL
 * handling and budgets across BLE and WiFi ingress, so cross-medium storms
 * are impossible and every medium relays for the others.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MeshCoreTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var core: MeshCore
    private lateinit var emitted: MutableList<ReceivedSos>
    private lateinit var relayed: MutableList<SosPacket>

    private fun sos(
        senderId: Short = 100,
        timestamp: Int = 1000,
        ttl: Int = 4
    ) = SosPacket(
        senderId = senderId,
        timestamp = timestamp,
        lat = 12.9716f,
        lon = 77.5946f,
        flags = SosPacket.FLAG_SOS.toByte(),
        ttl = ttl
    )

    @Before
    fun setUp() {
        BleDiagnosticsTracker.clearLogs()
        emitted = mutableListOf()
        relayed = mutableListOf()
        core = MeshCore(
            myDeviceId = 7,
            externalScope = testScope,
            jitterMs = { 0L }
        )
        core.emitter = { emitted.add(it) }
        core.relayHandler = { relayed.add(it) }
    }

    @Test
    fun testSosAlertsAndRelaysWithDecrementedTtl() = testScope.runTest {
        val outcome: MeshOutcome = core.ingest(sos(), rssi = -60, MeshTransport.BLE)
        runCurrent()

        assertTrue(outcome.alerted)
        assertTrue(outcome.relayed)
        assertEquals(1, emitted.size)
        assertEquals(1, relayed.size)
        assertEquals(3, relayed[0].ttl)
        assertEquals(100.toShort(), relayed[0].senderId)
    }

    @Test
    fun testCrossTransportDuplicateDropped() = testScope.runTest {
        // Heard over BLE first: alert + relay scheduled.
        core.ingest(sos(), rssi = -60, MeshTransport.BLE)
        runCurrent()
        assertEquals(1, emitted.size)
        assertEquals(1, relayed.size)

        // Same broadcast re-heard over WiFi: recognised as the same event.
        val outcome = core.ingest(sos(), rssi = -50, MeshTransport.WIFI)
        runCurrent()

        assertFalse(outcome.alerted)
        assertFalse(outcome.relayed)
        assertEquals(1, emitted.size)
        assertEquals(1, relayed.size)
    }

    @Test
    fun testTtlExhaustedAlertsButNeverRelays() = testScope.runTest {
        val outcome = core.ingest(sos(ttl = 0), rssi = -60, MeshTransport.WIFI)
        runCurrent()

        assertTrue(outcome.alerted)
        assertFalse(outcome.relayed)
        assertEquals(1, emitted.size)
        assertTrue(relayed.isEmpty())
    }

    @Test
    fun testForeignAckRelayedWithoutAlert() = testScope.runTest {
        val ack = SosPacket.create(
            senderId = 55,
            lat = null,
            lon = null,
            isAck = true,
            targetSenderId = 999,
            ttl = 4
        )
        val outcome = core.ingest(ack, rssi = -60, MeshTransport.BLE)
        runCurrent()

        assertFalse(outcome.alerted)
        assertTrue(outcome.relayed)
        assertTrue(emitted.isEmpty())
        assertEquals(1, relayed.size)
        assertEquals(3, relayed[0].ttl)
    }

    @Test
    fun testWifiAckToMeConsumed() = testScope.runTest {
        // PhoneB's ACK arrives over WiFi (no RSSI on WiFi): victim consumes it.
        val ack = SosPacket.create(
            senderId = 55,
            lat = null,
            lon = null,
            isAck = true,
            targetSenderId = 7,
            ttl = 4
        )
        val outcome = core.ingest(ack, rssi = Int.MIN_VALUE, MeshTransport.WIFI)
        runCurrent()

        assertTrue(outcome.alerted)
        assertFalse(outcome.relayed)
        assertEquals(1, emitted.size)
        assertEquals(com.brokencoders.narisuraksha.ble.MeshTransport.WIFI, emitted[0].transport)
        assertTrue(relayed.isEmpty())
    }

    @Test
    fun testForeignAckRelayedOverEitherMedium() = testScope.runTest {
        // ACK for another victim heard over WiFi: forward silently, no alert.
        val ack = SosPacket.create(
            senderId = 55,
            lat = null,
            lon = null,
            isAck = true,
            targetSenderId = 999,
            ttl = 2
        )
        val outcome = core.ingest(ack, rssi = Int.MIN_VALUE, MeshTransport.WIFI)
        runCurrent()

        assertFalse(outcome.alerted)
        assertTrue(outcome.relayed)
        assertTrue(emitted.isEmpty())
        assertEquals(1, relayed[0].ttl)
    }

    @Test
    fun testOwnPacketIgnored() = testScope.runTest {
        val outcome = core.ingest(sos(senderId = 7), rssi = -50, MeshTransport.BLE)
        runCurrent()

        assertFalse(outcome.alerted)
        assertFalse(outcome.relayed)
        assertTrue(emitted.isEmpty())
        assertTrue(relayed.isEmpty())
    }

    @Test
    fun testTestBeaconAlertsButNeverRelays() = testScope.runTest {
        val test = SosPacket.create(
            senderId = 42,
            lat = 12.0,
            lon = 77.0,
            isTest = true,
            ttl = 4
        )
        val outcome = core.ingest(test, rssi = -60, MeshTransport.WIFI)
        runCurrent()

        assertTrue(outcome.alerted)
        assertFalse(outcome.relayed)
        assertEquals(1, emitted.size)
        assertTrue(relayed.isEmpty())
    }
}
