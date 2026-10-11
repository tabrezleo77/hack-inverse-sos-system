package com.brokencoders.narisuraksha.trigger

import com.brokencoders.narisuraksha.core.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

data class ShakeDiagnostics(
    val isListening: Boolean = false,
    val isSensorAvailable: Boolean = false,
    val currentGForce: Float = 0f,
    val peakGForce: Float = 0f,
    val spikeCount: Int = 0,
    val lastSpikeTimestamp: Long = 0L,
    val isCooldownActive: Boolean = false
)

/**
 * Pure, testable algorithm engine for accelerometer shake detection.
 * Separated from Android SensorManager to enable fast, deterministic unit testing.
 *
 * Implements:
 * - Configurable acceleration threshold (default 2.7g)
 * - Minimum spike separation / debouncing (default 220ms) to filter table bumps & drops
 * - Sliding window for required spikes (default 3 spikes within 2000ms)
 * - Post-trigger cooldown (default 3000ms)
 */
class ShakeDetectionEngine(
    val thresholdG: Float = Constants.SHAKE_THRESHOLD_G,
    val requiredSpikes: Int = Constants.SHAKE_REQUIRED_SPIKES,
    val windowMs: Long = Constants.SHAKE_SPIKE_WINDOW_MS,
    val cooldownMs: Long = Constants.SHAKE_COOLDOWN_MS,
    val minIntervalMs: Long = Constants.SHAKE_MIN_SPIKE_INTERVAL_MS
) {

    private val spikeTimestamps = ArrayDeque<Long>()
    private var lastTriggerTimestamp: Long = 0L
    private var peakGForce: Float = 0f

    private val _diagnostics = MutableStateFlow(ShakeDiagnostics())
    val diagnostics: StateFlow<ShakeDiagnostics> = _diagnostics.asStateFlow()

    private var lastDiagnosticsUpdateMs: Long = 0L

    fun updateListeningState(isListening: Boolean, isSensorAvailable: Boolean) {
        _diagnostics.value = _diagnostics.value.copy(
            isListening = isListening,
            isSensorAvailable = isSensorAvailable
        )
    }

    /**
     * Ingests a 3-axis accelerometer sample in m/s^2.
     * Returns true if a valid shake pattern triggers an SOS.
     * Synchronized: called on the sensor HandlerThread while reset() runs on main.
     */
    @Synchronized
    fun processAcceleration(x: Float, y: Float, z: Float, timestampMs: Long): Boolean {
        // Standard Earth gravity (9.80665 m/s^2 = 1.0g)
        val acceleration = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val gForce = acceleration / 9.80665f

        if (gForce > peakGForce) {
            peakGForce = gForce
        }

        val inCooldown = lastTriggerTimestamp > 0L && ((timestampMs - lastTriggerTimestamp) < cooldownMs)

        // Evict expired spikes outside sliding window
        while (spikeTimestamps.isNotEmpty() && (timestampMs - spikeTimestamps.first() > windowMs)) {
            spikeTimestamps.removeFirst()
        }

        var triggered = false

        if (!inCooldown && gForce >= thresholdG) {
            // Debounce: verify minimum interval since the last recorded spike
            // This prevents a single sharp physical shock (e.g. dropping phone on table)
            // from registering multiple consecutive spikes within milliseconds.
            val lastSpike = spikeTimestamps.lastOrNull()
            if (lastSpike == null || (timestampMs - lastSpike >= minIntervalMs)) {
                spikeTimestamps.addLast(timestampMs)

                if (spikeTimestamps.size >= requiredSpikes) {
                    triggered = true
                    lastTriggerTimestamp = timestampMs
                    spikeTimestamps.clear()
                }
            }
        }

        // Only update StateFlow if state changed (spike, trigger, cooldown) or throttled to 250ms
        val shouldUpdate = (gForce >= thresholdG) || triggered ||
                (inCooldown != _diagnostics.value.isCooldownActive) ||
                (timestampMs - lastDiagnosticsUpdateMs >= 250L)

        if (shouldUpdate) {
            lastDiagnosticsUpdateMs = timestampMs
            _diagnostics.value = _diagnostics.value.copy(
                currentGForce = gForce,
                peakGForce = peakGForce,
                spikeCount = spikeTimestamps.size,
                lastSpikeTimestamp = spikeTimestamps.lastOrNull() ?: _diagnostics.value.lastSpikeTimestamp,
                isCooldownActive = inCooldown || triggered
            )
        }

        return triggered
    }

    @Synchronized
    fun reset() {
        spikeTimestamps.clear()
        lastTriggerTimestamp = 0L
        peakGForce = 0f
        lastDiagnosticsUpdateMs = 0L
        _diagnostics.value = _diagnostics.value.copy(
            currentGForce = 0f,
            peakGForce = 0f,
            spikeCount = 0,
            lastSpikeTimestamp = 0L,
            isCooldownActive = false
        )
    }
}
