package com.autokeys.app.nav

/**
 * Every screen in AutoKeys, defined once.
 *
 * This is the single source of truth for the app's navigation graph. The 13
 * screens come straight from the prototype's screen inventory. Keeping them in a
 * sealed class means the compiler catches any typo'd route and we can see the
 * whole app shape at a glance.
 */
sealed class Screen(val route: String, val title: String) {

    // ── Entry / auth flow ──
    object Splash      : Screen("splash", "AutoKeys")
    object Onboarding  : Screen("onboarding", "Welcome")
    object SignIn      : Screen("signin", "Sign in")

    // ── Main app (reachable from the drawer) ──
    object Play          : Screen("play", "Play")
    object Library       : Screen("library", "Library")
    object Setlists      : Screen("setlists", "Setlists")
    object Bluetooth     : Screen("bluetooth", "Bluetooth")
    object Profile       : Screen("profile", "Profile")
    object Settings      : Screen("settings", "Settings")
    object Premium       : Screen("premium", "My Plan")
    object Notifications : Screen("notifications", "Notifications")
    object Referral      : Screen("referral", "Invite & earn")
    object Tutorial      : Screen("tutorial", "Tutorial")

    companion object {
        /** The items that appear in the hamburger drawer, in order. */
        val drawerItems = listOf(
            Play, Library, Setlists, Profile, Settings, Bluetooth, Notifications, Referral, Premium
        )
    }
}
