package com.autokeys.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.autokeys.app.audio.PlayController

/**
 * The real Play screen - the heart of AutoKeys.
 *
 * Layout follows the prototype: genre grid, key/BPM/mode stats, override panel,
 * chord display with progression pills, live piano keys, volume, controls.
 * All audio lives in PlayController; this screen is presentational + input.
 *
 * Pass 1: working + close to the prototype. Pass 2 polishes exact spacing and
 * key animations.
 */

private val GREEN = Color(0xFF1D9E75)
private val GREEN_DARK = Color(0xFF085041)
private val GREEN_LIGHT = Color(0xFFE1F5EE)
private val BG = Color(0xFFF5F5F3)
private val CARD = Color.White
private val TEXT = Color(0xFF111111)
private val TEXT2 = Color(0xFF666666)
private val TEXT3 = Color(0xFFAAAAAA)

private val GENRES = listOf(
    "worship" to "Worship", "rnb" to "R&B", "reggae" to "Reggae", "afro" to "Afrobeats",
    "hiphop" to "Hip-hop", "pop" to "Pop", "jazz" to "Jazz", "classical" to "Classical"
)
private val KEYS = listOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")

@Composable
fun PlayScreen(onConnectBluetooth: () -> Unit) {
    val context = LocalContext.current
    val activity = context as ComponentActivity

    // One controller per screen instance; UI hops via runOnUiThread.
    val controller = remember { PlayController(onUi = { block -> activity.runOnUiThread(block) }) }

    // mic permission launcher (idiomatic, recomposition-safe)
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) controller.toggle() }

    DisposableEffect(Unit) { onDispose { controller.stop() } }

    fun onStartStop() {
        if (controller.isListening.value) { controller.toggle(); return }
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) controller.toggle() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Column(
        Modifier.fillMaxSize().background(BG)
            .verticalScrollCompat()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── Status line ──
        val listening = controller.isListening.value
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(8.dp).background(
                    if (listening) GREEN else TEXT3, RoundedCornerShape(4.dp)
                )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (listening) "Listening... detecting key & tempo" else "Ready - select a genre to begin",
                fontSize = 13.sp, color = TEXT2
            )
        }

        // ── Genre grid ──
        SectionLabel("Genre")
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxWidth().height(132.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            items(GENRES) { (id, label) ->
                GenreButton(label, controller.genre.value == id) { controller.setGenre(id) }
            }
        }

        // ── Key / BPM / Mode stats ──
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCell("Key", controller.detectedKey.value, Modifier.weight(1f))
            StatCell("BPM", if (controller.bpm.value > 0) controller.bpm.value.toString() else "-", Modifier.weight(1f))
            StatCell("Mode", controller.mode.value, Modifier.weight(1f))
        }

        // ── Manual override (key picker) ──
        OverridePanel(
            keys = KEYS,
            activeKey = controller.manualKey.value,
            onPick = { k -> controller.setManualKey(if (controller.manualKey.value == k) null else k) },
            onAuto = { controller.setManualKey(null) }
        )

        // ── Current chord ──
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CARD),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                SectionLabel("Current chord")
                Text(controller.currentChord.value, fontSize = 56.sp, fontWeight = FontWeight.Bold, color = TEXT)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    controller.progression.value.forEach { c ->
                        val active = c == controller.currentChord.value
                        Box(
                            Modifier.background(if (active) GREEN else Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(c, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color = if (active) Color.White else TEXT2)
                        }
                    }
                }
            }
        }

        // ── Live piano (8 white keys C..C, lit by the controller) ──
        SectionLabel("Keys")
        PianoStrip(litKeys = controller.litKeys.value)

        // ── Controls ──
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { onStartStop() },
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (listening) Color(0xFFA32D2D) else GREEN
                )
            ) {
                Text(if (listening) "Stop listening" else "Start listening",
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            OutlinedButton(
                onClick = onConnectBluetooth,
                modifier = Modifier.height(54.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Connect", fontSize = 14.sp, color = TEXT2)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ── Components ──

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold,
        color = TEXT3, letterSpacing = 0.8.sp)
}

@Composable
private fun GenreButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(58.dp)
            .background(if (selected) GREEN else CARD, RoundedCornerShape(12.dp))
            .border(
                width = 0.5.dp,
                color = if (selected) GREEN else Color(0xFFE0E0DE),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else TEXT)
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Color(0xFFF0F0EE), RoundedCornerShape(8.dp)).padding(12.dp)
    ) {
        Text(label, fontSize = 11.sp, color = TEXT3)
        Spacer(Modifier.height(3.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TEXT)
    }
}

@Composable
private fun OverridePanel(
    keys: List<String>, activeKey: String?, onPick: (String) -> Unit, onAuto: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CARD),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Manual override")
                if (activeKey != null) {
                    Text("Auto", fontSize = 12.sp, color = GREEN, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onAuto() })
                }
            }
            if (activeKey != null) {
                Text("Override active - mic detection paused", fontSize = 11.sp, color = Color(0xFFBA7517))
            }
            // key pills - wrap in two rows of 6
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                keys.chunked(6).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { k ->
                            val active = k == activeKey
                            Box(
                                Modifier.weight(1f).height(34.dp)
                                    .background(if (active) GREEN else Color(0xFFF0F0EE), RoundedCornerShape(8.dp))
                                    .clickable { onPick(k) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(k, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                    color = if (active) Color.White else TEXT2)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PianoStrip(litKeys: Set<Int>) {
    val labels = listOf("C","D","E","F","G","A","B","C")
    Row(
        Modifier.fillMaxWidth().height(120.dp)
            .background(CARD, RoundedCornerShape(12.dp))
            .border(0.5.dp, Color(0xFFE0E0DE), RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { i, lbl ->
            // last C maps back to index 0's pitch class for lighting
            val lit = litKeys.contains(i % 7)
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .background(if (lit) GREEN else Color(0xFFFAFAF9), RoundedCornerShape(6.dp))
                    .border(0.5.dp, Color(0xFFE0E0DE), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(lbl, fontSize = 11.sp, color = if (lit) Color.White else TEXT3,
                    modifier = Modifier.padding(bottom = 6.dp))
            }
        }
    }
}

// Small helper so we don't need an extra import at call site.
@Composable
private fun Modifier.verticalScrollCompat(): Modifier {
    val scroll = rememberScrollState()
    return this.verticalScroll(scroll)
}
