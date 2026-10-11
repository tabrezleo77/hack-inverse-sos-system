package com.brokencoders.narisuraksha.ui.viewmodels

import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.brokencoders.narisuraksha.data.SosDao
import com.brokencoders.narisuraksha.data.SosEventEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class HistoryViewModel(
    private val sosDao: SosDao,
    private val audioDir: File? = null
) : ViewModel() {

    val events: StateFlow<List<SosEventEntity>> = sosDao.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _currentlyPlayingAudioPath = MutableStateFlow<String?>(null)
    val currentlyPlayingAudioPath: StateFlow<String?> = _currentlyPlayingAudioPath.asStateFlow()

    /** One-shot user-visible playback errors, consumed by the UI as a Toast. */
    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    fun clearPlaybackError() {
        _playbackError.value = null
    }

    private var mediaPlayer: MediaPlayer? = null

    fun playAudio(path: String) {
        val file = File(path)
        if (!file.exists()) {
            Log.w(TAG, "Audio file not found at: $path")
            _playbackError.value = "Recording file is missing from storage."
            return
        }
        // 0-byte/corrupt captures from older builds can never decode — say so
        // instead of silently doing nothing (the old "not playing" symptom).
        if (file.length() < MIN_PLAYABLE_BYTES) {
            Log.w(TAG, "Audio file too small/corrupt (${file.length()}B): $path")
            _playbackError.value = "This recording is corrupted (0 bytes) and cannot play."
            return
        }

        if (_currentlyPlayingAudioPath.value == path) {
            stopAudio()
            return
        }

        stopAudio()

        try {
            val player = MediaPlayer()
            mediaPlayer = player
            player.setOnPreparedListener { it.start() }
            player.setOnCompletionListener {
                _currentlyPlayingAudioPath.value = null
                releasePlayer()
            }
            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error what=$what extra=$extra for $path")
                _playbackError.value = "Could not play this recording on this device."
                stopAudio()
                true
            }
            player.setDataSource(path)
            // Async: never blocks the UI thread on large/corrupt files.
            player.prepareAsync()
            _currentlyPlayingAudioPath.value = path
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio evidence", e)
            _playbackError.value = "Could not play this recording."
            stopAudio()
        }
    }

    private fun releasePlayer() {
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing media player", e)
        } finally {
            mediaPlayer = null
        }
    }

    fun stopAudio() {
        try {
            try {
                mediaPlayer?.stop()
            } catch (_: IllegalStateException) {
                // Not started (prepare failed / already stopped) — release only.
            }
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        } finally {
            mediaPlayer = null
            _currentlyPlayingAudioPath.value = null
        }
    }

    fun deleteEvent(event: SosEventEntity) {
        viewModelScope.launch {
            if (event.audioPath != null) {
                try {
                    File(event.audioPath).delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Could not delete audio file", e)
                }
            }
            sosDao.deleteEvent(event)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            stopAudio()
            // Delete all audio evidence files from disk
            events.value.forEach { event ->
                event.audioPath?.let { path ->
                    try {
                        File(path).delete()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to delete audio file: $path", e)
                    }
                }
            }
            try {
                audioDir?.listFiles()?.forEach { file ->
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear audio directory", e)
            }
            sosDao.clearAll()
            Log.i(TAG, "All incident records and audio evidence purged from device")
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
    }

    companion object {
        private const val TAG = "HistoryViewModel"

        /** Below this size a .m4a cannot contain a valid header + samples. */
        private const val MIN_PLAYABLE_BYTES = 1024L

        fun provideFactory(sosDao: SosDao, audioDir: File? = null): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HistoryViewModel(sosDao, audioDir) as T
            }
        }
    }
}
