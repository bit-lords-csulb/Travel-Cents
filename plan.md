# Travel Cents — Master Plan Dashboard

> Branch: `new-new-trip-layout` | Updated: 2026-04-05
>
> **Files:** [Pending tasks](plan-pending.md) · [Completed archive](plan-done.md) · [Reference (execution order, files, API budget)](plan-reference.md)

---

## Difficulty Routing

| Label | Scope | Model |
|---|---|---|
| `[Easy]` | Single file, < 30 lines | Haiku / Codex |
| `[Medium]` | 1–3 files, some logic | Sonnet / Codex |
| `[Hard]` | Multi-file, architecture | Sonnet / Gemini |
| `[Complex]` | Cross-cutting, deep context | Opus / paired agents |

---

## Phase Status

| Phase | Done | Total | Status |
|---|---|---|---|
| 0 — v1 Overhaul | 11 | 11 | ✅ complete |
| 1 — Performance | 9 | 9 | ✅ complete |
| 2 — Event Pipeline | ~30 | ~34 | ⚠ mostly done |
| 3 — Selection UX (original) | ~12 | ~15 | ⚠ mostly done |
| 3 — Post-Completion Refinements | ~22 | ~40 | ⚠ partial |
| 4 — UI Standardization | ~10 | ~17 | ⚠ partial |
| 5 — Polish & Edge Cases | 0 | 10 | ○ pending |
| 6 — Chat & Messaging | 0 | 26 | ○ pending |
| 7 — Profile & Identity | 0 | 8 | ○ pending |
| 8 — Settings Overhaul | 0 | 12 | ○ pending |

---

## Next Up (priority order)

| Task | Summary | Difficulty |
|---|---|---|
| 3.8.2 Step 1 | Validate Day 1 flight grouping via `test.py` after parser fix | `[Easy]` |
| 3.7 | Jiggle mode reorder (animation + Done banner + Firestore persist) | `[Medium]` |
| 3.5 remaining | Exclude already-selected options from change menu | `[Medium]` |
| 3.6 remaining | Groq prompt: no-repeat restaurant/activity instruction | `[Easy]` |
| 3.8.3 `NewTripViewModel` | Replace per-day Yelp fan-out with pooled fetch | `[Hard]` |
| 3.10 | Multi-photo gallery (HorizontalPager + `ImageGalleryScreen`) | `[Medium]` |
| 3.11 | Trip sharing: fix wrong-trip bug + nav wiring + read-only view | `[Hard]` |
| 4.1 | `WizardBottomBar` shared composable for all steps | `[Easy]` |
| 4.2 | `TravelCentsTextField` shared input composable | `[Easy]` |
| 8.1 | Fix logout state bug + account deletion cascade | `[Hard]` |
