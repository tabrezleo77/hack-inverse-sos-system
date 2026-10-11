package com.brokencoders.narisuraksha.ble

import com.brokencoders.narisuraksha.core.Constants
import com.brokencoders.narisuraksha.core.SosPacket
import java.util.concurrent.ConcurrentHashMap

/**
 * Composite key identifying a unique distress broadcast event or ACK.
 * senderId: originating sender ID
 * timestampOrTargetId: epoch timestamp for distress SOS, or targetSenderId for ACK
 * packetType: isolated flag type (SOS, ACK, TEST)
 */
data class DedupKey(
    val senderId: Short,
    val timestampOrTargetId: Int,
    val packetType: Byte
)

/**
 * Thread-safe deduplication cache for BLE mesh relay and packet filtering.
 *
 * Guarantees:
 * - Key = (senderId, timestamp or targetId field, packet type SOS/ACK)
 * - Entries expire after MESH_DEDUP_TTL_MS (~60s)
 * - Bounded size: MESH_DEDUP_MAX_ENTRIES (256, evict oldest)
 * - Clock injection for deterministic time-advancement testing
 * - Pure Kotlin (zero Android framework dependencies)
 */
class RelayDedupCache(
    private val expiryMs: Long = Constants.MESH_DEDUP_TTL_MS,
    private val maxCapacity: Int = Constants.MESH_DEDUP_MAX_ENTRIES,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val cache = ConcurrentHashMap<DedupKey, Long>()

    /**
     * Checks if key is already seen within [expiryMs].
     * Returns true if newly inserted (not seen or expired).
     * Returns false if duplicate within the expiry window.
     */
    @Synchronized
    fun checkAndInsert(key: DedupKey): Boolean {
        val now = clock()
        evictExpired(now)

        val lastSeen = cache[key]
        if (lastSeen != null && (now - lastSeen) < expiryMs) {
            return false // Duplicate
        }

        if (cache.size >= maxCapacity) {
            evictOldest()
        }

        cache[key] = now
        return true
    }

    /**
     * Convenience helper to check and insert a SosPacket directly.
     */
    fun checkAndInsert(packet: SosPacket): Boolean {
        return checkAndInsert(makeKey(packet))
    }

    /**
     * Backward-compatible alias for add(packet).
     */
    fun add(packet: SosPacket, nowMs: Long? = null): Boolean {
        return if (nowMs != null) {
            val key = makeKey(packet)
            synchronized(this) {
                evictExpired(nowMs)
                val lastSeen = cache[key]
                if (lastSeen != null && (nowMs - lastSeen) < expiryMs) {
                    false
                } else {
                    if (cache.size >= maxCapacity) evictOldest()
                    cache[key] = nowMs
                    true
                }
            }
        } else {
            checkAndInsert(packet)
        }
    }

    fun contains(key: DedupKey, nowMs: Long = clock()): Boolean {
        val lastSeen = cache[key] ?: return false
        return (nowMs - lastSeen) < expiryMs
    }

    fun contains(packet: SosPacket, nowMs: Long = clock()): Boolean {
        return contains(makeKey(packet), nowMs)
    }

    @Synchronized
    fun evictExpired(nowMs: Long = clock()) {
        // ConcurrentHashMap iterators do not support remove(); use removeIf instead.
        cache.entries.removeIf { nowMs - it.value >= expiryMs }
    }

    @Synchronized
    private fun evictOldest() {
        val oldestEntry = cache.entries.minByOrNull { it.value } ?: return
        cache.remove(oldestEntry.key)
    }

    fun size(): Int = cache.size

    @Synchronized
    fun clear() {
        cache.clear()
    }

    companion object {
        fun makeKey(packet: SosPacket): DedupKey {
            val packetType = (packet.flags.toInt() and SosPacket.FLAGS_MASK).toByte()
            return DedupKey(
                senderId = packet.senderId,
                timestampOrTargetId = packet.timestamp,
                packetType = packetType
            )
        }
    }
}
