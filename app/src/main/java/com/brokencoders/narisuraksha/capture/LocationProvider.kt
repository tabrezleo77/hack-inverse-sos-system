package com.brokencoders.narisuraksha.capture

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.brokencoders.narisuraksha.core.PermissionHelper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

data class Coordinates(
    val lat: Double,
    val lon: Double,
    val accuracy: Float? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(!lat.isNaN() && !lon.isNaN()) { "Coordinates cannot contain NaN" }
        require(!lat.isInfinite() && !lon.isInfinite()) { "Coordinates cannot be infinite" }
        require(lat in -90.0..90.0) { "Latitude out of range: $lat" }
        require(lon in -180.0..180.0) { "Longitude out of range: $lon" }
    }

    /**
     * A coordinate is usable only if it isn't the Null Island (0,0) sentinel.
     * A real fix at exactly (0,0) is practically impossible in our operating region
     * and is overwhelmingly reported by GPS receivers that haven't locked yet.
     */
    val isUsable: Boolean
        get() = !(lat == 0.0 && lon == 0.0)
}

/**
 * A single validated position fix with honest precision metadata.
 * The UI and SOS messages MUST surface [accuracyM] and [ageMs] — never
 * display coordinates without their estimated accuracy.
 */
data class LocationFix(
    val lat: Double,
    val lon: Double,
    /** Estimated 68% confidence radius in meters, null when unknown. */
    val accuracyM: Float?,
    val timestamp: Long = System.currentTimeMillis(),
    /** "gps" (GNSS), "network", "fused", "rtt"/"uwb" (future ranging), "cached". */
    val provider: String = "fused"
) {
    fun ageMs(now: Long = System.currentTimeMillis()): Long = (now - timestamp).coerceAtLeast(0L)

    /** Quality tier driving UI badges and SOS inclusion policy. */
    fun quality(now: Long = System.currentTimeMillis()): FixQuality {
        val age = ageMs(now)
        if (age > STALE_AGE_MS) return FixQuality.STALE
        val acc = accuracyM
        return when {
            acc != null && acc <= HIGH_ACCURACY_M && age <= FRESH_AGE_MS -> FixQuality.HIGH
            acc != null && acc <= MEDIUM_ACCURACY_M && age <= USABLE_AGE_MS -> FixQuality.MEDIUM
            else -> FixQuality.LOW
        }
    }

    /** "±12 m · GPS · 5 s ago" — or "No fix" semantics via null callers. */
    fun describe(now: Long = System.currentTimeMillis()): String {
        val accText = accuracyM?.let { "±${it.toInt()} m" } ?: "accuracy unknown"
        val ageS = ageMs(now) / 1000
        val ageText = when {
            ageS < 5 -> "just now"
            ageS < 60 -> "$ageS s ago"
            else -> "${ageS / 60} min ago"
        }
        return "$accText · ${provider.uppercase()} · $ageText"
    }

    fun mapsLink(): String = "https://maps.google.com/?q=$lat,$lon"

    fun toCoordinates(): Coordinates = Coordinates(
        lat = lat,
        lon = lon,
        accuracy = accuracyM,
        timestamp = timestamp
    )

    companion object {
        const val HIGH_ACCURACY_M = 15f
        const val MEDIUM_ACCURACY_M = 50f
        const val FRESH_AGE_MS = 15_000L
        const val USABLE_AGE_MS = 60_000L
        const val STALE_AGE_MS = 120_000L
    }
}

enum class FixQuality {
    HIGH,
    MEDIUM,
    LOW,
    STALE
}

interface LocationCoordinateProvider {
    suspend fun getCurrentLocation(): Coordinates?
    val currentCoordinates: StateFlow<Coordinates?>

    /** Latest continuous-tracking fix (null until the first fix arrives). */
    val latestFix: StateFlow<LocationFix?>
        get() = MutableStateFlow(null)

    /** Begins high-accuracy repeated updates; safe to call repeatedly. */
    fun startTracking() {}

    /** Stops repeated updates. */
    fun stopTracking() {}
}

