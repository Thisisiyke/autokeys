package com.autokeys.spike

import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Turns frequencies into musical notes, and a noisy stream of notes into a
 * single stable "key". This is the layer that makes detection feel solid
 * instead of jittery.
 */
object MusicMath {

    private val NOTE_NAMES = arrayOf(
        "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    )

    /** A detected note: its name without octave (e.g. "G"), plus the octave and cents-off. */
    data class Note(val name: String, val octave: Int, val cents: Int) {
        val full: String get() = "$name$octave"
    }

    /**
     * Convert a frequency in Hz to the nearest musical note.
     * Standard tuning: A4 = 440 Hz. MIDI note 69 = A4.
     */
    fun frequencyToNote(freq: Float): Note {
        // How many semitones above/below A4 this frequency is.
        val semitonesFromA4 = 12.0 * (ln(freq / 440.0) / ln(2.0))
        val midi = (semitonesFromA4 + 69.0)
        val midiRounded = midi.roundToInt()

        val name = NOTE_NAMES[((midiRounded % 12) + 12) % 12]
        val octave = midiRounded / 12 - 1
        // Cents: how far the actual pitch is from the exact note (+/- 50 = halfway to next).
        val cents = ((midi - midiRounded) * 100).roundToInt()
        return Note(name, octave, cents)
    }

    /**
     * Rolling key detector.
     *
     * A smartphone mic in a live room never gives you a clean single pitch — it
     * flickers between the sung note, overtones, and noise. So we don't trust any
     * single reading. We keep the last N note-names and report the most frequent
     * one as the "key". This is the handoff doc's "mode of ~20 readings over ~2s".
     *
     * Only the note NAME matters for key (octave is ignored): a G2 and a G4 both
     * vote for "G".
     */
    class KeySmoother(private val windowSize: Int = 20) {
        private val recent = ArrayDeque<String>()

        /** Add a detected note name; returns the current best-guess key, or null if not enough data. */
        fun push(noteName: String): String? {
            recent.addLast(noteName)
            if (recent.size > windowSize) recent.removeFirst()
            // Need a reasonable sample before committing to a key.
            if (recent.size < windowSize / 2) return null
            return recent
                .groupingBy { it }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
        }

        /** How dominant the current key is (0f–1f). High = confident, low = unstable signal. */
        fun confidence(): Float {
            if (recent.isEmpty()) return 0f
            val top = recent.groupingBy { it }.eachCount().maxByOrNull { it.value }?.value ?: 0
            return top.toFloat() / recent.size
        }

        fun reset() = recent.clear()
    }
}
