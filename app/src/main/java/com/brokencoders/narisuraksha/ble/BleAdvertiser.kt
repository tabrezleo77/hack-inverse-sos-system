package com.brokencoders.narisuraksha.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.brokencoders.narisuraksha.core.Constants
import com.brokencoders.narisuraksha.core.PermissionHelper
import com.brokencoders.narisuraksha.core.SosPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Priority levels for the single-advertiser queue.
 * Own SOS distress broadcast holds absolute priority (rank 1) and preempts ACKs (rank 2) and relay forwarding (rank 3).
 */
enum class AdvPriority(val rank: Int) {
    OWN_SOS(1),
    OWN_ACK(2),
    RELAY(3)
}

data class QueuedAdv(
    val packet: SosPacket,
    val priority: AdvPriority,
    val durationMs: Long,
    val enqueuedAt: Long = System.currentTimeMillis()
)

/**
 * Thin abstraction over BLE advertiser hardware for unit testability without Android stubs.
 */
interface HardwareAdvertiser {
    fun startAdvertising(
        packet: SosPacket,
        priority: AdvPriority,
        durationMs: Long,
        onSuccess: () -> Unit,
        onFailure: (errorCode: Int) -> Unit
    )
    fun stopAdvertising()
}

/**
 * Concrete Android BLE hardware advertiser implementation.
 * Builds low-latency, high-tx advertise frames under the 31-byte legacy BLE limit.
 */
class AndroidHardwareAdvertiser(
    private val context: Context,
    private val getAdvertiser: () -> BluetoothLeAdvertiser?
) : HardwareAdvertiser {

    private var activeCallback: AdvertiseCallback? = null
    private var fallbackAttempted = false

    @SuppressLint("MissingPermission")
    override fun startAdvertising(
        packet: SosPacket,
        priority: AdvPriority,
        durationMs: Long,
        onSuccess: () -> Unit,
        onFailure: (errorCode: Int) -> Unit
    ) {
        if (!PermissionHelper.hasBluetoothAdvertisePermission(context)) {
            Log.w(TAG, "Missing BLUETOOTH_ADVERTISE permission")
            onFailure(AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR)
            return
        }

        val advertiser = getAdvertiser()
        if (advertiser == null) {
            Log.w(TAG, "BluetoothLeAdvertiser unavailable or Bluetooth adapter off")
            onFailure(AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR)
            return
        }

        val encodedPacket = PacketCodec.encode(packet)
        if (encodedPacket == null) {
            Log.e(TAG, "Failed to encode packet for hardware advertisement")
            onFailure(AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR)
            return
        }

        val payloadPair = buildAdvertisePayloads(encodedPacket)
        if (payloadPair == null) {
            Log.e(TAG, "Encoded packet (${encodedPacket.size}B) exceeds legacy BLE advertising limits")
            onFailure(AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE)
            return
        }

        val (data, scanResponse) = payloadPair
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(0)
            .build()

        fallbackAttempted = false
        val cb = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                onSuccess()
            }

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                val isDataOrInternalError =
                    errorCode == ADVERTISE_FAILED_DATA_TOO_LARGE ||
                    errorCode == ADVERTISE_FAILED_INTERNAL_ERROR

                if (isDataOrInternalError && !fallbackAttempted) {
                    fallbackAttempted = true
                    try {
                        val minimalData = AdvertiseData.Builder()
                            .addServiceData(Constants.SOS_PARCEL_UUID, encodedPacket)
                            .setIncludeDeviceName(false)
                            .setIncludeTxPowerLevel(false)
                            .build()
                        advertiser.startAdvertising(settings, minimalData, this)
                        return
                    } catch (e: Exception) {
                        Log.e(TAG, "Minimal fallback advertising attempt failed", e)
                    }
                }
                onFailure(errorCode)
            }
        }

        activeCallback = null

        try {
            if (scanResponse != null) {
                advertiser.startAdvertising(settings, data, scanResponse, cb)
            } else {
                advertiser.startAdvertising(settings, data, cb)
            }
            activeCallback = cb
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting BLE advertisement on hardware", e)
            activeCallback = null
            onFailure(AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR)
        }
    }

    @SuppressLint("MissingPermission")
    override fun stopAdvertising() {
        val cb = activeCallback
        if (cb != null) {
            try {
                getAdvertiser()?.stopAdvertising(cb)
            } catch (e: Exception) {
                Log.e(TAG, "Exception stopping BLE hardware advertisement", e)
            }
            activeCallback = null
        }
    }

    private fun buildAdvertisePayloads(encoded: ByteArray): Pair<AdvertiseData, AdvertiseData?>? {
        val uuid = Constants.SOS_PARCEL_UUID
        val sdOverhead = serviceDataOverhead(uuid)
        val mfgOverhead = AD_MANUFACTURER_DATA_OVERHEAD

        val mainSdSize = AD_FLAGS_SIZE + sdOverhead + encoded.size
        val scanRespMfgSize = AD_FLAGS_SIZE + mfgOverhead + encoded.size

        if (mainSdSize <= MAX_LEGACY_ADV_PAYLOAD && scanRespMfgSize <= MAX_LEGACY_ADV_PAYLOAD) {
            val data = AdvertiseData.Builder()
                .addServiceData(uuid, encoded)
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .build()
            val scanResponse = AdvertiseData.Builder()
                .addManufacturerData(Constants.MANUFACTURER_ID, encoded)
                .setIncludeDeviceName(false)
                .build()
            return data to scanResponse
        }

        if (mainSdSize <= MAX_LEGACY_ADV_PAYLOAD) {
            val data = AdvertiseData.Builder()
                .addServiceData(uuid, encoded)
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .build()
            return data to null
        }

        return null
    }

    private fun serviceDataOverhead(uuid: ParcelUuid): Int =
        if (isShortUuid(uuid.uuid)) AD_SERVICE_DATA_16BIT_OVERHEAD else AD_SERVICE_DATA_128BIT_OVERHEAD

    private fun isShortUuid(uuid: UUID): Boolean {
        val s = uuid.toString().lowercase()
        return s.length == 36 && s.startsWith("0000") && s.endsWith("0000-1000-8000-00805f9b34fb")
    }

    companion object {
        private const val TAG = "BleAdvertiser"
        private const val MAX_LEGACY_ADV_PAYLOAD = 31
        private const val AD_FLAGS_SIZE = 3
        private const val AD_SERVICE_DATA_16BIT_OVERHEAD = 4
        private const val AD_SERVICE_DATA_128BIT_OVERHEAD = 18
        private const val AD_MANUFACTURER_DATA_OVERHEAD = 4
    }
}

