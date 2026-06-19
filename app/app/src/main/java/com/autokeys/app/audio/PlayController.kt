package com.autokeys.app.audio

import androidx.compose.runtime.mutableStateOf

/**
 * PlayController — owns the audio lifecycle for the Play screen and exposes plain
 * state the Compose UI reads. This is the seam between the proven audio engine and
 * the app's UI: the screen never touches AudioEngine/ChordSynth directly, it talks
 * to this.
 *
 * Lifted from the spike's MainActivity logic, cleaned into a reusable controller so
 * the screen stays presentational.
 */
class PlayController(
    private val onUi: (() -> Unit) -> Unit   // hop a block onto the UI thread
) {
    private var engine: AudioEngine? = null
    private val synth = ChordSynth()
    private val keySmoother = MusicMath.KeySmoother(windowSize = 20)

    // ── Observable state for the UI ──
    val heardNote = mutableStateOf("—")
    val heardFreq = mutableStateOf(0f)
    val detectedKey = mutableStateOf("—")
    val stability = mutableStateOf(0f)
    val isListening = mutableStateOf(false)
    val genre = mutableStateOf("worship")
    val currentChord = mutableStateOf("—")
    val progression = mutableStateOf<List<String>>(emptyList())
    val bpm = mutableStateOf(72)
    val mode = mutableStateOf("Major")
    // Which white-key indices (0..7, C..C) are currently lit, for the piano view.
    val litKeys = mutableStateOf<Set<Int>>(emptySet())

    // ── Manual override ──
    val manualKey = mutableStateOf<String?>(null)

    // genre defaults pulled from the prototype
    private data class GenreInfo(val bpm: Int, val mode: String)
    private val genreInfo = mapOf(
        "worship" to GenreInfo(72, "Major"),
        "pop" to GenreInfo(120, "Major"),
        "rnb" to GenreInfo(90, "Minor"),
        "reggae" to GenreInfo(80, "Minor"),
        "afro" to GenreInfo(110, "Major"),
        "hiphop" to GenreInfo(95, "Minor"),
        "jazz" to GenreInfo(100, "Minor"),
        "classical" to GenreInfo(88, "Major")
    )

    @Volatile private var currentKeyInternal: String? = null
    @Volatile private var chords: List<ChordEngine.Chord> = emptyList()
    @Volatile private var chordIdx = 0
    @Volatile private var cycling = false
    private var chordThread: Thread? = null

    // Map a chord's root note name to the white-key index on our 8-key C..C piano.
    private val whiteKeyIndex = mapOf(
        "C" to 0, "D" to 1, "E" to 2, "F" to 3, "G" to 4, "A" to 5, "B" to 6
    )

    fun setGenre(g: String) {
        genre.value = g
        genreInfo[g]?.let { bpm.value = it.bpm; mode.value = it.mode }
        currentKeyInternal?.let { rebuild(it) }
    }

    fun setManualKey(k: String?) {
        manualKey.value = k
        if (k != null) {
            detectedKey.value = k
            currentKeyInternal = k
            rebuild(k)
            if (isListening.value) startCycling()
        }
    }

    fun toggle() {
        if (isListening.value) stop() else start()
    }

    private fun start() {
        keySmoother.reset()
        currentKeyInternal = manualKey.value
        synth.start()

        // If a manual key is set, start playing immediately without waiting for detection.
        manualKey.value?.let { rebuild(it); startCycling() }

        engine = AudioEngine { freq, _ ->
            if (freq > 0f) {
                val note = MusicMath.frequencyToNote(freq)
                onUi {
                    heardNote.value = note.name
                    heardFreq.value = freq
                }
                // Manual override pauses detection-driven key changes.
                if (manualKey.value == null) {
                    val locked = keySmoother.push(note.name)
                    onUi { stability.value = keySmoother.confidence() }
                    if (locked != null) {
                        onUi { detectedKey.value = locked }
                        if (locked != currentKeyInternal) {
                            currentKeyInternal = locked
                            rebuild(locked)
                            startCycling()
                        }
                    }
                }
            } else {
                onUi { heardNote.value = "—"; heardFreq.value = 0f }
            }
        }.also { it.start() }

        isListening.value = true
    }

    private fun rebuild(key: String) {
        chords = ChordEngine.progressionFor(key, genre.value)
        chordIdx = 0
        onUi { progression.value = chords.map { it.name } }
    }

    private fun startCycling() {
        if (cycling) return
        cycling = true
        chordThread = Thread {
            // chord every (2 beats) at the genre tempo; refine later
            while (cycling && isListening.value) {
                val prog = chords
                if (prog.isNotEmpty()) {
                    val chord = prog[chordIdx % prog.size]
                    val freqs = chord.semitonesFromC.map { ChordEngine.semitoneToFreq(it) }.toFloatArray()
                    synth.playChord(freqs)
                    // light the white keys for this chord's notes
                    val lit = chord.name.takeWhile { it == 'C' || it == 'D' || it == 'E' || it == 'F' ||
                        it == 'G' || it == 'A' || it == 'B' }
                    val rootKey = whiteKeyIndex[chord.name.firstOrNull()?.toString() ?: ""]
                    onUi {
                        currentChord.value = chord.name
                        // light root + third + fifth approx (root, +2 steps, +4 steps on white keys)
                        rootKey?.let {
                            litKeys.value = setOf(it, (it + 2) % 7, (it + 4) % 7)
                        }
                    }
                    chordIdx++
                }
                val beatMs = (60000L / bpm.value) * 2
                try { Thread.sleep(beatMs.coerceIn(500L, 3000L)) }
                catch (e: InterruptedException) { break }
            }
        }.also { it.start() }
    }

    private fun stopCycling() {
        cycling = false
        chordThread?.interrupt()
        chordThread = null
    }

    fun stop() {
        stopCycling()
        engine?.stop(); engine = null
        synth.clear(); synth.stop()
        isListening.value = false
        heardNote.value = "—"; heardFreq.value = 0f
        currentChord.value = "—"
        litKeys.value = emptySet()
    }
}
