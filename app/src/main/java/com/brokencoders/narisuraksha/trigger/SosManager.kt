package com.brokencoders.narisuraksha.trigger

import android.content.Context
import android.util.Log
import com.brokencoders.narisuraksha.capture.AudioEvidenceCapture
import com.brokencoders.narisuraksha.capture.LocationCoordinateProvider
import com.brokencoders.narisuraksha.core.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages the SOS lifecycle:
 * Trigger -> 3s Countdown -> Capture (Audio + GPS) -> Confirmed SOS Broadcast
 */
class SosManager(
    private val context: Context? = null,
    private val audioRecorder: AudioEvidenceCapture,
    private val locationProvider: LocationCoordinateProvider,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val countdownStepDelayMs: Long = 1000L
) : SosTrigger {

    private val _events = MutableSharedFlow<SosEvent>(
        replay = 1,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val events: Flow<SosEvent> = _events.asSharedFlow()

    private val _countdownSeconds = MutableStateFlow(Constants.COUNTDOWN_DURATION_SECONDS)
    val countdownSeconds: StateFlow<Int> = _countdownSeconds.asStateFlow()

    private val _isCountingDown = MutableStateFlow(false)
    val isCountingDown: StateFlow<Boolean> = _isCountingDown.asStateFlow()

    private val _isSosActive = MutableStateFlow(false)
    val isSosActive: StateFlow<Boolean> = _isSosActive.asStateFlow()

    private var countdownJob: Job? = null
    private var confirmedEvent: SosEvent.Confirmed? = null

    @Synchronized
    fun triggerSos() {
        if (_isCountingDown.value || _isSosActive.value) {
            Log.d(TAG, "SOS trigger ignored: already in progress")
            return
        }

        Log.i(TAG, "SOS countdown initiated!")
        _isCountingDown.value = true
        _countdownSeconds.value = Constants.COUNTDOWN_DURATION_SECONDS
        _events.tryEmit(SosEvent.CountdownStarted)

        countdownJob?.cancel()
        countdownJob = externalScope.launch {
            for (i in Constants.COUNTDOWN_DURATION_SECONDS downTo 1) {
                _countdownSeconds.value = i
                delay(countdownStepDelayMs)
            }
            // Cancel at T-0 must still abort: confirmSos does suspend audio+GPS
            // work that would otherwise broadcast despite the user's cancel.
            if (!_isCountingDown.value) {
                Log.d(TAG, "SOS confirm aborted: countdown was cancelled")
                return@launch
            }
            _countdownSeconds.value = 0
            _isCountingDown.value = false
            _isSosActive.value = true

            // Confirm SOS, capture audio & location
            confirmSos()
        }
    }

    fun cancelCountdown() {
        if (!_isCountingDown.value) return

        Log.i(TAG, "SOS countdown cancelled by user (False alarm).")
        countdownJob?.cancel()
        countdownJob = null
        _isCountingDown.value = false
        _countdownSeconds.value = Constants.COUNTDOWN_DURATION_SECONDS
        _events.tryEmit(SosEvent.Cancelled)
    }

    private suspend fun confirmSos() {
        Log.i(TAG, "SOS Confirmed! Capturing location and starting audio evidence recorder...")
        
        // 1. Start audio recording
        val audioPath = audioRecorder.startRecording()

        // 2. Fetch offline GPS location
        val location = locationProvider.getCurrentLocation()

        val event = SosEvent.Confirmed(
            audioPath = audioPath,
            lat = location?.lat,
            lon = location?.lon
        )
        confirmedEvent = event
        _events.emit(event)
    }

    fun stopSos() {
        Log.i(TAG, "Stopping active SOS broadcast.")
        _isSosActive.value = false
        _isCountingDown.value = false
        countdownJob?.cancel()
        countdownJob = null
        try {
            audioRecorder.stopRecording()
        } catch (e: Exception) {
            Log.w(TAG, "Audio stop failed during SOS shutdown: ${e.message}")
        }
        confirmedEvent = null
    }

    companion object {
        private const val TAG = "SosManager"
    }
}
