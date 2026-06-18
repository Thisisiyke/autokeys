package com.autokeys.spike

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/**
 * Phase 0 - FULL spike. Detection + playback wired together.
 *
 * Flow: pick a genre -> Start -> sing -> detection locks a key -> chord engine builds
 * the progression in that key -> synth plays it -> we DISPLAY the detected key, the
 * current chord, and a rough latency number (time from key-lock to first chord out).
 *
 * The latency readout is the whole point of this build: it tells us whether the
 * Kotlin/AudioTrack output is fast enough, or whether we need to drop to Oboe/C++.
 */
class MainActivity : ComponentActivity() {

    private val green = Color(0xFF1D9E75)
    private val greenDark = Color(0xFF085041)
    private val bg = Color(0xFFF5F5F3)

    private var engine: AudioEngine? = null
    private val synth = ChordSynth()
    private val keySmoother = MusicMath.KeySmoother(windowSize = 20)

    private val genres = listOf("worship", "pop", "rnb", "reggae", "afro", "hiphop", "jazz", "classical")

    // UI state
    private val noteState = mutableStateOf("-")
    private val freqState = mutableStateOf(0f)
    private val keyState = mutableStateOf("-")
    private val confState = mutableStateOf(0f)
    private val listeningState = mutableStateOf(false)
    private val genreState = mutableStateOf("worship")
    private val chordNameState = mutableStateOf("-")
    private val latencyState = mutableStateOf(-1L)
    private val progressionState = mutableStateOf<List<String>>(emptyList())

    // playback control
    @Volatile private var currentKey: String? = null
    @Volatile private var progression: List<ChordEngine.Chord> = emptyList()
    @Volatile private var chordIdx = 0
    private var chordTimer: Thread? = null
    @Volatile private var cycling = false
    @Volatile private var keyLockedAt = 0L
    @Volatile private var awaitingFirstChord = false

