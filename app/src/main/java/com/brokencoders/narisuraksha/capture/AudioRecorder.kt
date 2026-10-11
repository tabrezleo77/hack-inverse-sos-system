package com.brokencoders.narisuraksha.capture

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.brokencoders.narisuraksha.core.PermissionHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

interface AudioEvidenceCapture {
    fun startRecording(): String?
    fun stopRecording(): String?
}

/**
 * Captures background audio evidence when an SOS is confirmed.
 * Audio is saved to app-private storage and never transmitted over BLE
 * (as BLE payload is too small for audio data).
 */
class AudioRecorder(private val context: Context) : AudioEvidenceCapture {

    private var mediaRecorder: MediaRecorder? = null
    private var currentRecordingFile: File? = null
    private var isRecording = false

    override fun startRecording(): String? {
        if (!PermissionHelper.hasAudioPermission(context)) {
            Log.w(TAG, "Cannot start recording: RECORD_AUDIO permission not granted")
            return null
        }

        if (isRecording) {
            Log.d(TAG, "Audio recording already in progress: ${currentRecordingFile?.absolutePath}")
            return currentRecordingFile?.absolutePath
        }

        return try {
            val audioDir = File(context.filesDir, "sos_recordings")
            if (!audioDir.exists()) {
                if (!audioDir.mkdirs() && !audioDir.exists()) {
                    Log.e(TAG, "Cannot create audio dir (storage full/unmounted)")
                    return null
                }
            }
            if (!audioDir.canWrite()) {
                Log.e(TAG, "Audio dir not writable")
                return null
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(audioDir, "SOS_$timestamp.m4a")
            currentRecordingFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(64000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            isRecording = true
            Log.i(TAG, "Audio recording started at: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false
            currentRecordingFile = null
            null
        }
    }

    override fun stopRecording(): String? {
        if (!isRecording) return null

        val path = currentRecordingFile?.absolutePath
        val file = currentRecordingFile
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            // stop() too early (<1s), mic stolen by a call, or already released:
            // the file is corrupt/0-byte — delete it and report null so callers
            // never treat it as valid evidence.
            Log.e(TAG, "Error stopping audio recording (deleting corrupt file)", e)
            try {
                mediaRecorder?.reset()
            } catch (_: Exception) {
                // ignore secondary failures during cleanup
            } finally {
                try {
                    mediaRecorder?.release()
                } catch (_: Exception) {
                    // ignore secondary failures during cleanup
                }
                mediaRecorder = null
                isRecording = false
                currentRecordingFile = null
            }
            try {
                if (file != null && file.exists()) file.delete()
            } catch (_: Exception) {
                // ignore delete failures
            }
            return null
        }
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
            Log.i(TAG, "Audio recording saved successfully: $path")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio recorder", e)
        } finally {
            mediaRecorder = null
            isRecording = false
        }
        return path
    }

    companion object {
        private const val TAG = "AudioRecorder"
    }
}
