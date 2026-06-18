# AutoKeys — the real app

Native Kotlin / Jetpack Compose. Package `com.autokeys.app`. This is the shippable
app (Phase 4+), built in vertical slices. The proven audio engine from
`../audio-spike/` gets ported into the Play screen in step 2.

> **Stack decision:** native Kotlin, Android-first, fastest path to Play Store.
> iOS is a separate future build (UI rebuilt in Swift; audio re-implemented).
> See `../../docs/decisions/`.

## Build status — step 1 of 6: navigation skeleton ✅

All 13 screens exist and are reachable through the hamburger drawer. Most are
stubs; we replace them one slice at a time.

| Step | What | Checklist phase |
|------|------|-----------------|
| 1 ✅ | App skeleton + drawer navigation, all 13 screens navigable | Phase 4 |
| 2 | **Play screen for real** — port audio engine, genre grid, chords, piano | Phase 4 / 1 |
| 3 | Library + Setlists + save/load with persistence | Phase 4 |
| 4 | Onboarding + Sign-in + Profile | Phase 4 / 3 |
| 5 | Settings, Premium, Notifications, Referral | Phase 4 |
| 6 | Backend (Supabase) + accounts + billing (RevenueCat) | Phase 3 / 5 |

## Run it

1. **File → Open** in Android Studio → select this `app/` folder (the one with `settings.gradle.kts`).
2. Let Gradle sync (auto-generates the wrapper, downloads deps).
3. Plug in your phone (USB debugging already enabled from the spike).
4. Press **Run ▶**.

## What you should see (step 1)

- Splash screen → "Get started" goes to the onboarding stub → "Continue to app" lands on Play.
- "Already have an account" → sign-in stub → Play.
- From any main screen, the hamburger (top-left) opens the drawer.
- Every drawer item navigates and is highlighted when active.
- Most screens say "coming next" with a note on which step fills them in.

This step proves the navigation spine. Step 2 is where it comes alive — the real
Play screen with the working audio engine.

## Project layout

```
app/src/main/java/com/autokeys/app/
├── MainActivity.kt          NavHost + drawer + top bar (the nav spine)
├── nav/Screen.kt            all 13 screens defined once (source of truth)
├── ui/AppDrawer.kt          the hamburger drawer
├── ui/PlaceholderScreen.kt  labelled stub for not-yet-built screens
└── audio/                   (empty — proven engine ports in here at step 2)
```
