package com.brokencoders.narisuraksha.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.brokencoders.narisuraksha.NariSurakshaApp
import com.brokencoders.narisuraksha.core.Constants
import com.brokencoders.narisuraksha.data.SosEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ScanForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ScanForegroundService onCreate")

        val notificationHelper = (application as NariSurakshaApp).notificationHelper
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var fgsType = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                // BLE scan results are location-derived: include location type when granted
                // to satisfy API 34+ FGS type enforcement.
                if (com.brokencoders.narisuraksha.core.PermissionHelper.hasLocationPermission(this)) {
                    fgsType = fgsType or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                }
                startForeground(
                    Constants.NOTIFICATION_ID_SERVICE,
                    notificationHelper.buildScanServiceNotification(),
                    fgsType
                )
            } else {
                startForeground(Constants.NOTIFICATION_ID_SERVICE, notificationHelper.buildScanServiceNotification())
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground with type failed: ${e.message}", e)
            try {
                startForeground(Constants.NOTIFICATION_ID_SERVICE, notificationHelper.buildScanServiceNotification())
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback startForeground also failed: ${e2.message}", e2)
            }
        }

        acquireWakeLock()
        startScanning()
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NariSuraksha::ScanWakeLock")?.apply {
                acquire(10 * 60 * 1000L) // 10 minutes timeout per cycle
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire WakeLock: ${e.message}")
        }
    }

    private fun startScanning() {
        val app = application as NariSurakshaApp
        val bleTransport = app.bleTransport
        val sosDao = app.database.sosDao()
        val notificationHelper = app.notificationHelper

        // WiFi mesh (primary long-range medium) listens alongside BLE.
        // It idles silently when WiFi is unjoined — never a crash path.
        try {
            app.wifiMesh.start()
        } catch (e: Exception) {
            Log.w(TAG, "WiFi mesh listener failed: ${e.message}")
        }

        if (!com.brokencoders.narisuraksha.core.PermissionHelper.hasBluetoothPermissions(this)) {
            Log.w(TAG, "Guardian BLE scan skipped: Bluetooth permissions missing (WiFi mesh continues)")
            return
        }

        try {
            bleTransport.startScanning()
        } catch (e: SecurityException) {
            Log.e(TAG, "Guardian scan aborted: missing BLE permission", e)
            return
        }

        serviceScope.launch {
            bleTransport.received.collectLatest { receivedSos ->
                // Non-emergency TEST beacons stay in diagnostics only: persisting
                // them or notifying would manufacture fake SOS emergencies.
                if (receivedSos.packet.isTest) {
                    Log.d(TAG, "TEST beacon from sender ${receivedSos.packet.senderId} kept out of DB/notifications")
                    return@collectLatest
                }
                // ACK packets are responder acknowledgements, NOT new SOS emergencies.
                // Persisting them as TYPE_RECEIVED and firing the heads-up alert would
                // mislabel an acknowledgement as a fresh distress beacon and pollute
                // the history DB. ACKs are surfaced through MainViewModel.responderAckCount
                // (and a dedicated "responder acknowledged" notification) instead.
                if (receivedSos.packet.isAck) {
                    Log.i(TAG, "ACK from responder #${receivedSos.packet.senderId} for target #${receivedSos.packet.targetSenderId} — not a new SOS, skipping DB insert/emergency notification")
                    return@collectLatest
                }
                Log.i(TAG, "Handling received SOS event from sender: ${receivedSos.packet.senderId}")

                // Save to Room Database
                val entity = SosEventEntity(
                    eventType = SosEventEntity.TYPE_RECEIVED,
                    senderId = receivedSos.packet.senderId,
                    timestamp = (receivedSos.packet.timestamp.toLong() * 1000L),
                    lat = if (receivedSos.packet.isLocationUnavailable) null else receivedSos.packet.lat.toDouble(),
                    lon = if (receivedSos.packet.isLocationUnavailable) null else receivedSos.packet.lon.toDouble(),
                    audioPath = null,
                    rssi = receivedSos.rssi,
                    isLocationUnavailable = receivedSos.packet.isLocationUnavailable,
                    createdAt = receivedSos.receivedAt
                )
                sosDao.insertEvent(entity)

                // Show High Priority Heads-up notification
                notificationHelper.showReceivedSosAlert(receivedSos)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SCAN) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Re-ensure the transport-level scan is active when the service is
        // (re)started. onCreate already calls startScanning() (which also
        // launches the received-event collector); here we only restart the
        // BLE/WiFi transport scan so a subsequent start() delivery (e.g. from
        // MainActivity.onResume or ensureGuardianScanActive) revives a dead
        // scan without launching a duplicate collector. BleScanner.startScanning
        // is a no-op if already scanning.
        try {
            val app = application as NariSurakshaApp
            if (com.brokencoders.narisuraksha.core.PermissionHelper.hasBluetoothPermissions(this)) {
                app.bleTransport.startScanning()
            }
            app.wifiMesh.start()
        } catch (e: Exception) {
            Log.w(TAG, "Scan restart from onStartCommand failed: ${e.message}")
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "ScanForegroundService onDestroy")
        try {
            val app = application as NariSurakshaApp
            app.bleTransport.stopScanning()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan in service onDestroy", e)
        }
        try {
            (application as NariSurakshaApp).wifiMesh.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping WiFi mesh in onDestroy: ${e.message}")
        }
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        serviceScope.cancel()
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {
            // Pre-O fallback handled by stopSelf below
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "ScanForegroundService"
        const val ACTION_START_SCAN = "com.brokencoders.narisuraksha.START_SCAN"
        const val ACTION_STOP_SCAN = "com.brokencoders.narisuraksha.STOP_SCAN"

        fun start(context: Context) {
            try {
                val intent = Intent(context, ScanForegroundService::class.java).apply {
                    action = ACTION_START_SCAN
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start ScanForegroundService: ${e.message}", e)
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, ScanForegroundService::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop ScanForegroundService: ${e.message}", e)
            }
        }
    }
}
