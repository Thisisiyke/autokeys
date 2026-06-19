package com.autokeys.app.audio

import kotlin.math.pow

/**
 * Chord engine.
 *
 * Given a detected key and a genre, produce the chord progression and the actual
 * note frequencies to feed the synth. This is the music-theory brain that sits
 * between detection ("they're singing in G") and playback ("play G major: G+B+D").
 *
 * Scope for the spike: we voice simple triads in a comfortable octave. Genre
 * mainly sets which chords appear and in what order. Richer voicings (7ths,
 * inversions, bass) come later — right now we're proving the pipe and the timing.
 */
object ChordEngine {

    private val NOTE_INDEX = mapOf(
        "C" to 0, "C#" to 1, "D" to 2, "D#" to 3, "E" to 4, "F" to 5,
        "F#" to 6, "G" to 7, "G#" to 8, "A" to 9, "A#" to 10, "B" to 11
    )

    /** Semitone offsets from the root for common triad qualities. */
    private val MAJOR = intArrayOf(0, 4, 7)
    private val MINOR = intArrayOf(0, 3, 7)

    /**
     * A chord = a name to show ("Em") plus the semitone offsets from C0 to actually play.
     */
    data class Chord(val name: String, val semitonesFromC: IntArray)

    /**
     * A genre preset: the chord progression expressed as scale-degree + quality,
     * built relative to whatever key was detected.
     *
     * Each step is (scaleDegreeSemitone, isMinor, suffix). We build the actual
     * chord by transposing onto the detected root.
     */
    private data class Step(val offset: Int, val minor: Boolean, val suffix: String)

    // Progressions are classic, genre-appropriate, and intentionally simple.
    // offset = semitones above the key root for this chord's root.
    private val GENRE_PROGRESSIONS = mapOf(
        // I – IV – vi – V  (worship/pop backbone)
        "worship"   to listOf(Step(0, false, ""), Step(5, false, ""), Step(9, true, "m"), Step(7, false, "")),
        "pop"       to listOf(Step(0, false, ""), Step(7, false, ""), Step(9, true, "m"), Step(5, false, "")),
        // ii – V – I – vi (jazzy)
        "jazz"      to listOf(Step(2, true, "m"), Step(7, false, ""), Step(0, false, ""), Step(9, true, "m")),
        // i – VI – III – VII (minor, reggae feel)
        "reggae"    to listOf(Step(0, true, "m"), Step(8, false, ""), Step(3, false, ""), Step(10, false, "")),
        // vi – IV – I – V (R&B / emotional)
        "rnb"       to listOf(Step(9, true, "m"), Step(5, false, ""), Step(0, false, ""), Step(7, false, "")),
        // I – V – vi – IV (afrobeats bright)
        "afro"      to listOf(Step(0, false, ""), Step(7, false, ""), Step(9, true, "m"), Step(5, false, "")),
        "hiphop"    to listOf(Step(9, true, "m"), Step(0, false, ""), Step(5, false, ""), Step(7, false, "")),
        "classical" to listOf(Step(0, false, ""), Step(5, false, ""), Step(7, false, ""), Step(0, false, ""))
    )

    private val NAMES_SHARP = arrayOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")

    /**
     * Build the full chord progression for a detected key + genre.
     * @param keyRoot e.g. "G"
     * @param genre   e.g. "worship"
     * @return ordered list of chords to cycle through
     */
    fun progressionFor(keyRoot: String, genre: String): List<Chord> {
        val rootIdx = NOTE_INDEX[keyRoot] ?: 0
        val steps = GENRE_PROGRESSIONS[genre] ?: GENRE_PROGRESSIONS["worship"]!!
        return steps.map { step ->
            val chordRoot = (rootIdx + step.offset) % 12
            val quality = if (step.minor) MINOR else MAJOR
            val name = NAMES_SHARP[chordRoot] + step.suffix
            // Voice the triad in octave 4 (comfortable middle range).
            val semis = quality.map { (4 * 12) + chordRoot + it }.toIntArray()
            Chord(name, semis)
        }
    }

    /**
     * Convert a semitone-from-C0 value to a frequency in Hz.
     * C0 is MIDI 12. A4 (MIDI 69) = 440 Hz.
     */
    fun semitoneToFreq(semitoneFromC0: Int): Float {
        val midi = semitoneFromC0 + 12
        return (440.0 * 2.0.pow((midi - 69) / 12.0)).toFloat()
    }
}
