package com.autokeys.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.concurrent.thread

/**
 * Live microphone capture, tuned for low latency.
 *
 * Reads mono 16-bit PCM in a tight loop on a background thread, converts each
 * buffer to Float [-1,1], and hands it to the YIN detector. Results come back
 * on the callback. We keep this in Kotlin/AudioRecord for the detection spike;
 * the low-latency OUTPUT path later is where we drop to Oboe/C++.
 *
 * BUFFER_SIZE is the single most important tuning knob here:
 *  - smaller = lower latency, but YIN has fewer samples so low notes get shaky
 *  - larger  = more stable pitch, but more lag
 * 2048 @ 44.1kHz ≈ 46ms of audio — a sane starting point. We'll tune on-device.
 */
class AudioEngine(
    private val onPitch: (freqHz: Float, rmsLevel: Float) -> Unit
) {
    companion object {
        const val SAMPLE_RATE = 44100
        const val BUFFER_SIZE = 2048
    }

    @Volatile private var running = false
    private var recordThread: Thread? = null
    private val detector = YinPitchDetector(SAMPLE_RATE, BUFFER_SIZE)

    @SuppressLint("MissingPermission") // caller guarantees RECORD_AUDIO is granted before start()
    fun start() {
        if (running) return
        running = true

        recordThread = thread(name = "autokeys-audio", priority = Thread.MAX_PRIORITY) {
            // Ask the OS for at least its minimum buffer; use the larger of that and ours.
            val minBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val recordBufBytes = maxOf(minBuf, BUFFER_SIZE * 2) // 2 bytes per 16-bit sample

            // VOICE_RECOGNITION source: minimal OS processing, low latency, no AGC games.
            // (Avoid MIC/DEFAULT here — some devices add noise suppression that mangles pitch.)
            val recorder = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                recordBufBytes
            )

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                running = false
                return@thread
            }

            val shortBuf = ShortArray(BUFFER_SIZE)
            val floatBuf = FloatArray(BUFFER_SIZE)

            recorder.startRecording()
            try {
                while (running) {
                    val read = recorder.read(shortBuf, 0, BUFFER_SIZE)
                    if (read <= 0) continue

                    // 16-bit PCM (-32768..32767) → Float (-1..1)
                    var sumSquares = 0f
                    for (i in 0 until read) {
                        val s = shortBuf[i] / 32768f
                        floatBuf[i] = s
                        sumSquares += s * s
                    }
                    val rms = kotlin.math.sqrt(sumSquares / read)

                    val freq = detector.detect(floatBuf)
                    onPitch(freq, rms)
                }
            } finally {
                recorder.stop()
                recorder.release()
            }
        }
    }

    fun stop() {
        running = false
        recordThread?.join(500)
        recordThread = null
    }
}
