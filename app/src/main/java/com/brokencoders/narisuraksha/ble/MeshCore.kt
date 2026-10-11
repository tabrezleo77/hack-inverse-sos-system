package com.brokencoders.narisuraksha.ble

import android.util.Log
import com.brokencoders.narisuraksha.core.Constants
import com.brokencoders.narisuraksha.core.SosPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Ingress medium of a mesh packet. The relay pipeline is transport-agnostic:
 * BLE and WiFi share one [MeshCore] so deduplication, TTL handling, budgets
 * and rate limits apply across media and cross-medium storms are impossible.
 */
enum class MeshTransport {
    BLE,
    WIFI
}

/**
 * Result of ingesting one packet into the shared mesh pipeline.
 */
data class MeshOutcome(
    val alerted: Boolean,
    val relayed: Boolean
)

/**
 * Shared, transport-agnostic mesh ingress pipeline.
 *
 * Both [BleScanner] and the WiFi mesh transport feed decoded packets here.
 * Owns the single [RelayDedupCache], relay budget, per-sender cooldown and
 * global alert window, so a packet arriving over BLE and re-heard over WiFi
 * (or vice versa) is recognised as the same broadcast event and dropped.
 *
 * Pure coordinator: local delivery goes to [emitter], forwarding to
 * [relayHandler] after a randomised jitter delay. Diagnostics tags carry the
 * ingress transport prefix (`NARI_BLE_*` / `NARI_WIFI_*`).
 */
