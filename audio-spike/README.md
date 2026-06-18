# Phase 0 — Audio Engine Spike

This is the make-or-break. Nothing in `app/` gets built until this passes.

**Goal:** prove that mic → pitch detection → key → chord-out works in a real room at low latency, on a real mid-range Android.

This is a throwaway proving ground, not production code. One ugly test screen. The only thing that matters here is the numbers.

---

## What the spike must do

1. Open the device microphone, get a live audio stream.
2. Run real-time pitch detection on the raw buffer (native, YIN/pYIN-style).
3. Convert detected frequency → musical note (e.g. 392 Hz → G4).
4. Derive the key from a rolling buffer (~20 readings, most-frequent note over ~2s).
5. Load a bundled piano soundfont (Salamander Grand) and play a chord via Oboe.
6. Wire detected key → chord engine so the chord that plays matches what's sung.

---

## Pass / fail tests — done in a REAL room

The spike only "passes" when all of these are true:

- [ ] **Latency:** sung note → chord out is **under ~100 ms** ← the whole product
- [ ] Detection works with **one vocalist** in a quiet room
- [ ] Detection works with **several people singing in harmony**
- [ ] Detection survives **speaker bleed** (own chord output going back into the mic)
- [ ] Verified on a **real mid-range Android**, not a simulator or flagship

> If it passes → proceed to Phase 1.
> If it fails → fix detection and latency here. Do not start the app. No UI polish saves a laggy core.

---

## Latency log

Record every real-device test run here. Be honest — this table is the gate.

| Date | Device | Detection→chord latency | Single voice | Harmony | Speaker bleep | Notes |
|------|--------|------------------------|--------------|---------|---------------|-------|
| | | | | | | |

---

## How to run

_(Filled in once the Android test project is scaffolded here.)_

```
# placeholder — setup steps go here when we start coding
```
