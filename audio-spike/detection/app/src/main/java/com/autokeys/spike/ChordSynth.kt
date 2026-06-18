package com.autokeys.spike

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Low-latency chord synthesizer (Kotlin / AudioTrack).
 *
 * Generates piano-ISH tones in code and streams them to the speaker. We're using
 * AudioTrack (not Oboe/C++) deliberately: it proves the full sing→chord loop and
 * lets us MEASURE latency before deciding whether the C++ complexity is justified.
 *
 * Sound design note: this is additive synthesis (a few harmonics + a fast attack
 * and exponential decay) to fake a plucked/struck-string timbre. It will sound
 * like an electric keyboard, not a Steinway. Real recorded piano samples come
 * later — right now we care about TIMING, not tone.
 *
 * Latency settings:
 *  - PERFORMANCE_MODE_LOW_LATENCY asks the OS for the fast audio path
 *  - small buffer, streaming mode
 */
class ChordSynth {

    companion object {
        const val SAMPLE_RATE = 44100
    }

    @Volatile private var track: AudioTrack? = null
    @Volatile private var playing = false
    private var playThread: Thread? = null

    // The chord currently being asked to sound, as a list of frequencies (Hz).
    @Volatile private var targetFreqs: FloatArray = FloatArray(0)
    // Bumped every time the chord changes, so the synth thread re-triggers the attack.
    @Volatile private var triggerId: Long = 0

    fun start() {
        if (playing) return
        playing = true

        playThread = thread(name = "autokeys-synth", priority = Thread.MAX_PRIORITY) {
            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
            )

            val at = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()

            track = at
            at.play()

            // Render in small blocks so a chord change takes effect almost immediately.
            val blockSize = 256
            val block = FloatArray(blockSize)

            var phase = DoubleArray(0)        // running phase per voice (avoids clicks)
            var localFreqs = FloatArray(0)
            var localTrigger = -1L
            var samplesSinceAttack = 0

            while (playing) {
                // Did the chord change? Re-seed voices and restart the envelope.
                if (triggerId != localTrigger) {
                    localTrigger = triggerId
                    localFreqs = targetFreqs.copyOf()
                    phase = DoubleArray(localFreqs.size)
                    samplesSinceAttack = 0
                }

                if (localFreqs.isEmpty()) {
                    // Nothing to play — output silence, keep the stream alive.
                    java.util.Arrays.fill(block, 0f)
                    at.write(block, 0, blockSize, AudioTrack.WRITE_BLOCKING)
                    continue
                }

                for (i in 0 until blockSize) {
                    val t = samplesSinceAttack.toDouble() / SAMPLE_RATE
                    // Piano-ish envelope: near-instant attack, exponential decay over ~1.8s.
                    val env = exp(-t * 1.8).toFloat()
                    var sample = 0f
                    for (v in localFreqs.indices) {
                        val f = localFreqs[v]
                        phase[v] += 2.0 * PI * f / SAMPLE_RATE
                        if (phase[v] > 2.0 * PI) phase[v] -= 2.0 * PI
                        // Fundamental + a couple of decaying harmonics for a richer, less pure tone.
                        val p = phase[v]
                        val s = sin(p) + 0.35f * sin(2 * p) + 0.15f * sin(3 * p)
                        sample += s.toFloat()
                    }
                    // Normalise by voice count so chords don't clip, apply envelope + headroom.
                    sample = (sample / localFreqs.size) * env * 0.6f
                    block[i] = sample
                    samplesSinceAttack++
                }
                at.write(block, 0, blockSize, AudioTrack.WRITE_BLOCKING)
            }

            at.stop()
            at.release()
        }
    }

    /** Play a chord (list of frequencies). Re-triggers the attack envelope. */
    fun playChord(freqs: FloatArray) {
        targetFreqs = freqs
        triggerId++   // signal the synth thread to restart the envelope
    }

    /** Silence. */
    fun clear() {
        targetFreqs = FloatArray(0)
        triggerId++
    }

    fun stop() {
        playing = false
        playThread?.join(500)
        playThread = null
        track = null
    }
}
