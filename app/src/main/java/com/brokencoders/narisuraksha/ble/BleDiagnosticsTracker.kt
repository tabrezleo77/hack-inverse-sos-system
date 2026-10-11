package com.brokencoders.narisuraksha.ble

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BleLogEntry(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
    val tag: String,
    val message: String,
    val isError: Boolean = false
)

/**
 * Tracks real-time BLE diagnostics, packet counters, logs, and debug scan mode.
 * Emits structured Logcat messages (e.g. NARI_BLE_ADVERTISE_START, NARI_BLE_PACKET_RECEIVED)
 * for rapid diagnostics across physical Android devices.
 */
object BleDiagnosticsTracker {

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _packetsSentCount = MutableStateFlow(0)
    val packetsSentCount: StateFlow<Int> = _packetsSentCount.asStateFlow()

    private val _packetsReceivedCount = MutableStateFlow(0)
    val packetsReceivedCount: StateFlow<Int> = _packetsReceivedCount.asStateFlow()

    private val _acksSentCount = MutableStateFlow(0)
    val acksSentCount: StateFlow<Int> = _acksSentCount.asStateFlow()

    private val _acksReceivedCount = MutableStateFlow(0)
    val acksReceivedCount: StateFlow<Int> = _acksReceivedCount.asStateFlow()

    private val _rejectedPacketsCount = MutableStateFlow(0)
    val rejectedPacketsCount: StateFlow<Int> = _rejectedPacketsCount.asStateFlow()

    private val _relayedCount = MutableStateFlow(0)
    val relayedCount: StateFlow<Int> = _relayedCount.asStateFlow()

    private val _duplicatesDroppedCount = MutableStateFlow(0)
    val duplicatesDroppedCount: StateFlow<Int> = _duplicatesDroppedCount.asStateFlow()

    private val _lastReceivedTtl = MutableStateFlow<Int?>(null)
    val lastReceivedTtl: StateFlow<Int?> = _lastReceivedTtl.asStateFlow()

    // Debug scan mode: When true, enables minimal/software filtering so hardware driver
    // limitations do not suppress incoming packets on heterogeneous OEM chipsets.
    // Default to TRUE: universal discovery across chipsets beats battery savings,
    // and the strict readiness gate + diagnostics screen let users toggle it.
    private val _debugScanModeEnabled = MutableStateFlow(true)
    val debugScanModeEnabled: StateFlow<Boolean> = _debugScanModeEnabled.asStateFlow()

    private val _logs = MutableStateFlow<List<BleLogEntry>>(emptyList())
    val logs: StateFlow<List<BleLogEntry>> = _logs.asStateFlow()

    @Synchronized
    fun recordEvent(tag: String, message: String, isError: Boolean = false) {
        if (isError) {
            Log.e(tag, message)
        } else {
            Log.i(tag, message)
        }

        val entry = BleLogEntry(
            tag = tag,
            message = message,
            isError = isError
        )

        val currentList = _logs.value.toMutableList()
        currentList.add(0, entry)
        if (currentList.size > 80) {
            currentList.removeAt(currentList.lastIndex)
        }
        _logs.value = currentList
    }

    fun setAdvertising(active: Boolean) {
        _isAdvertising.value = active
    }

    fun setScanning(active: Boolean) {
        _isScanning.value = active
    }

    fun incrementPacketsSent() {
        _packetsSentCount.update { it + 1 }
    }

    fun incrementPacketsReceived() {
        _packetsReceivedCount.update { it + 1 }
    }

    fun incrementAcksSent() {
        _acksSentCount.update { it + 1 }
    }

    fun incrementAcksReceived() {
        _acksReceivedCount.update { it + 1 }
    }

    fun incrementRejectedPackets() {
        _rejectedPacketsCount.update { it + 1 }
    }

    fun incrementRelayed() {
        _relayedCount.update { it + 1 }
    }

    fun incrementDuplicatesDropped() {
        _duplicatesDroppedCount.update { it + 1 }
    }

    fun setLastReceivedTtl(ttl: Int) {
        _lastReceivedTtl.value = ttl
    }

    fun setDebugScanMode(enabled: Boolean) {
        _debugScanModeEnabled.value = enabled
        recordEvent("NARI_BLE_CONFIG", "Debug Scan Mode (software filtering) set to: $enabled")
    }

    fun clearLogs() {
        _logs.value = emptyList()
        _packetsSentCount.value = 0
        _packetsReceivedCount.value = 0
        _acksSentCount.value = 0
        _acksReceivedCount.value = 0
        _rejectedPacketsCount.value = 0
        _relayedCount.value = 0
        _duplicatesDroppedCount.value = 0
        _lastReceivedTtl.value = null
    }
}