class MeshCore(
    private val myDeviceId: Short,
    private val externalScope: CoroutineScope,
    val dedupCache: RelayDedupCache = RelayDedupCache(),
    private val relayBudgetTracker: RelayBudgetTracker = RelayBudgetTracker(),
    private val jitterMs: (LongRange) -> Long = { range ->
        kotlin.random.Random.nextLong(range.first, range.last + 1)
    },
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    /** Invoked with the TTL-decremented packet when this node must forward. */
    var relayHandler: ((SosPacket) -> Unit)? = null

    /** Invoked for locally delivered alerts and consumed ACKs. */
    var emitter: ((ReceivedSos) -> Unit)? = null

    // Per-sender cooldown map (local alert spam protection, NOT relay gating).
    private val senderLastSeenMap = ConcurrentHashMap<Short, Long>()

    // Sliding window of alert timestamps across all senders (spoof-flood guard).
    private val globalAlertTimestamps = ConcurrentLinkedQueue<Long>()

    /**
     * Ingests one decoded packet from any transport.
     * Returns what happened (alerted locally and/or scheduled for relay).
     */
    fun ingest(packet: SosPacket, rssi: Int, transport: MeshTransport): MeshOutcome {
        val now = clock()
        val tag = if (transport == MeshTransport.WIFI) "NARI_WIFI" else "NARI_BLE"

        val decision = RelayPolicy.evaluate(
            packet = packet,
            myDeviceId = myDeviceId,
            cache = dedupCache,
            canRelayBudget = { relayBudgetTracker.tryAcquire() }
        )

        if (decision != RelayDecision.IGNORE_OWN) {
            BleDiagnosticsTracker.setLastReceivedTtl(packet.ttl)
        }

        return when (decision) {
            RelayDecision.IGNORE_OWN -> MeshOutcome(alerted = false, relayed = false)

            RelayDecision.DROP_DUPLICATE -> {
                BleDiagnosticsTracker.incrementDuplicatesDropped()
                BleDiagnosticsTracker.recordEvent(
                    "${tag}_DUPLICATE_DROPPED",
                    "DROP dup sender=${packet.senderId}"
                )
                MeshOutcome(alerted = false, relayed = false)
            }

            RelayDecision.RELAY_ONLY -> {
                scheduleRelay(packet, tag)
                MeshOutcome(alerted = false, relayed = true)
            }

            RelayDecision.ALERT_AND_RELAY -> {
                scheduleRelay(packet, tag)
                val alerted = processLocalAlert(packet, rssi, now, tag, transport)
                MeshOutcome(alerted = alerted, relayed = true)
            }

            RelayDecision.ALERT_ONLY -> {
                val alerted = processLocalAlert(packet, rssi, now, tag, transport)
                MeshOutcome(alerted = alerted, relayed = false)
            }

            RelayDecision.CONSUME_ACK_FOR_ME -> {
                BleDiagnosticsTracker.incrementAcksReceived()
                BleDiagnosticsTracker.recordEvent(
                    "${tag}_ACK_RECEIVED",
                    "RX sender=${packet.senderId} ttl=${packet.ttl} rssi=$rssi"
                )
                // Record the distinct responder in the process-wide registry so
                // the acknowledged count survives activity recreation and does
                // not depend on a MainViewModel existing.
                ResponderAckRegistry.recordAck(packet.senderId)
                emitter?.invoke(ReceivedSos(packet = packet, rssi = rssi, receivedAt = now, transport = transport))
                MeshOutcome(alerted = true, relayed = false)
            }
        }
    }

    private fun scheduleRelay(packet: SosPacket, tag: String) {
        val relayedPacket = packet.withDecrementedTtl()
        val delayMs = jitterMs(
            Constants.MESH_RELAY_DELAY_MIN_MS..Constants.MESH_RELAY_DELAY_MAX_MS
        )
        BleDiagnosticsTracker.recordEvent("${tag}_RELAY", "RELAY ttl=${relayedPacket.ttl}")
        BleDiagnosticsTracker.incrementRelayed()

        externalScope.launch {
            delay(delayMs)
            try {
                relayHandler?.invoke(relayedPacket)
            } catch (e: Exception) {
                Log.w(TAG, "Relay handler failed: ${e.message}")
            }
        }
    }

    /**
     * Local alert delivery with per-sender cooldown + global flood guard.
     * Returns false when the alert was suppressed (relay forwarding, decided
     * earlier, is intentionally NOT gated by these limits).
     */
    private fun processLocalAlert(
        packet: SosPacket,
        rssi: Int,
        now: Long,
        tag: String,
        transport: MeshTransport
    ): Boolean {
        // Clean up per-sender cooldown map periodically
        if (senderLastSeenMap.size > 200) {
            senderLastSeenMap.entries.removeIf { now - it.value > 180_000L }
        }

        // Rate-limiting for local distress alerts (does NOT affect mesh relay forwarding)
        if (!packet.isAck && !packet.isTest) {
            // Global alert rate limit
            val windowStart = now - Constants.GLOBAL_ALERT_RATE_LIMIT_WINDOW_MS
            while (globalAlertTimestamps.peek()?.let { it < windowStart } == true) {
                globalAlertTimestamps.poll()
            }
            if (globalAlertTimestamps.size >= Constants.GLOBAL_ALERT_RATE_LIMIT_MAX_PER_MINUTE) {
                BleDiagnosticsTracker.incrementRejectedPackets()
                BleDiagnosticsTracker.recordEvent(
                    "${tag}_PACKET_REJECTED",
                    "Global alert rate limit reached (${Constants.GLOBAL_ALERT_RATE_LIMIT_MAX_PER_MINUTE}/min). Suppressed #${packet.senderId}"
                )
                return false
            }

            // Per-sender cooldown (10s)
            val lastSeenForSender = senderLastSeenMap[packet.senderId]
            if (lastSeenForSender != null && (now - lastSeenForSender < Constants.PACKET_RATE_LIMIT_MS)) {
                BleDiagnosticsTracker.incrementRejectedPackets()
                return false
            }

            globalAlertTimestamps.add(now)
            senderLastSeenMap[packet.senderId] = now
        }

        BleDiagnosticsTracker.incrementPacketsReceived()
        val eventTag = if (packet.isTest) "${tag}_TEST_RECEIVED" else "${tag}_PACKET_RECEIVED"
        BleDiagnosticsTracker.recordEvent(
            eventTag,
            "RX sender=${packet.senderId} ttl=${packet.ttl} rssi=$rssi"
        )

        Log.i(TAG, "Delivered SOS alert from sender ${packet.senderId}, RSSI: $rssi dBm, TTL: ${packet.ttl}")
        emitter?.invoke(ReceivedSos(packet = packet, rssi = rssi, receivedAt = now, transport = transport))
        return true
    }

    companion object {
        private const val TAG = "MeshCore"
    }
}
