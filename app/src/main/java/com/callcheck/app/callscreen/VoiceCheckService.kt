package com.callcheck.app.callscreen

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlin.math.ln
import kotlin.math.max

/**
 * The voice-authenticity check, built as honestly as Android allows.
 *
 * WHAT THIS ACTUALLY DOES:
 *   1. Puts the active call on speakerphone (with the user's explicit
 *      per-call consent — this is not silent).
 *   2. Records from the regular microphone (MediaRecorder.AudioSource.MIC),
 *      which at that point is picking up whatever is playing through the
 *      speaker — i.e. the caller's voice, mixed with room noise and
 *      anything the user says.
 *   3. Runs a lightweight spectral-flatness heuristic over ~13 seconds
 *      of audio as a rough, non-validated signal.
 *
 * WHAT THIS CANNOT DO, AND WHY:
 *   - It cannot access the call's audio stream directly.
 *     AudioSource.VOICE_CALL / VOICE_DOWNLINK require system/privileged
 *     permissions (CAPTURE_AUDIO_OUTPUT) that Google does not grant to
 *     third-party apps — this throws SecurityException by design, on
 *     every Android version currently in the field.
 *   - Speakerphone capture is noisy, picks up the user's own voice, and
 *     will be less accurate than direct-stream analysis.
 *   - The flatness heuristic below is illustrative signal processing,
 *     not a trained deepfake/voice-clone classifier. For real accuracy
 *     you'd need to swap analyzeSamples() for a proper on-device model
 *     (e.g. a TensorFlow Lite model trained on real vs. synthetic
 *     speech), which is a separate ML project in itself.
 *   - Recording a call (even your own side) has consent requirements
 *     that vary by country and, in India, by state — confirm the rules
 *     that apply to you before shipping this to real users.
 */
class VoiceCheckService : Service() {

    private var audioRecord: AudioRecord? = null
    private val sampleRate = 16000
    private val channel = AudioFormat.CHANNEL_IN_MONO
    private val encoding = AudioFormat.ENCODING_PCM_16BIT

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification("Checking voice on speakerphone…"))

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.isSpeakerphoneOn = true // requires an active call + MODIFY_AUDIO_SETTINGS

        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channel, encoding)
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channel, encoding, minBuf)

        Thread { runCapture(minBuf) }.start()
        return START_NOT_STICKY
    }

    private fun runCapture(bufferSize: Int) {
        val record = audioRecord ?: return
        val buffer = ShortArray(bufferSize)
        val flatnessSamples = mutableListOf<Double>()
        val durationMs = 13_000L
        val startTime = System.currentTimeMillis()

        record.startRecording()
        while (System.currentTimeMillis() - startTime < durationMs) {
            val read = record.read(buffer, 0, buffer.size)
            if (read > 0) {
                flatnessSamples.add(spectralFlatness(buffer, read))
            }
        }
        record.stop()
        record.release()

        val verdict = classify(flatnessSamples)
        postResultNotification(verdict)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Simple, illustrative flatness estimate over raw PCM — swap for a real ML model for production use. */
    private fun spectralFlatness(samples: ShortArray, len: Int): Double {
        var sum = 0.0
        var logSum = 0.0
        for (i in 0 until len) {
            val v = max(1.0, kotlin.math.abs(samples[i].toDouble()))
            sum += v
            logSum += ln(v)
        }
        val am = sum / len
        val gm = kotlin.math.exp(logSum / len)
        return if (am > 0) gm / am else 0.0
    }

    private data class Verdict(val suspicious: Boolean, val confidence: Int)

    private fun classify(samples: List<Double>): Verdict {
        if (samples.isEmpty()) return Verdict(suspicious = false, confidence = 50)
        val avg = samples.average()
        val suspicious = avg > 0.55 // arbitrary demo threshold — NOT a validated decision boundary
        val confidence = (55 + kotlin.math.abs(avg - 0.55) * 100).toInt().coerceIn(50, 90)
        return Verdict(suspicious, confidence)
    }

    private fun postResultNotification(verdict: Verdict) {
        val text = if (verdict.suspicious)
            "Possible synthetic voice detected (${verdict.confidence}% demo confidence). Verify by another channel before trusting this call."
        else
            "No red flags in this quick check (${verdict.confidence}% demo confidence). Still verify anything unusual."

        val notification = buildNotification(text)
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_RESULT_ID, notification)
    }

    private fun buildNotification(text: String): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelObj = NotificationChannel(CHANNEL_ID, "Call voice check", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(channelObj)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CallCheck")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "callcheck_voice"
        const val NOTIF_ID = 1001
        const val NOTIF_RESULT_ID = 1002
    }
}