/**
 * Offline-friendly location provider with precise real-time tracking.
 *
 * - Continuous GNSS tracking ([startTracking]): HIGH_ACCURACY, 5 s interval /
 *   2 s fastest / 5 m displacement. Every fix refreshes [latestFix] and
 *   [currentCoordinates], so SOS always sends the freshest known position.
 * - One-shot chain ([getCurrentLocation]): fresh tracked fix → indoor ranging
 *   (future RTT/UWB) → high-accuracy one-shot → balanced/network one-shot →
 *   last-known → null (callers propagate FLAG_LOCATION_UNAVAILABLE).
 * - Never blocks SOS: every stage has a tight timeout and failures yield null.
 */
class LocationProvider(
    private val context: Context,
    private val rangingProvider: RangingProvider = RangingProvider.NoOp
) : LocationCoordinateProvider {

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val _currentCoordinates = MutableStateFlow<Coordinates?>(null)
    override val currentCoordinates: StateFlow<Coordinates?> = _currentCoordinates.asStateFlow()

    private val _latestFix = MutableStateFlow<LocationFix?>(null)
    override val latestFix: StateFlow<LocationFix?> = _latestFix.asStateFlow()

    private var trackingCallback: LocationCallback? = null
    private var isTracking = false

    @SuppressLint("MissingPermission")
    override fun startTracking() {
        if (isTracking) return
        if (!PermissionHelper.hasLocationPermission(context)) {
            Log.w(TAG, "Tracking not started: location permission missing")
            return
        }
        try {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, TRACK_INTERVAL_MS)
                .setMinUpdateIntervalMillis(TRACK_FASTEST_MS)
                .setMinUpdateDistanceMeters(TRACK_MIN_DISPLACEMENT_M)
                .setMaxUpdateDelayMillis(TRACK_MAX_DELAY_MS)
                .build()
            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    val fix = toFix(loc, "gps")
                    if (fix != null) {
                        _latestFix.value = fix
                        _currentCoordinates.value = fix.toCoordinates()
                    }
                }
            }
            trackingCallback = callback
            fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnFailureListener { e ->
                    Log.w(TAG, "Tracking request failed: ${e.message}")
                    trackingCallback = null
                    isTracking = false
                }
                .addOnSuccessListener {
                    isTracking = true
                    Log.i(TAG, "High-accuracy location tracking started")
                }
        } catch (e: SecurityException) {
            Log.w(TAG, "Tracking blocked: missing location permission", e)
            trackingCallback = null
        } catch (e: Exception) {
            Log.w(TAG, "Tracking start failed: ${e.message}")
            trackingCallback = null
        }
    }

    override fun stopTracking() {
        if (!isTracking && trackingCallback == null) return
        try {
            trackingCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Tracking stop failed: ${e.message}")
        } finally {
            trackingCallback = null
            isTracking = false
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Coordinates? = withContext(Dispatchers.IO) {
        if (!PermissionHelper.hasLocationPermission(context)) {
            Log.w(TAG, "Location permission not granted. Returning null.")
            return@withContext null
        }

        if (!PermissionHelper.isLocationServiceEnabled(context)) {
            Log.w(TAG, "Location Services disabled on device. Trying last known location fallback...")
        }

        // Stage 0: fresh continuous-tracking fix (zero latency, zero extra battery).
        _latestFix.value?.let { fix ->
            if (fix.ageMs() <= LocationFix.FRESH_AGE_MS) {
                _currentCoordinates.value = fix.toCoordinates()
                return@withContext fix.toCoordinates()
            }
        }

        // Stage 1: indoor ranging (RTT/UWB) when a future provider is available.
        try {
            if (rangingProvider.isAvailable) {
                rangingProvider.preciseFix()?.let { fix ->
                    Log.i(TAG, "Using ranging fix: ${fix.describe()}")
                    _latestFix.value = fix
                    _currentCoordinates.value = fix.toCoordinates()
                    return@withContext fix.toCoordinates()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Ranging fix failed, continuing GNSS chain: ${e.message}")
        }

        try {
            // Stage 2: fresh high-accuracy GNSS one-shot (2.5 s budget).
            val fresh = withTimeoutOrNull(2500L) {
                try {
                    val task = fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        null
                    )
                    Tasks.await(task, 2200, TimeUnit.MILLISECONDS)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed getting fresh location: ${e.message}")
                    null
                }
            }

            toFix(fresh, "gps")?.let { fix ->
                Log.i(
                    TAG,
                    "Acquired fresh GPS location: lat=${fix.lat}, lon=${fix.lon}, acc=±${fix.accuracyM?.toInt()}m"
                )
                _latestFix.value = fix
                _currentCoordinates.value = fix.toCoordinates()
                return@withContext fix.toCoordinates()
            }

            // Stage 3: balanced / network one-shot (1.5 s budget) for indoors.
            Log.d(TAG, "Trying balanced-accuracy (network-assisted) fix...")
            val networkLoc: Location? = withTimeoutOrNull(1500L) {
                try {
                    val task = fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        null
                    )
                    Tasks.await(task, 1300, TimeUnit.MILLISECONDS)
                } catch (e: Exception) {
                    Log.w(TAG, "Balanced fix failed: ${e.message}")
                    null
                }
            }

            toFix(networkLoc, "network")?.let { fix ->
                Log.i(TAG, "Using network-assisted fix: ${fix.describe()}")
                _latestFix.value = fix
                _currentCoordinates.value = fix.toCoordinates()
                return@withContext fix.toCoordinates()
            }

            // Stage 4: fast last-known fallback (1.0 s budget).
            Log.d(TAG, "Attempting fast fallback to last known location...")
            val lastLoc: Location? = try {
                Tasks.await(fusedLocationClient.lastLocation, 1000, TimeUnit.MILLISECONDS)
            } catch (e: Exception) {
                Log.w(TAG, "Failed getting last known location: ${e.message}")
                null
            }

            toFix(lastLoc, "cached")?.let { fix ->
                Log.i(
                    TAG,
                    "Using last known GPS location: lat=${fix.lat}, lon=${fix.lon}, acc=±${fix.accuracyM?.toInt()}m"
                )
                _latestFix.value = fix
                _currentCoordinates.value = fix.toCoordinates()
                return@withContext fix.toCoordinates()
            }

            Log.w(TAG, "No usable GPS location available offline. Returning null (FLAG_LOCATION_UNAVAILABLE).")
            null
        } catch (e: SecurityException) {
            Log.w(TAG, "Location request denied (permission revoked mid-flight)", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring location", e)
            null
        }
    }

    /**
     * Converts a raw [Location] into a validated [LocationFix], returning null
     * if the fix is implausible (null, NaN, out of range, or Null Island).
     */
    private fun toFix(loc: Location?, provider: String): LocationFix? {
        if (loc == null) return null

        val lat = loc.latitude
        val lon = loc.longitude

        if (lat.isNaN() || lon.isNaN()) {
            Log.w(TAG, "Rejecting NaN coordinates")
            return null
        }
        if (lat.isInfinite() || lon.isInfinite()) {
            Log.w(TAG, "Rejecting infinite coordinates")
            return null
        }
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) {
            Log.w(TAG, "Rejecting out-of-range coordinates: lat=$lat, lon=$lon")
            return null
        }
        if (lat == 0.0 && lon == 0.0) {
            Log.w(TAG, "Rejecting Null Island (0,0) fix")
            return null
        }

        val accuracy = if (loc.hasAccuracy()) loc.accuracy else null
        val timestamp = if (loc.time > 0) loc.time else System.currentTimeMillis()
        return LocationFix(
            lat = lat,
            lon = lon,
            accuracyM = accuracy,
            timestamp = timestamp,
            provider = provider
        )
    }

    /**
     * Converts a raw [Location] into validated [Coordinates] (legacy path).
     */
    private fun buildCoordinates(loc: Location?): Coordinates? {
        return toFix(loc, "gps")?.toCoordinates()
    }

    companion object {
        private const val TAG = "LocationProvider"
        private const val TRACK_INTERVAL_MS = 5_000L
        private const val TRACK_FASTEST_MS = 2_000L
        private const val TRACK_MIN_DISPLACEMENT_M = 5f
        private const val TRACK_MAX_DELAY_MS = 10_000L
    }
}
