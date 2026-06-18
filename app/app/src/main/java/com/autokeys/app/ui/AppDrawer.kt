package com.autokeys.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autokeys.app.nav.Screen

/**
 * The hamburger drawer — AutoKeys' entire navigation surface.
 * No bottom tab bar; every main screen is reached from here, per the handoff doc.
 */
@Composable
fun AppDrawer(
    current: String,
    onNavigate: (Screen) -> Unit,
    onClose: () -> Unit
) {
    val green = Color(0xFF1D9E75)
    val greenDark = Color(0xFF085041)

    ModalDrawerSheet(
        drawerContainerColor = Color.White,
        modifier = Modifier.width(280.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(vertical = 24.dp)) {

            // ── Header: user identity (placeholder until accounts are wired) ──
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(44.dp).background(green, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("KM", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Kofi Mensah", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF111111))
                    Text("Free plan", fontSize = 12.sp, color = Color(0xFF999999))
                }
            }
            Divider(color = Color(0xFFEEEEEE))
            Spacer(Modifier.height(8.dp))

            // ── Drawer items ──
            Screen.drawerItems.forEach { screen ->
                val selected = screen.route == current
                Row(
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .background(
                            if (selected) Color(0xFFE1F5EE) else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onNavigate(screen) }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        screen.title,
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) greenDark else Color(0xFF333333)
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            Divider(color = Color(0xFFEEEEEE))
            Row(
                Modifier.fillMaxWidth()
                    .clickable { onNavigate(Screen.SignIn) }
                    .padding(horizontal = 28.dp, vertical = 16.dp)
            ) {
                Text("Sign out", fontSize = 14.sp, color = Color(0xFFA32D2D))
            }
        }
    }
}
