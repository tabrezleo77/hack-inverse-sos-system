package com.brokencoders.narisuraksha.trigger

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Shake detector that uses the accelerometer.
 * Triggers when 3 spikes > 2.7g occur within a 2-second window, with a 3-second cooldown.
 * Runs on a dedicated background HandlerThread at SENSOR_DELAY_UI to keep the UI thread completely stutter-free.
 */
class ShakeDetector(
    context: Context,
    val engine: ShakeDetectionEngine = ShakeDetectionEngine()
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var sensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null

    private val _shakeEvents = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val shakeEvents: Flow<Unit> = _shakeEvents.asSharedFlow()

    val diagnostics: StateFlow<ShakeDiagnostics> get() = engine.diagnostics

    val isSensorAvailable: Boolean get() = accelerometer != null && sensorManager != null
    var isListening: Boolean = false
        private set

    init {
        engine.updateListeningState(isListening = false, isSensorAvailable = isSensorAvailable)
    }

    @Synchronized
    fun start() {
        if (isListening || !isSensorAvailable) {
            engine.updateListeningState(isListening = isListening, isSensorAvailable = isSensorAvailable)
            return
        }

        try {
            val thread = HandlerThread("ShakeDetectorThread").apply { start() }
            sensorThread = thread
            val handler = Handler(thread.looper)
            sensorHandler = handler

            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI, handler)
            isListening = true
            engine.updateListeningState(isListening = true, isSensorAvailable = true)
            Log.d(TAG, "ShakeDetector started on background thread with SENSOR_DELAY_UI")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ShakeDetector", e)
            // registerListener threw after the thread was started — quit it to
            // avoid leaking a HandlerThread on every failed start.
            try {
                sensorThread?.quitSafely()
            } catch (_: Exception) {
                // ignore teardown failures
            }
            sensorThread = null
            sensorHandler = null
        }
    }

    @Synchronized
    fun stop() {
        if (!isListening) return
        try {
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering sensor listener: ${e.message}")
        }
        sensorThread?.quitSafely()
        sensorThread = null
        sensorHandler = null
        isListening = false
        engine.reset()
        engine.updateListeningState(isListening = false, isSensorAvailable = isSensorAvailable)
        Log.d(TAG, "ShakeDetector stopped")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isListening) return
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        if (event.values.size < 3) return

        val now = System.currentTimeMillis()
        val triggered = engine.processAcceleration(
            x = event.values[0],
            y = event.values[1],
            z = event.values[2],
            timestampMs = now
        )

        if (triggered) {
            Log.i(TAG, "Shake pattern detected! Triggering SOS countdown.")
            _shakeEvents.tryEmit(Unit)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val TAG = "ShakeDetector"
    }
}
