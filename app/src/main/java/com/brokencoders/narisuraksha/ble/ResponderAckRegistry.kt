package com.brokencoders.narisuraksha.ble

import java.util.concurrent.ConcurrentHashMap

/**
 * Process-wide registry of distinct responders who have acknowledged the
 * current SOS, keyed by responder device ID.
 *
 * Lives in a singleton (not an Activity-scoped ViewModel) so the acknowledged
 * count survives activity recreation (rotation, process restore, returning to
 * the app) and is fed by MeshCore the moment an ACK-for-me is consumed —
 * independent of whether a MainViewModel exists. The UI reads [responderCount]
 * and [responderIds] from here.
 */
object ResponderAckRegistry {

    /** responderId -> first-seen epoch millis (for diagnostics). */
    private val responders = ConcurrentHashMap<Short, Long>()

    /**
     * Records a responder ACK. Returns true if this responder is newly
     * acknowledged (not a duplicate), false if already recorded.
     * Thread-safe and idempotent — duplicate ACK bursts from the same
     * responder never inflate the count.
     */
    fun recordAck(responderId: Short): Boolean {
        return responders.putIfAbsent(responderId, System.currentTimeMillis()) == null
    }

    /** Distinct responder count for the current SOS. */
    val responderCount: Int get() = responders.size

    /** Snapshot of acknowledged responder IDs. */
    val responderIds: Set<Short> get() = responders.keys.toSet()

    /**
     * Clears all recorded responders — called when a new SOS epoch begins
     * (triggerSos / cancelCountdown / stopSos) so the count resets for the
     * next emergency.
     */
    fun clear() {
        responders.clear()
    }
}
