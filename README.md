# AutoKeys

**Play the keyboard like a pro — no experience needed.**

AutoKeys listens to singers in real time, detects the musical key and tempo, and automatically plays the correct keyboard chords through a connected Bluetooth speaker. Built for churches, studios, and live venues that need keyboard accompaniment but have no trained player.

Target: **worldwide**, **iOS and Android**.

---

## Current phase

> **Phase 0 — Audio engine spike.** Proving the core (mic → key/tempo → chord out) works in a real room at low latency, on Android, before building anything else.

Track progress in `docs/build-checklist.docx`.

---

## Repo layout

```
autokeys/
├── docs/          Spec + decisions. Read-only reference, not where we build.
│   ├── developer-handoff.docx     The functional/technical spec
│   ├── build-checklist.docx       Phase-by-phase progress tracker
│   └── decisions/                 Architecture decisions (dated, append-only)
├── prototype/     The HTML prototype = pixel-perfect source of truth for UI.
├── audio-spike/   PHASE 0. The risky core, built and latency-tested in isolation.
└── app/           The real React Native app. Built ONLY after the spike passes.
```

**Discipline:**
- `prototype/` and `docs/` are reference. Copy *from* them; never build *in* them.
- `audio-spike/` and `app/` are kept separate on purpose. Do not start `app/` until Phase 0 passes its latency and detection tests.

---

## Architecture (the short version)

- **React Native (bare workflow)** for all UI, navigation, state, and backend — written once, runs on iOS and Android.
- **Native audio module** for the real-time path (mic capture, pitch detection, synthesis). Only lightweight messages ("detected key = G", "play chord X") cross into JS. The UI never touches raw audio.
- **Android audio first** — it's the worst case for latency, so it's the honest test. iOS audio is downhill after.
- Backend: Supabase (auth + Postgres + storage). Billing: RevenueCat.

Full reasoning in `docs/decisions/0001-android-first-native-audio.md`.

---

## The make-or-break test (Phase 0)

The whole product is a latency game. Before any screen is built, the spike must pass, on a real mid-range Android, in a real room:

- [ ] Sung note → chord out in **under ~100 ms**
- [ ] Detection works with one vocalist, and with several singing in harmony
- [ ] Detection survives speaker bleed (own output back into the mic)
- [ ] Verified on real mid-range hardware, not a simulator or flagship

If it passes, the rest is straightforward. If it fails, we fix detection and latency before anything else. See `audio-spike/README.md`.

---

## Stack reference

| Layer | Choice |
|---|---|
| App framework | React Native (bare) |
| Real-time audio | Native module (Android: Oboe + native pitch tracking) |
| Bluetooth | react-native-ble-manager |
| State | Zustand + AsyncStorage |
| Backend | Supabase |
| Billing | RevenueCat |
| Piano sound | Salamander Grand soundfont (bundled, not remote) |
