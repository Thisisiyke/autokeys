# 0001 — Android-first audio, native audio module, worldwide iOS + Android

**Date:** 2026-06-18
**Status:** Accepted

## Context

AutoKeys' entire value is that the chord plays *the instant* a singer hits the note. That makes the product a real-time latency problem first and a UI problem second. The target market is **worldwide**, covering both high-income iOS-heavy markets (US/UK/AU church-tech buyers) and Android-dominant regions.

The original handoff doc specified Tone.js + ML5.js running inside `react-native-webview`.

## Decisions

### 1. App framework: React Native (bare workflow)
The 13 screens are already designed and RN is strong for UI/navigation/state/backend. One codebase serves both platforms. Bare workflow (not Expo Go) because we need native modules.

### 2. Real-time audio: native module, NOT WebView
**Overriding the handoff doc.** A WebView is a full browser engine; every audio sample and mic reading crosses the native↔JS-in-browser bridge, adding delay and jitter. That's exactly where 300–400ms lag appears on mid-range Android — the hardest version of the hardest part of the app.

Rule: **the audio path stays native; React Native handles everything else.** Only lightweight messages ("detected key = G", "play chord X") cross into JS. The UI reacts to events and never touches raw audio.

- Pitch detection: native pitch-tracking (YIN/pYIN-style) on the raw buffer. More robust for live singing and far lower latency than ML5/CREPE-in-browser.
- Synthesis/playback: native engine. Android = Oboe (Google low-latency audio) driving a sampled piano.
- Bluetooth: react-native-ble-manager; OS handles audio routing to the paired speaker.

### 3. Build the audio path on Android FIRST
Engineering reason, not market reason. Android is the worst case for audio latency (fragmentation, audio-stack variance). Proving sub-100ms detection-to-chord on a mid-range Android means iOS is downhill. Building iOS first would feel great and prove nothing.

### 4. iOS is a first-class launch target
Worldwide + paying customers skewing iPhone means iOS is commercially core, not "phase two whenever." The RN + native-module split is what makes this cheap: UI is written once; only the isolated audio module gets re-implemented for iOS (Oboe → iOS audio engine). **Goal: both stores at launch, or iOS very close behind.** Android leads only in the build order of the risky audio piece.

## Consequences

- The native audio module is the genuinely hard part and has no real code yet in prototype or handoff. This is *why* it's Phase 0 and built/tested in isolation in `audio-spike/` before any screen is wired.
- `audio-spike/` and `app/` stay separate until the spike passes its latency/detection tests.
- Backend: Supabase. Billing: RevenueCat. (Standard, fast to launch.)

## Superseded

- Handoff doc's WebView + Tone.js + ML5.js audio approach.
- Earlier framing of "Africa/Android-first product" — corrected to worldwide, both platforms.
