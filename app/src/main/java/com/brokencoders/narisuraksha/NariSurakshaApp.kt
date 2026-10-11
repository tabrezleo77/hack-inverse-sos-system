package com.brokencoders.narisuraksha

import android.app.Application
import android.util.Log
import com.brokencoders.narisuraksha.ble.BleAdvertiser
import com.brokencoders.narisuraksha.ble.BleScanner
import com.brokencoders.narisuraksha.ble.BleTransport
import com.brokencoders.narisuraksha.ble.BleTransportImpl
import com.brokencoders.narisuraksha.ble.MeshCore
import com.brokencoders.narisuraksha.capture.AudioRecorder
import com.brokencoders.narisuraksha.capture.LocationProvider
import com.brokencoders.narisuraksha.core.DeviceIdProvider
import com.brokencoders.narisuraksha.core.PermissionHelper
import com.brokencoders.narisuraksha.data.AppDatabase
import com.brokencoders.narisuraksha.data.UserPreferencesRepository
import com.brokencoders.narisuraksha.service.NotificationHelper
import com.brokencoders.narisuraksha.service.ScanForegroundService
import com.brokencoders.narisuraksha.trigger.ShakeDetector
import com.brokencoders.narisuraksha.trigger.SosManager
import com.brokencoders.narisuraksha.wifi.WifiMeshTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NariSurakshaApp : Application() {

    private val applicationScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this) }
    val emergencyContactDao by lazy { database.emergencyContactDao() }
    val preferencesRepository by lazy { UserPreferencesRepository(this) }
    val deviceIdProvider by lazy { DeviceIdProvider(this) }
    val audioRecorder by lazy { AudioRecorder(this) }
    val locationProvider by lazy { LocationProvider(this) }
    val sosManager by lazy { SosManager(this, audioRecorder, locationProvider, applicationScope) }

    val bleAdvertiser by lazy { BleAdvertiser(this, applicationScope) }

    /**
     * Single shared mesh pipeline for BLE + WiFi. One dedup cache, one TTL
     * policy, one budget — a broadcast heard on both media is one event.
     */
    val meshCore by lazy { MeshCore(deviceIdProvider.deviceId, applicationScope) }
    val bleScanner by lazy { BleScanner(this, deviceIdProvider, applicationScope, meshCore) }
    val bleTransport: BleTransport by lazy { BleTransportImpl(bleAdvertiser, bleScanner) }

    /** Primary long-range medium. Idles silently when WiFi is unavailable. */
    val wifiMesh by lazy { WifiMeshTransport(this, meshCore, applicationScope) }

    val shakeDetector by lazy { ShakeDetector(this) }
    val notificationHelper by lazy { NotificationHelper(this) }

    override fun onCreate() {
        super.onCreate()
        // Eagerly create notification channels on the app thread so the first
        // foreground service start never pays channel-creation cost inside its
        // ~10s startForeground timeout window.
        try {
            notificationHelper
        } catch (e: Exception) {
            Log.w("NariSurakshaApp", "Notification channel init failed: ${e.message}")
        }
        Log.i("NariSurakshaApp", "Application initialized with Anonymous Device ID: ${deviceIdProvider.deviceId}")

        // Mesh relay fan-out: every relay transmits on BOTH media.
        // WiFi is the primary long-range carrier; BLE is the always-on fallback.
        // Wired once here (never in BleTransportImpl) so init order cannot drop it.
        bleScanner.relayHandler = { packet ->
            try {
                bleAdvertiser.queueRelay(packet)
            } catch (e: Exception) {
                Log.w("NariSurakshaApp", "BLE relay enqueue failed: ${e.message}")
            }
            try {
                wifiMesh.broadcast(packet)
            } catch (e: Exception) {
                Log.w("NariSurakshaApp", "WiFi relay broadcast failed: ${e.message}")
            }
        }

        // Initialize Background Guardian Scanner if enabled
        applicationScope.launch {
            val isScanEnabled = preferencesRepository.isGuardianScanEnabled.first()
            if (isScanEnabled && PermissionHelper.hasBluetoothPermissions(this@NariSurakshaApp)) {
                try {
                    ScanForegroundService.start(this@NariSurakshaApp)
                } catch (e: Exception) {
                    Log.w("NariSurakshaApp", "Could not start ScanForegroundService at launch: ${e.message}")
                }
            }
        }
    }
}
