# Detection Spike — how to run

Phase 0, input half. This app opens the mic and shows, live: the detected note,
its frequency and cents-off, a smoothed **key**, and a mic-level meter. **No audio
output** — we're only proving that detection is trustworthy before building the
harder playback layer.

---

## Run it (≈5 minutes)

1. **Open in Android Studio**
   `File → Open` → select this `detection/` folder (the one with `settings.gradle.kts`).
   Android Studio will sync Gradle and **auto-generate the Gradle wrapper** (`gradlew`,
   `gradle-wrapper.jar`) and download dependencies. First sync takes a few minutes — let it finish.

2. **Plug in your phone** via USB.
   - On the phone: enable **Developer options** (Settings → About phone → tap "Build number" 7 times).
   - Then Settings → Developer options → turn on **USB debugging**.
   - When the phone asks "Allow USB debugging?", tap **Allow**.

3. **Select your device** in the device dropdown at the top of Android Studio
   (it should show your phone's name, not an emulator).

4. **Press Run** (the green ▶). The app installs and launches.

5. On first launch tap **Start listening** → grant the mic permission when asked.

---

## What you should see

- Sing or play a note → the big letter shows the detected note (C, D, E…), the Hz,
  and how many cents sharp/flat.
- Hold a note → the **DETECTED KEY** card settles on it and the stability bar fills.
- Stop → everything resets.

---

## The tests that matter (log results in ../README.md)

Do these **on the real phone, out loud, in a real room** — not at your desk in silence:

1. **Single voice, quiet room** — sing a steady note. Does the detected note match? Is it stable or jumpy?
2. **Harmony** — a few people singing together. Does it lock onto a sensible key, or thrash?
3. **Speaker bleed sim** — play music from another speaker nearby while singing. Does detection survive?
4. **Range** — try low male notes and high female notes. Low notes are the usual failure point.

We are NOT measuring latency yet (no output to measure against). We're answering one
question: **is the detected key trustworthy enough to drive chords?** If yes → I build the
Oboe playback layer next. If it's jumpy → tell me how, and I tune the detector
(threshold, buffer size, the noise gate) before we go further.

---

## Tuning knobs (if I ask you to change one)

All in `app/src/main/java/com/autokeys/spike/`:

- `AudioEngine.kt` → `BUFFER_SIZE` — bigger = steadier but laggier; smaller = snappier but shakier on low notes.
- `YinPitchDetector.kt` → `threshold` (0.10–0.20) — lower = stricter, fewer false notes, more dropouts.
- `YinPitchDetector.kt` → the `rms < 0.01f` noise gate — raise it if a noisy room makes it detect notes during silence.
- `MusicMath.kt` → `KeySmoother(windowSize)` — bigger = more stable key, slower to react to a key change.

---

## If it won't build

- Make sure you opened the `detection/` folder itself, not its parent.
- `File → Sync Project with Gradle Files` after it finishes the first sync.
- If it complains about the JDK: `Settings → Build → Build Tools → Gradle` → set Gradle JDK to **17**.
- Paste any red error here and I'll fix it.
