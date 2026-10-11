package com.brokencoders.narisuraksha

import com.brokencoders.narisuraksha.core.SosPacket
import com.brokencoders.narisuraksha.wifi.WifiMeshTransport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Validates the WiFi UDP envelope: same SosPacket bytes as BLE, framed so
 * garbage on a busy LAN can never decode into a phantom SOS.
 */
class WifiEnvelopeTest {

    private fun sos(ttl: Int = 4) = SosPacket(
        senderId = 1234,
        timestamp = 987654,
        lat = 28.6139f,
        lon = 77.2090f,
        flags = SosPacket.FLAG_SOS.toByte(),
        ttl = ttl
    )

    @Test
    fun testWrapUnwrapRoundTripPreservesTtl() {
        val wrapped = WifiMeshTransport.wrap(sos(ttl = 3))
        assertNotNull(wrapped)

        val decoded = WifiMeshTransport.unwrap(wrapped)
        assertNotNull(decoded)
        assertEquals(1234.toShort(), decoded!!.senderId)
        assertEquals(987654, decoded.timestamp)
        assertEquals(3, decoded.ttl)
        assertEquals(28.6139f, decoded.lat, 0.0001f)
    }

    @Test
    fun testAckRoundTripPreservesTarget() {
        val ack = SosPacket.create(
            senderId = 55,
            lat = null,
            lon = null,
            isAck = true,
            targetSenderId = 1234,
            ttl = 2
        )
        val decoded = WifiMeshTransport.unwrap(WifiMeshTransport.wrap(ack))
        assertNotNull(decoded)
        assertEquals(1234.toShort(), decoded!!.targetSenderId)
        assertEquals(2, decoded.ttl)
    }

    @Test
    fun testGarbageRejected() {
        assertNull(WifiMeshTransport.unwrap(null))
        assertNull(WifiMeshTransport.unwrap(ByteArray(0)))
        assertNull(WifiMeshTransport.unwrap(ByteArray(3)))
        // Wrong magic
        assertNull(WifiMeshTransport.unwrap(byteArrayOf(0x42, 0x01, 0x00, 0x11, 0x00)))
        // Wrong version
        assertNull(WifiMeshTransport.unwrap(byteArrayOf(0x57, 0x09, 0x00, 0x11, 0x00)))
        // Truncated: claims 17 payload bytes, carries 1
        assertNull(WifiMeshTransport.unwrap(byteArrayOf(0x57, 0x01, 0x00, 0x11, 0x00)))
        // Random LAN noise
        assertNull(
            WifiMeshTransport.unwrap(
                "NOT-A-MESH-PACKET-AT-ALL-JUST-NOISE".toByteArray()
            )
        )
    }
}
