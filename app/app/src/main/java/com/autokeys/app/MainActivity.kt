package com.autokeys.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.autokeys.app.nav.Screen
import com.autokeys.app.ui.AppDrawer
import com.autokeys.app.ui.PlaceholderScreen
import com.autokeys.app.screens.PlayScreen
import kotlinx.coroutines.launch

/**
 * AutoKeys - real app, step 1: navigation skeleton.
 *
 * All 13 screens exist and are reachable through the hamburger drawer. Most are
 * stubs for now; we replace them one vertical slice at a time, starting with Play.
 * No bottom tab bar - drawer is the whole nav surface, per the handoff doc.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AutoKeysApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoKeysApp() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Track current route so the drawer highlights the active screen.
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: Screen.Splash.route

    // Splash, onboarding and sign-in are full-screen with no drawer/top bar.
    val chromeless = currentRoute in listOf(
        Screen.Splash.route, Screen.Onboarding.route, Screen.SignIn.route
    )

    MaterialTheme {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !chromeless,
            drawerContent = {
                if (!chromeless) {
                    AppDrawer(
                        current = currentRoute,
                        onNavigate = { screen ->
                            scope.launch { drawerState.close() }
                            navController.navigate(screen.route) {
                                launchSingleTop = true
                            }
                        },
                        onClose = { scope.launch { drawerState.close() } }
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    if (!chromeless) {
                        TopAppBar(
                            title = {
                                val title = Screen.drawerItems.firstOrNull { it.route == currentRoute }?.title
                                    ?: "AutoKeys"
                                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                            },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    // Simple hamburger drawn with three bars (no icon dependency).
                                    Column(
                                        Modifier.padding(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        repeat(3) {
                                            Box(Modifier.width(18.dp).height(2.dp).then(Modifier).background(Color(0xFF111111)))
                                        }
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                        )
                    }
                }
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    AppNavHost(navController)
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(nav: NavHostController) {
    NavHost(navController = nav, startDestination = Screen.Splash.route) {

        // ── Entry / auth ──
        composable(Screen.Splash.route) {
            SplashStub(
                onGetStarted = { nav.navigate(Screen.Onboarding.route) },
                onSignIn = { nav.navigate(Screen.SignIn.route) }
            )
        }
        composable(Screen.Onboarding.route) {
            StubWithContinue("Onboarding", "Phase 4 - role, genres, permissions, all-set (4 steps).") {
                nav.navigate(Screen.Play.route)
            }
        }
        composable(Screen.SignIn.route) {
            StubWithContinue("Sign in", "Phase 3 - email + Google/Apple/Facebook auth.") {
                nav.navigate(Screen.Play.route)
            }
        }

        // ── Main app ──
        composable(Screen.Play.route) {
            PlayScreen(onConnectBluetooth = { nav.navigate(Screen.Bluetooth.route) })
        }
        composable(Screen.Library.route) {
            PlaceholderScreen("Library", "Step 3 - saved sessions, search, filters, load into player, with real on-device persistence.")
        }
        composable(Screen.Setlists.route) {
            PlaceholderScreen("Setlists", "Step 3 - build setlists from library songs, play in sequence.")
        }
        composable(Screen.Bluetooth.route) {
            PlaceholderScreen("Bluetooth", "Phase 2 - native device scan/connect; OS routes chord audio to the speaker.")
        }
        composable(Screen.Profile.route) {
            PlaceholderScreen("Profile", "Step 4 - profile info, stats, edit, plan badge.")
        }
        composable(Screen.Settings.route) {
            PlaceholderScreen("Settings", "Step 5 - app preferences and toggles.")
        }
        composable(Screen.Premium.route) {
            PlaceholderScreen("My Plan", "Phase 5 - pricing tiers, trial, RevenueCat purchase.")
        }
        composable(Screen.Notifications.route) {
            PlaceholderScreen("Notifications", "Step 5 - notification list, filters, read state.")
        }
        composable(Screen.Referral.route) {
            PlaceholderScreen("Invite & earn", "Step 5 - invite link, share, reward tracking.")
        }
    }
}

// ── Lightweight inline stubs for the entry flow (keeps step 1 self-contained) ──

@Composable
private fun SplashStub(onGetStarted: () -> Unit, onSignIn: () -> Unit) {
    val green = Color(0xFF1D9E75)
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF5F5F3)).padding(24.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("AutoKeys", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111))
        Spacer(Modifier.height(8.dp))
        Text("Play the keyboard like a pro - no experience needed.",
            fontSize = 14.sp, color = Color(0xFF666666),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onGetStarted,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = green)
        ) { Text("Get started", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onSignIn,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        ) { Text("Already have an account", fontSize = 14.sp, color = Color(0xFF666666)) }
    }
}

@Composable
private fun StubWithContinue(title: String, note: String, onContinue: () -> Unit) {
    val green = Color(0xFF1D9E75)
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF5F5F3)).padding(24.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111))
        Spacer(Modifier.height(12.dp))
        Text(note, fontSize = 13.sp, color = Color(0xFF888888),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 19.sp)
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = green)
        ) { Text("Continue to app", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
    }
}
