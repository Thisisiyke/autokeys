package com.autokeys.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A labelled placeholder used for screens not yet built out.
 *
 * Step 1 of the real app is the navigation skeleton: every screen exists and is
 * reachable, but most are stubs. We replace these one slice at a time (Play first).
 * The @phase tag tells you which checklist step fills it in.
 */
@Composable
fun PlaceholderScreen(
    title: String,
    phaseNote: String
) {
    val green = Color(0xFF1D9E75)
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF5F5F3)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111))
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.background(Color(0xFFE1F5EE), RoundedCornerShape(20.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text("coming next", fontSize = 12.sp, color = Color(0xFF085041), fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            phaseNote,
            fontSize = 13.sp, color = Color(0xFF888888),
            textAlign = TextAlign.Center, lineHeight = 19.sp
        )
    }
}
