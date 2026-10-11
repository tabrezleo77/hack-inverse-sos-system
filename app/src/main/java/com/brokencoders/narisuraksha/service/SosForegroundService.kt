package com.brokencoders.narisuraksha.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.brokencoders.narisuraksha.NariSurakshaApp
import com.brokencoders.narisuraksha.core.Constants
import com.brokencoders.narisuraksha.core.SosPacket
import com.brokencoders.narisuraksha.data.SosEventEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SosForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SosForegroundService onCreate")
        val notificationHelper = (application as NariSurakshaApp).notificationHelper
        val notification = notificationHelper.buildSosBroadcastingNotification()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var fgsType = 0
                if (com.brokencoders.narisuraksha.core.PermissionHelper.hasBluetoothPermissions(this)) {
                    fgsType = fgsType or ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                }
                if (com.brokencoders.narisuraksha.core.PermissionHelper.hasLocationPermission(this)) {
                    fgsType = fgsType or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    if (com.brokencoders.narisuraksha.core.PermissionHelper.hasAudioPermission(this)) {
                        fgsType = fgsType or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    }
                }
                if (fgsType != 0) {
                    startForeground(Constants.NOTIFICATION_ID_SOS_ACTIVE, notification, fgsType)
                } else {
                    startForeground(Constants.NOTIFICATION_ID_SOS_ACTIVE, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
                }
            } else {
                startForeground(Constants.NOTIFICATION_ID_SOS_ACTIVE, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground with types failed: ${e.message}", e)
            try {
                startForeground(Constants.NOTIFICATION_ID_SOS_ACTIVE, notification)
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback startForeground failed: ${e2.message}", e2)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SOS) {
            stopSelf()
            return START_NOT_STICKY
        }

        val lat = intent?.getDoubleExtra(EXTRA_LAT, Double.NaN).let { if (it == null || it.isNaN()) null else it }
        val lon = intent?.getDoubleExtra(EXTRA_LON, Double.NaN).let { if (it == null || it.isNaN()) null else it }
        val audioPath = intent?.getStringExtra(EXTRA_AUDIO_PATH)

        broadcastSos(lat, lon, audioPath)

        return START_NOT_STICKY
    }

    private fun broadcastSos(lat: Double?, lon: Double?, audioPath: String?) {
        val app = application as NariSurakshaApp
        val deviceId = app.deviceIdProvider.deviceId
        val bleTransport = app.bleTransport
        val sosDao = app.database.sosDao()

        val packet = SosPacket.create(
            senderId = deviceId,
            lat = lat,
            lon = lon
        )

        Log.i(TAG, "Starting BLE broadcast for packet: $packet")
        // Primary medium first: WiFi UDP flood reaches ~50-100m per hop.
        try {
            app.wifiMesh.broadcast(packet)
        } catch (e: Exception) {
            Log.w(TAG, "WiFi SOS broadcast failed (BLE continues): ${e.message}")
        }
        try {
            bleTransport.startAdvertising(packet)
        } catch (e: SecurityException) {
            Log.e(TAG, "BLE advertise aborted: missing BLUETOOTH_ADVERTISE permission (WiFi mesh and SMS continue)", e)
        } catch (e: Exception) {
            Log.e(TAG, "BLE advertise failed: ${e.message} (WiFi mesh and SMS continue)", e)
        }
        // Ensure device is scanning simultaneously so incoming responder ACKs are captured!
        try {
            bleTransport.startScanning()
        } catch (e: SecurityException) {
            Log.w(TAG, "SOS ACK scan unavailable: missing BLUETOOTH_SCAN permission", e)
        }

        serviceScope.launch {
            // Persist SENT event to Room
            val entity = SosEventEntity(
                eventType = SosEventEntity.TYPE_SENT,
                senderId = deviceId,
                timestamp = System.currentTimeMillis(),
                lat = lat,
                lon = lon,
                audioPath = audioPath,
                rssi = null,
                isLocationUnavailable = (lat == null || lon == null),
                createdAt = System.currentTimeMillis()
            )
            sosDao.insertEvent(entity)

            // Live location refresh: while the alert is active, re-broadcast
            // with fresher coordinates when the victim moves (≥25 m) or every
            // 60 s, so responders converge on the current position.
            var lastLat = lat
            var lastLon = lon
            var lastRefresh = System.currentTimeMillis()
            val deadline = lastRefresh + Constants.SOS_ADVERTISE_TIMEOUT_MS
            try {
                // Bounded by the broadcast window: collection ends at the
                // deadline and the service stops itself below.
                kotlinx.coroutines.withTimeoutOrNull((deadline - System.currentTimeMillis()).coerceAtLeast(0L)) {
                    app.locationProvider.latestFix.collect { fix ->
                        if (fix == null) return@collect
                        val now = System.currentTimeMillis()
                    val prevLat = lastLat
                    val prevLon = lastLon
                    val moved = (prevLat == null || prevLon == null) ||
                        haversineM(prevLat, prevLon, fix.lat, fix.lon) >= LIVE_UPDATE_MIN_DISPLACEMENT_M
                    val due = now - lastRefresh >= LIVE_UPDATE_MAX_AGE_MS
                    if ((moved || due) && fix.quality(now) != com.brokencoders.narisuraksha.capture.FixQuality.STALE) {
                        lastLat = fix.lat
                        lastLon = fix.lon
                        lastRefresh = now
                        try {
                            val fresh = SosPacket.create(
                                senderId = deviceId,
                                lat = fix.lat,
                                lon = fix.lon
                            )
                            Log.i(TAG, "Live update: re-broadcasting SOS at ${fix.describe(now)}")
                            bleTransport.stopAdvertising()
                            bleTransport.startAdvertising(fresh)
                            app.wifiMesh.broadcast(fresh)
                        } catch (e: Exception) {
                            Log.w(TAG, "Live location re-broadcast failed: ${e.message}")
                        }
                    }
                }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Live location collector ended: ${e.message}")
            }

            // Auto-stop service after timeout
            Log.i(TAG, "Broadcasting timeout complete. Stopping SosForegroundService.")
            stopSelf()
        }
    }

    private fun haversineM(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return 2 * r * Math.asin(Math.sqrt(a))
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "SosForegroundService onDestroy")
        try {
            val app = application as NariSurakshaApp
            app.bleTransport.stopAdvertising()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping advertising in onDestroy", e)
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
        private const val TAG = "SosForegroundService"
        /** Re-broadcast live position after this displacement. */
        private const val LIVE_UPDATE_MIN_DISPLACEMENT_M = 25.0
        /** Re-broadcast live position at least this often. */
        private const val LIVE_UPDATE_MAX_AGE_MS = 60_000L
        const val ACTION_START_SOS = "com.brokencoders.narisuraksha.START_SOS"
        const val ACTION_STOP_SOS = "com.brokencoders.narisuraksha.STOP_SOS"
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LON = "extra_lon"
        const val EXTRA_AUDIO_PATH = "extra_audio_path"

        fun start(context: Context, lat: Double?, lon: Double?, audioPath: String?) {
            try {
                val intent = Intent(context, SosForegroundService::class.java).apply {
                    action = ACTION_START_SOS
                    lat?.let { putExtra(EXTRA_LAT, it) }
                    lon?.let { putExtra(EXTRA_LON, it) }
                    audioPath?.let { putExtra(EXTRA_AUDIO_PATH, it) }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start SosForegroundService: ${e.message}", e)
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, SosForegroundService::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop SosForegroundService: ${e.message}", e)
            }
        }
    }
}
