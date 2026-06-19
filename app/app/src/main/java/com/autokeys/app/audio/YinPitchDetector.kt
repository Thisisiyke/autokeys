package com.autokeys.app.audio

import kotlin.math.abs

/**
 * YIN pitch detection.
 *
 * Reference: de Cheveigné & Kawahara (2002), "YIN, a fundamental frequency
 * estimator for speech and music."
 *
 * This is deliberately hand-written, not a library, so we can tune every
 * threshold once real singers in a real room stress it. The four steps below
 * map 1:1 to the paper.
 *
 * Usage: feed it a buffer of mono Float samples in [-1, 1]; it returns the
 * detected frequency in Hz, or -1f if the signal isn't pitched/loud enough.
 */
class YinPitchDetector(
    private val sampleRate: Int,
    private val bufferSize: Int,
    // Below this "how confident are we it's periodic" value, accept the pitch.
    // Lower = stricter (fewer false notes, more dropouts). 0.10–0.20 is typical.
    private val threshold: Float = 0.15f
) {
    // Half the buffer: YIN can only measure periods up to bufferSize/2 long.
    private val halfBuffer = bufferSize / 2
    private val yinBuffer = FloatArray(halfBuffer)

    /**
     * @return detected pitch in Hz, or -1f if no reliable pitch found.
     */
    fun detect(audio: FloatArray): Float {
        // Gate on loudness first. Silence and room hiss should report "no pitch",
        // not a garbage frequency. RMS is cheap and good enough as a noise gate.
        var sumSquares = 0f
        for (i in 0 until bufferSize) sumSquares += audio[i] * audio[i]
        val rms = kotlin.math.sqrt(sumSquares / bufferSize)
        if (rms < 0.01f) return -1f   // too quiet — treat as no input

        // ── Step 1: difference function ──
        // For each candidate period tau, measure how different the signal is
        // from a copy of itself shifted by tau. A true period gives ~0 here.
        for (tau in 0 until halfBuffer) {
            var sum = 0f
            for (i in 0 until halfBuffer) {
                val delta = audio[i] - audio[i + tau]
                sum += delta * delta
            }
            yinBuffer[tau] = sum
        }

        // ── Step 2: cumulative mean normalized difference ──
        // Normalises so the function starts at 1 and dips toward 0 at real periods.
        // This is the key trick that stops YIN picking tau=0 every time.
        yinBuffer[0] = 1f
        var runningSum = 0f
        for (tau in 1 until halfBuffer) {
            runningSum += yinBuffer[tau]
            yinBuffer[tau] *= tau / runningSum
        }

        // ── Step 3: absolute threshold ──
        // Walk up tau and take the FIRST dip that falls below threshold —
        // not the global minimum. This avoids octave errors (picking 2x the period).
        var tauEstimate = -1
        var tau = 2
        while (tau < halfBuffer) {
            if (yinBuffer[tau] < threshold) {
                // Found a dip; follow it down to its local minimum.
                while (tau + 1 < halfBuffer && yinBuffer[tau + 1] < yinBuffer[tau]) {
                    tau++
                }
                tauEstimate = tau
                break
            }
            tau++
        }
        if (tauEstimate == -1) return -1f   // nothing periodic enough

        // ── Step 4: parabolic interpolation ──
        // The true period rarely lands exactly on an integer sample. Fit a parabola
        // through the dip and its neighbours to get a sub-sample, more accurate tau.
        val betterTau: Float
        val x0 = if (tauEstimate > 0) tauEstimate - 1 else tauEstimate
        val x2 = if (tauEstimate + 1 < halfBuffer) tauEstimate + 1 else tauEstimate
        if (x0 == tauEstimate) {
            betterTau = if (yinBuffer[tauEstimate] <= yinBuffer[x2]) tauEstimate.toFloat() else x2.toFloat()
        } else if (x2 == tauEstimate) {
            betterTau = if (yinBuffer[tauEstimate] <= yinBuffer[x0]) tauEstimate.toFloat() else x0.toFloat()
        } else {
            val s0 = yinBuffer[x0]
            val s1 = yinBuffer[tauEstimate]
            val s2 = yinBuffer[x2]
            betterTau = tauEstimate + (s2 - s0) / (2f * (2f * s1 - s2 - s0))
        }

        return sampleRate / betterTau
    }
}