/**
 * Single-slot BLE advertiser queue managing priorities, preemption, slot time limits,
 * deduplication, and retries.
 */
class BleAdvertiser(
    context: Context? = null,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    customHardwareAdvertiser: HardwareAdvertiser? = null
) {
    private val hardwareAdvertiser: HardwareAdvertiser = customHardwareAdvertiser
        ?: if (context != null) {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            AndroidHardwareAdvertiser(context) { bluetoothManager?.adapter?.bluetoothLeAdvertiser }
        } else {
            throw IllegalArgumentException("Context required when customHardwareAdvertiser is not provided")
        }

    @Volatile
    var isAdvertising = false
        private set

    private var timeoutJob: Job? = null
    private var startRetryCount = 0

    // ---------------------------------------------------------------------
    // Queue & Concurrency state
    // ---------------------------------------------------------------------
    private val stateLock = Any()

    /** Packet currently occupying the physical hardware advertiser */
    private var currentPacket: SosPacket? = null
    private var currentPriority: AdvPriority? = null
    private var currentDurationMs: Long = 0L

    /** Advertisements waiting for slot availability */
    private val advertiseQueue = ArrayDeque<QueuedAdv>()

    /** Timestamps of relayed packets in the last 60s for battery protection */
    private val relayTimestamps = ConcurrentLinkedQueue<Long>()

    fun getQueueSize(): Int = synchronized(stateLock) { advertiseQueue.size }
    fun getCurrentPacket(): SosPacket? = synchronized(stateLock) { currentPacket }
    fun getCurrentPriority(): AdvPriority? = synchronized(stateLock) { currentPriority }

    // ---------------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------------

    /**
     * Start advertising an originating SOS or ACK beacon.
     * Own SOS distress preempts active relays immediately.
     */
    fun startAdvertising(packet: SosPacket, timeoutMs: Long = Constants.SOS_ADVERTISE_TIMEOUT_MS) {
        val priority = when {
            packet.isAck -> AdvPriority.OWN_ACK
            else -> AdvPriority.OWN_SOS
        }
        // Respect the caller's timeout for ACKs too: the ACK burst path passes a
        // short per-burst timeout (ACK_ADVERTISE_TIMEOUT_MS = 3s) so that bursts
        // spaced 3.5s apart actually get airtime. Overriding to the 10s relay
        // duration queued bursts 2 and 3 behind the 10s slot and defeated the
        // burst-retry design. Only default to MESH_RELAY_ADVERTISE_MS when the
        // caller didn't specify a custom timeout.
        val duration = if (timeoutMs != Constants.SOS_ADVERTISE_TIMEOUT_MS) timeoutMs
                       else if (packet.isAck) Constants.MESH_RELAY_ADVERTISE_MS
                       else timeoutMs
        enqueueOrExecute(QueuedAdv(packet, priority, duration))
    }

    /**
     * Enqueues a mesh relayed packet (SOS or ACK) with ttl-1.
     * Enforces configurable battery safety cap (max relays per minute).
     */
    fun queueRelay(packet: SosPacket) {
        val now = System.currentTimeMillis()
        val windowStart = now - 60_000L

        // Purge timestamps older than 1 minute
        while (relayTimestamps.peek()?.let { it < windowStart } == true) {
            relayTimestamps.poll()
        }

        if (relayTimestamps.size >= Constants.MESH_MAX_RELAYS_PER_MIN) {
            BleDiagnosticsTracker.recordEvent(
                "NARI_BLE_RELAY_SUPPRESSED",
                "Battery protection cap reached (${Constants.MESH_MAX_RELAYS_PER_MIN} relays/min). Suppressed #${packet.senderId}"
            )
            return
        }

        relayTimestamps.add(now)
        enqueueOrExecute(QueuedAdv(packet, AdvPriority.RELAY, Constants.MESH_RELAY_ADVERTISE_MS))
    }

    /**
     * Stop active advertising and advance queue to next pending item.
     */
    fun stopAdvertising() {
        stopAdvertisingInternal(advance = true)
    }

    /**
     * Stop advertising and optionally flush the entire pending queue.
     */
    fun stopAll(clearQueue: Boolean = true) {
        if (clearQueue) {
            synchronized(stateLock) {
                advertiseQueue.clear()
            }
        }
        stopAdvertisingInternal(advance = !clearQueue)
    }

    // ---------------------------------------------------------------------
    // Queue Dispatch
    // ---------------------------------------------------------------------
    private fun enqueueOrExecute(request: QueuedAdv) {
        synchronized(stateLock) {
            val occupyingPriority = currentPriority

            when {
                // Case 1: Slot is idle -> run immediately
                occupyingPriority == null -> {
                    dispatchHardware(request)
                }

                // Case 2: New request has higher priority than currently running item -> preempt
                request.priority.rank < occupyingPriority.rank -> {
                    Log.i(TAG, "Preempting lower priority $occupyingPriority with ${request.priority}")
                    val preemptedPacket = currentPacket
                    val preemptedPrio = currentPriority
                    if (preemptedPacket != null && preemptedPrio != null && preemptedPrio != AdvPriority.RELAY) {
                        // Re-queue non-relay items (e.g. ACK) so they resume later
                        advertiseQueue.addFirst(QueuedAdv(preemptedPacket, preemptedPrio, Constants.MESH_RELAY_ADVERTISE_MS))
                    }
                    stopHardwareOnly()
                    dispatchHardware(request)
                }

                // Case 3: Same or lower priority -> add to queue with deduplication replacement
                else -> {
                    val dedupKey = RelayDedupCache.makeKey(request.packet)
                    val existingIndex = advertiseQueue.indexOfFirst {
                        RelayDedupCache.makeKey(it.packet) == dedupKey
                    }
                    if (existingIndex != -1) {
                        // Replace queued item with the same dedup key instead of stacking duplicates
                        advertiseQueue[existingIndex] = request
                        Log.d(TAG, "Replaced existing queued item for key $dedupKey")
                    } else {
                        // Bound the queue length (max 16 entries)
                        if (advertiseQueue.size >= MAX_QUEUE_CAPACITY) {
                            advertiseQueue.removeLast()
                        }
                        advertiseQueue.addLast(request)
                        Log.d(TAG, "Enqueued ${request.priority} #${request.packet.senderId} (queue size = ${advertiseQueue.size})")
                    }
                }
            }
        }
    }

    fun advanceQueue() {
        val next = synchronized(stateLock) {
            if (advertiseQueue.isEmpty()) return
            // Sort by priority rank (OWN_SOS 1 > OWN_ACK 2 > RELAY 3), FIFO within same rank
            val bestIndex = advertiseQueue.indices.minByOrNull { advertiseQueue[it].priority.rank } ?: return
            advertiseQueue.removeAt(bestIndex)
        }

        Log.i(TAG, "Advancing advertise queue -> starting ${next.priority} #${next.packet.senderId}")
        dispatchHardware(next)
    }

    private fun dispatchHardware(request: QueuedAdv) {
        synchronized(stateLock) {
            currentPacket = request.packet
            currentPriority = request.priority
            currentDurationMs = request.durationMs
        }

        val logTag = when (request.priority) {
            AdvPriority.RELAY -> "NARI_BLE_RELAY_BROADCAST"
            AdvPriority.OWN_ACK -> "NARI_BLE_ACK_SENT"
            AdvPriority.OWN_SOS -> "NARI_BLE_ADVERTISE_START"
        }
        BleDiagnosticsTracker.recordEvent(
            logTag,
            "Broadcasting [${request.priority}]: senderId=#${request.packet.senderId}, ttl=${request.packet.ttl}, " +
                    "duration=${request.durationMs / 1000}s"
        )

        hardwareAdvertiser.startAdvertising(
            packet = request.packet,
            priority = request.priority,
            durationMs = request.durationMs,
            onSuccess = {
                startRetryCount = 0
                isAdvertising = true
                BleDiagnosticsTracker.setAdvertising(true)
                BleDiagnosticsTracker.incrementPacketsSent()

                val desc = when {
                    request.priority == AdvPriority.RELAY -> "RELAY #${request.packet.senderId} (TTL: ${request.packet.ttl})"
                    request.packet.isAck -> "ACK to #${request.packet.targetSenderId}"
                    request.packet.isTest -> "TEST SOS #${request.packet.senderId}"
                    else -> "DISTRESS SOS #${request.packet.senderId}"
                }
                BleDiagnosticsTracker.recordEvent(
                    "NARI_BLE_ADVERTISE_SUCCESS",
                    "BLE Broadcast active [$desc] (LowLatency, HighTx)"
                )

                timeoutJob?.cancel()
                timeoutJob = externalScope.launch {
                    delay(request.durationMs)
                    BleDiagnosticsTracker.recordEvent(
                        "NARI_BLE_ADVERTISE_START",
                        "Advertising slot timeout (${request.durationMs / 1000}s) reached for ${request.priority}. Stopping."
                    )
                    stopAdvertisingInternal(advance = true)
                }
            },
            onFailure = { errorCode ->
                val isTransient = (errorCode == AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS ||
                        errorCode == AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED)

                if (isTransient && startRetryCount < 1) {
                    startRetryCount++
                    Log.w(TAG, "Transient advertising failure (Code $errorCode). Retrying once in 200ms...")
                    // Snapshot the failed request: a higher-priority SOS enqueued during
                    // the 200ms window must not be clobbered by a stale re-dispatch.
                    val failedRequest = request
                    externalScope.launch {
                        delay(200L)
                        val currentAtRetry: SosPacket? = synchronized(stateLock) { currentPacket }
                        when {
                            // Still the failed slot (nothing preempted) -> retry it.
                            currentAtRetry === failedRequest.packet -> {
                                stopHardwareOnly()
                                dispatchHardware(failedRequest)
                            }
                            // Slot went idle (stopAll/stop) -> drain queue normally.
                            currentAtRetry == null -> advanceQueue()
                            // A newer packet (e.g. preempting SOS) took over -> leave it alone.
                            else -> Unit
                        }
                    }
                    return@startAdvertising
                }

                startRetryCount = 0
                synchronized(stateLock) {
                    currentPacket = null
                    currentPriority = null
                }
                isAdvertising = false
                BleDiagnosticsTracker.setAdvertising(false)

                val errorMsg = when (errorCode) {
                    AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "ADVERTISE_FAILED_DATA_TOO_LARGE (Code 1)"
                    AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS (Code 2)"
                    AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "ADVERTISE_FAILED_ALREADY_STARTED (Code 3)"
                    AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "ADVERTISE_FAILED_INTERNAL_ERROR (Code 4)"
                    AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "ADVERTISE_FAILED_FEATURE_UNSUPPORTED (Code 5)"
                    else -> "Error Code $errorCode"
                }
                BleDiagnosticsTracker.recordEvent(
                    "NARI_BLE_ADVERTISE_FAILURE",
                    "BLE Advertising failed: $errorMsg",
                    isError = true
                )

                // Dispatch async to avoid synchronous recursion (StackOverflowError)
                // when the hardware layer fails synchronously for every queued item.
                externalScope.launch { advanceQueue() }
            }
        )
    }

    private fun stopHardwareOnly() {
        timeoutJob?.cancel()
        timeoutJob = null

        // Always attempt the hardware stop, regardless of the isAdvertising flag.
        // isAdvertising is set asynchronously in the start-success callback, so if
        // stopHardwareOnly runs between dispatchHardware()'s startAdvertising()
        // call and its success callback, isAdvertising is still false and we'd
        // skip the stop — leaving a still-starting advertisement that then fails
        // the next start with ADVERTISE_FAILED_ALREADY_STARTED and gets dropped.
        // Calling stopAdvertising() on a not-yet-started slot is a safe no-op.
        try {
            hardwareAdvertiser.stopAdvertising()
        } catch (e: Exception) {
            Log.e(TAG, "Exception stopping hardware advertiser", e)
        }
        isAdvertising = false
        BleDiagnosticsTracker.setAdvertising(false)
    }

    private fun stopAdvertisingInternal(advance: Boolean) {
        stopHardwareOnly()
        synchronized(stateLock) {
            currentPacket = null
            currentPriority = null
        }
        if (advance) {
            advanceQueue()
        }
    }

    companion object {
        private const val TAG = "BleAdvertiser"
        const val MAX_QUEUE_CAPACITY = 16
    }
}