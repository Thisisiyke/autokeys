package com.autokeys.spike

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/**
 * Phase 0 detection spike — single screen.
 *
 * Shows, live: the detected note, its frequency and cents-off, the input level,
 * and the smoothed "key" with a confidence bar. This is the screen you point at
 * a singer to judge whether detection is trustworthy enough to build on.
 *
 * No audio output here on purpose — input half only. Output (Oboe) comes after
 * we trust this.
 */
class MainActivity : ComponentActivity() {

    private val green = Color(0xFF1D9E75)
    private val greenDark = Color(0xFF085041)
    private val bg = Color(0xFFF5F5F3)

    private var engine: AudioEngine? = null
    private val keySmoother = MusicMath.KeySmoother(windowSize = 20)

    // Compose state the audio thread updates and the UI reads.
    private val noteState = mutableStateOf("—")
    private val freqState = mutableStateOf(0f)
    private val centsState = mutableStateOf(0)
    private val levelState = mutableStateOf(0f)
    private val keyState = mutableStateOf("—")
    private val confState = mutableStateOf(0f)
    private val listeningState = mutableStateOf(false)

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
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))
                Text("AutoKeys", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111))
                Text("Phase 0 — detection spike", fontSize = 14.sp, color = Color(0xFF666666))

                Spacer(Modifier.height(40.dp))

                // ── Big detected note ──
                Text(
                    text = noteState.value,
                    fontSize = 120.sp,
                    fontWeight = FontWeight.Bold,
                    color = greenDark,
                    textAlign = TextAlign.Center
                )
                val f = freqState.value
                Text(
                    text = if (f > 0) "%.1f Hz".format(f) else "no pitch",
                    fontSize = 18.sp,
                    color = Color(0xFF666666)
                )
                // Cents-off: how sharp/flat. Near 0 = in tune.
                val c = centsState.value
                Text(
                    text = if (f > 0) (if (c >= 0) "+$c cents" else "$c cents") else "",
                    fontSize = 14.sp,
                    color = if (kotlin.math.abs(c) < 15) green else Color(0xFFBA7517)
                )

                Spacer(Modifier.height(32.dp))

                // ── Smoothed key ──
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("DETECTED KEY", fontSize = 11.sp, color = Color(0xFFAAAAAA), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(keyState.value, fontSize = 44.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111))
                        Spacer(Modifier.height(12.dp))
                        // Confidence bar
                        Box(
                            Modifier.fillMaxWidth().height(8.dp)
                                .background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp))
                        ) {
                            Box(
                                Modifier.fillMaxWidth(confState.value.coerceIn(0f, 1f)).height(8.dp)
                                    .background(green, RoundedCornerShape(4.dp))
                            )
                        }
                        Text(
                            "stability ${(confState.value * 100).toInt()}%",
                            fontSize = 11.sp, color = Color(0xFFAAAAAA),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Input level meter ──
                Box(
                    Modifier.fillMaxWidth().height(6.dp)
                        .background(Color(0xFFEEEEEE), RoundedCornerShape(3.dp))
                ) {
                    Box(
                        Modifier.fillMaxWidth((levelState.value * 6f).coerceIn(0f, 1f)).height(6.dp)
                            .background(Color(0xFF378ADD), RoundedCornerShape(3.dp))
                    )
                }
                Text("mic level", fontSize = 11.sp, color = Color(0xFFAAAAAA), modifier = Modifier.padding(top = 4.dp))

                Spacer(Modifier.weight(1f))

                // ── Start / Stop ──
                Button(
                    onClick = { if (listeningState.value) stopListening() else requestMicThenStart() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (listeningState.value) Color(0xFFA32D2D) else green
                    )
                ) {
                    Text(
                        if (listeningState.value) "Stop listening" else "Start listening",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    private fun requestMicThenStart() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) startListening() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startListening() {
        keySmoother.reset()
        engine = AudioEngine { freq, rms ->
            // This callback runs on the audio thread — update state, Compose handles the hop to UI.
            levelState.value = rms
            if (freq > 0f) {
                val note = MusicMath.frequencyToNote(freq)
                noteState.value = note.name
                freqState.value = freq
                centsState.value = note.cents
                keySmoother.push(note.name)?.let { keyState.value = it }
                confState.value = keySmoother.confidence()
            } else {
                noteState.value = "—"
                freqState.value = 0f
            }
        }.also { it.start() }
        listeningState.value = true
    }

    private fun stopListening() {
        engine?.stop()
        engine = null
        listeningState.value = false
        noteState.value = "—"
        freqState.value = 0f
        levelState.value = 0f
    }

    override fun onStop() {
        super.onStop()
        if (listeningState.value) stopListening()
    }
}
