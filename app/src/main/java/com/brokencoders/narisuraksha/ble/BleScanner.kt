package com.brokencoders.narisuraksha.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import com.brokencoders.narisuraksha.core.Constants
import com.brokencoders.narisuraksha.core.DeviceIdProvider
import com.brokencoders.narisuraksha.core.PermissionHelper
import com.brokencoders.narisuraksha.core.SosPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Scans for nearby BLE SOS broadcasts.
 * Supports 16-bit Service UUID, 128-bit UUID, Manufacturer Data fallback, and raw packet scanning.
 * Includes both hardware-filtered scan and software-filtered debug scan mode for reliable cross-OEM discovery.
 */
class BleScanner(
    private val context: Context,
    private val deviceIdProvider: DeviceIdProvider,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    meshCore: MeshCore? = null
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? get() = bluetoothManager?.adapter
    private var scanner: BluetoothLeScanner? = null

    private val _received = MutableSharedFlow<ReceivedSos>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val received: Flow<ReceivedSos> = _received.asSharedFlow()

    /**
     * Shared transport-agnostic mesh pipeline (dedup, TTL, budgets, alert
     * rate limits). The WiFi mesh transport feeds the same instance, so a
     * broadcast heard over both media is recognised as one event.
     */
    private val mesh: MeshCore = meshCore ?: MeshCore(
        myDeviceId = deviceIdProvider.deviceId,
        externalScope = externalScope
    )

    init {
        mesh.emitter = { _received.tryEmit(it) }
    }

    @Volatile
    var isScanning = false
        private set

    private var fallbackScanAttempted = false

    /** RelayDedupCache with 60-second expiration (shared with WiFi transport) */
    val dedupCache: RelayDedupCache get() = mesh.dedupCache

    /** Callback invoked when a packet is scheduled for mesh relay forwarding */
    var relayHandler: ((SosPacket) -> Unit)?
        get() = mesh.relayHandler
        set(value) {
            mesh.relayHandler = value
        }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            if (result == null) return
            externalScope.launch(Dispatchers.Default) {
                processScanResult(result)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            if (results.isNullOrEmpty()) return
            externalScope.launch(Dispatchers.Default) {
                results.forEach { processScanResult(it) }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            isScanning = false
            BleDiagnosticsTracker.setScanning(false)
            val errorMsg = when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "ALREADY_STARTED (Code 1)"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "REGISTRATION_FAILED (Code 2)"
                SCAN_FAILED_INTERNAL_ERROR -> "INTERNAL_ERROR (Code 3)"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "FEATURE_UNSUPPORTED (Code 4)"
                else -> "Error Code $errorCode"
            }
            BleDiagnosticsTracker.recordEvent("NARI_BLE_SCAN_FAILURE", "Scan failed: $errorMsg", isError = true)
            Log.e(TAG, "BLE Scan failed with error code: $errorMsg")

            // Auto-fallback: If hardware filtering fails, restart without filters
            if (!fallbackScanAttempted) {
                fallbackScanAttempted = true
                BleDiagnosticsTracker.recordEvent("NARI_BLE_SCAN_START", "Auto-recovering: restarting scanner without hardware filters...")
                startScanWithoutFilters()
            }
        }
    }

    private fun processScanResult(result: ScanResult?) {
        if (result == null) return
        val scanRecord = result.scanRecord ?: return

        // 1. Try 16-bit Service Data via Android API
        var payloadBytes = scanRecord.getServiceData(Constants.SOS_PARCEL_UUID)

        // 2. Try 128-bit Legacy Service Data
        if (payloadBytes == null) {
            payloadBytes = scanRecord.getServiceData(ParcelUuid(Constants.SOS_LEGACY_128_UUID))
        }

        // 3. Try Manufacturer Data fallback
        if (payloadBytes == null) {
            payloadBytes = scanRecord.getManufacturerSpecificData(Constants.MANUFACTURER_ID)
        }

        // 4. Scan all available service data entries if UUID variation occurred
        if (payloadBytes == null && scanRecord.serviceData != null) {
            for ((uuid, data) in scanRecord.serviceData) {
                if (data == null) continue
                if (uuid.uuid.toString().contains("fde1", ignoreCase = true) ||
                    data.size >= PacketCodec.LEGACY_PACKET_SIZE_BYTES
                ) {
                    payloadBytes = data
                    break
                }
            }
        }

        // 5. Deep Raw-Byte Fallback: Search raw advertisement frame for 0xFDE1 or packet magic
        if (payloadBytes == null && scanRecord.bytes != null) {
            payloadBytes = extractPayloadFromRawBytes(scanRecord.bytes)
        }

        if (payloadBytes == null) return

        val packet = PacketCodec.decode(payloadBytes)
        if (packet == null) {
            BleDiagnosticsTracker.incrementRejectedPackets()
            val deviceAddress = try {
                if (PermissionHelper.hasBluetoothConnectPermission(context)) {
                    result.device?.address ?: "unknown"
                } else {
                    "unknown"
                }
            } catch (_: SecurityException) {
                "unknown"
            }
            BleDiagnosticsTracker.recordEvent(
                "NARI_BLE_PACKET_REJECTED",
                "Failed to decode packet of length ${payloadBytes.size} bytes from $deviceAddress"
            )
            return
        }

        // Shared mesh pipeline: dedup/TTL/budget/rate-limits apply across BLE + WiFi.
        mesh.ingest(packet, result.rssi, MeshTransport.BLE)
    }

    /**
     * Injects a simulated packet directly into the scanner pipeline.
     * Emulator development ONLY: must never be reachable from production UI —
     * injected SOS packets schedule REAL mesh relays and REAL alerts.
     * For safe on-air demos use FLAG_TEST broadcasts (never relayed, never
     * bannered) instead.
     */
    fun injectSimulatedPacket(packet: SosPacket, rssi: Int = -65) {
        mesh.ingest(packet, rssi, MeshTransport.BLE)
    }

    /**
     * Resilient raw AD structure parser.
     * Walks byte-by-byte through raw advertisement array looking for 16-bit Service Data (0x16)
     * with UUID 0xFDE1, Manufacturer Data (0xFF), or magic byte header 0x53 0x01.
     */
    private fun extractPayloadFromRawBytes(raw: ByteArray): ByteArray? {
        var index = 0
        while (index < raw.size) {
            val length = raw[index].toInt() and 0xFF
            if (length == 0) break // 0 indicates end of AD structures
            if (index + 1 + length > raw.size) break

            val type = raw[index + 1].toInt() and 0xFF
            val dataStartIndex = index + 2
            val dataLength = length - 1

            // 0x16 = Service Data (16-bit UUID)
            if (type == 0x16 && dataLength >= 2 + PacketCodec.LEGACY_PACKET_SIZE_BYTES) {
                val uuid0 = raw[dataStartIndex].toInt() and 0xFF
                val uuid1 = raw[dataStartIndex + 1].toInt() and 0xFF
                // 0xFDE1 is [0xE1, 0xFD] in little-endian
                if (uuid0 == 0xE1 && uuid1 == 0xFD) {
                    val payload = ByteArray(dataLength - 2)
                    System.arraycopy(raw, dataStartIndex + 2, payload, 0, payload.size)
                    return payload
                }
            } else if (type == 0xFF && dataLength >= 2 + PacketCodec.LEGACY_PACKET_SIZE_BYTES) {
                // 0xFF = Manufacturer Data
                val company0 = raw[dataStartIndex].toInt() and 0xFF
                val company1 = raw[dataStartIndex + 1].toInt() and 0xFF
                if (company0 == 0xFF && company1 == 0xFF) {
                    val payload = ByteArray(dataLength - 2)
                    System.arraycopy(raw, dataStartIndex + 2, payload, 0, payload.size)
                    return payload
                }
            }
            index += 1 + length
        }

        // Direct pattern fallback: scan for magic byte 0x53 ('S') followed by version 0x01 or 0x02
        for (i in 0..(raw.size - PacketCodec.PACKET_SIZE_BYTES)) {
            if (raw[i] == PacketCodec.MAGIC_BYTE &&
                (raw[i + 1] == PacketCodec.PROTOCOL_VERSION_V1 || raw[i + 1] == PacketCodec.PROTOCOL_VERSION_V2)
            ) {
                val candidate = ByteArray(PacketCodec.PACKET_SIZE_BYTES)
                System.arraycopy(raw, i, candidate, 0, candidate.size)
                return candidate
            }
        }
        return null
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (isScanning) {
            Log.d(TAG, "BLE scan already active")
            return
        }

        if (!PermissionHelper.hasBluetoothScanPermission(context)) {
            val msg = "Missing BLUETOOTH_SCAN permission"
            BleDiagnosticsTracker.recordEvent("NARI_BLE_SCAN_FAILURE", msg, isError = true)
            Log.w(TAG, msg)
            return
        }

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            val msg = "Bluetooth is disabled or unavailable for scanning"
            BleDiagnosticsTracker.recordEvent("NARI_BLE_SCAN_FAILURE", msg, isError = true)
            Log.w(TAG, msg)
            return
        }

        scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            val msg = "BLE Scanner not available on this device"
            BleDiagnosticsTracker.recordEvent("NARI_BLE_SCAN_FAILURE", msg, isError = true)
            Log.e(TAG, msg)
            return
        }

        fallbackScanAttempted = false

        if (BleDiagnosticsTracker.debugScanModeEnabled.value) {
            startScanWithoutFilters()
        } else {
            startScanWithFilters()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScanWithFilters() {
        // The advertiser ONLY emits Service Data (AD type 0x16) under SOS_SERVICE_UUID.
        // A ScanFilter.setServiceUuid() matches against the Service-UUID-LIST ADs
        // (0x03/0x07), which our advertisements do NOT carry — so a UUID filter
        // matches nothing on devices where the filter runs in software, silently
        // killing reception. Use setServiceData(uuid, null) which matches the
        // actual AD structure we transmit (any payload under this service-data UUID).
        val filters = listOf(
            ScanFilter.Builder().setServiceData(Constants.SOS_PARCEL_UUID, null).build()
        )

        val settingsBuilder = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setReportDelay(0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            settingsBuilder.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            settingsBuilder.setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
        }

        try {
            scanner?.startScan(filters, settingsBuilder.build(), scanCallback)
            isScanning = true
            BleDiagnosticsTracker.setScanning(true)
            BleDiagnosticsTracker.recordEvent("NARI_BLE_SCAN_START", "BLE Scanner started with Service-Data UUID filter (FDE1)")
            Log.i(TAG, "BLE Scanner started with service-data filter")
        } catch (e: Exception) {
            BleDiagnosticsTracker.recordEvent("NARI_BLE_SCAN_FAILURE", "Hardware filter start failed: ${e.message}. Falling back...", isError = true)
            fallbackScanAttempted = true
            startScanWithoutFilters()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScanWithoutFilters() {
        val settingsBuilder = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setReportDelay(0)

        try {
            scanner?.startScan(emptyList(), settingsBuilder.build(), scanCallback)
            isScanning = true
            BleDiagnosticsTracker.setScanning(true)
            BleDiagnosticsTracker.recordEvent("NARI_BLE_SCAN_START", "BLE Scanner active in Debug Mode (software filtering enabled)")
            Log.i(TAG, "BLE Scanner started without hardware filters (Debug software filtering)")
        } catch (e: Exception) {
            isScanning = false
            BleDiagnosticsTracker.setScanning(false)
            BleDiagnosticsTracker.recordEvent("NARI_BLE_SCAN_FAILURE", "Software filter scan failed: ${e.message}", isError = true)
            Log.e(TAG, "Scan without filters failed", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        if (!isScanning) return
        try {
            scanner?.stopScan(scanCallback)
            BleDiagnosticsTracker.recordEvent("NARI_BLE_SCAN_START", "BLE Scanner stopped")
            Log.i(TAG, "BLE Scanner stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Exception stopping BLE scan", e)
        } finally {
            isScanning = false
            BleDiagnosticsTracker.setScanning(false)
        }
    }

    fun restartScanning() {
        stopScanning()
        startScanning()
    }

    companion object {
        private const val TAG = "BleScanner"
    }
}