    private val micPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startListening() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SpikeScreen() }
    }

    @Composable
    private fun SpikeScreen() {
        MaterialTheme {
            Column(
                modifier = Modifier.fillMaxSize().background(bg).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("AutoKeys", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111))
                Text("Phase 0 - full spike (detect + play)", fontSize = 13.sp, color = Color(0xFF666666))
                Spacer(Modifier.height(12.dp))

                Text("GENRE", fontSize = 11.sp, color = Color(0xFFAAAAAA), fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(6.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxWidth().height(96.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(genres) { g ->
                        val selected = genreState.value == g
                        Box(
                            Modifier.fillMaxWidth().height(42.dp)
                                .background(if (selected) green else Color.White, RoundedCornerShape(10.dp))
                                .clickable { genreState.value = g; onGenreChanged() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(g.replaceFirstChar { it.uppercase() }, fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) Color.White else Color(0xFF333333))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("HEARD", fontSize = 10.sp, color = Color(0xFFAAAAAA), fontWeight = FontWeight.Bold)
                        Text(noteState.value, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                        Text(if (freqState.value > 0) "%.0f Hz".format(freqState.value) else "-",
                            fontSize = 11.sp, color = Color(0xFFAAAAAA))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("KEY", fontSize = 10.sp, color = Color(0xFFAAAAAA), fontWeight = FontWeight.Bold)
                        Text(keyState.value, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = greenDark)
                        Text("${(confState.value * 100).toInt()}% stable", fontSize = 11.sp, color = Color(0xFFAAAAAA))
                    }
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NOW PLAYING", fontSize = 11.sp, color = Color(0xFFAAAAAA), fontWeight = FontWeight.Bold)
                        Text(chordNameState.value, fontSize = 64.sp, fontWeight = FontWeight.Bold, color = green)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            progressionState.value.forEach { c ->
                                Box(
                                    Modifier.background(
                                        if (c == chordNameState.value) green else Color(0xFFEEEEEE),
                                        RoundedCornerShape(8.dp)
                                    ).padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(c, fontSize = 12.sp,
                                        color = if (c == chordNameState.value) Color.White else Color(0xFF666666))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            latencyState.value < 0 -> Color.White
                            latencyState.value <= 100 -> Color(0xFFE1F5EE)
                            else -> Color(0xFFFAEEDA)
                        }
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SING -> CHORD LATENCY", fontSize = 11.sp, color = Color(0xFF888888), fontWeight = FontWeight.Bold)
                        Text(
                            if (latencyState.value < 0) "-" else "${latencyState.value} ms",
                            fontSize = 36.sp, fontWeight = FontWeight.Bold,
                            color = when {
                                latencyState.value < 0 -> Color(0xFF999999)
                                latencyState.value <= 100 -> greenDark
                                else -> Color(0xFFBA7517)
                            }
                        )
                        Text(
                            when {
                                latencyState.value < 0 -> "sing a note to measure"
                                latencyState.value <= 100 -> "under 100ms - excellent"
                                latencyState.value <= 200 -> "acceptable, room to improve"
                                else -> "too high - needs Oboe"
                            },
                            fontSize = 11.sp, color = Color(0xFF888888)
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = { if (listeningState.value) stopListening() else requestMicThenStart() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (listeningState.value) Color(0xFFA32D2D) else green
                    )
                ) {
                    Text(if (listeningState.value) "Stop" else "Start listening",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }

    private fun onGenreChanged() {
        currentKey?.let { rebuildProgression(it) }
    }

    private fun rebuildProgression(key: String) {
        progression = ChordEngine.progressionFor(key, genreState.value)
        progressionState.value = progression.map { it.name }
        chordIdx = 0
    }

    private fun requestMicThenStart() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) startListening() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startListening() {
        keySmoother.reset()
        currentKey = null
        latencyState.value = -1L
        synth.start()

        engine = AudioEngine { freq, _ ->
            if (freq > 0f) {
                val note = MusicMath.frequencyToNote(freq)
                noteState.value = note.name
                freqState.value = freq
                val locked = keySmoother.push(note.name)
                confState.value = keySmoother.confidence()
                if (locked != null) {
                    keyState.value = locked
                    if (locked != currentKey) {
                        currentKey = locked
                        rebuildProgression(locked)
                        keyLockedAt = System.currentTimeMillis()
                        awaitingFirstChord = true
                        startCycling()
                    }
                }
            } else {
                noteState.value = "-"
                freqState.value = 0f
            }
        }.also { it.start() }

        listeningState.value = true
    }

    private fun startCycling() {
        if (cycling) return
        cycling = true
        chordTimer = Thread {
            val beatMs = 1500L
            while (cycling && listeningState.value) {
                val prog = progression
                if (prog.isNotEmpty()) {
                    val chord = prog[chordIdx % prog.size]
                    val freqs = chord.semitonesFromC.map { ChordEngine.semitoneToFreq(it) }.toFloatArray()
                    synth.playChord(freqs)
                    if (awaitingFirstChord) {
                        awaitingFirstChord = false
                        val ms = System.currentTimeMillis() - keyLockedAt
                        runOnUiThread {
                            chordNameState.value = chord.name
                            latencyState.value = ms
                        }
                    } else {
                        runOnUiThread { chordNameState.value = chord.name }
                    }
                    chordIdx++
                }
                try { Thread.sleep(beatMs) } catch (e: InterruptedException) { break }
            }
        }.also { it.start() }
    }

    private fun stopCycling() {
        cycling = false
        chordTimer?.interrupt()
        chordTimer = null
    }

    private fun stopListening() {
        stopCycling()
        engine?.stop(); engine = null
        synth.clear(); synth.stop()
        listeningState.value = false
        noteState.value = "-"; freqState.value = 0f
        chordNameState.value = "-"
    }

    override fun onStop() {
        super.onStop()
        if (listeningState.value) stopListening()
    }
}
