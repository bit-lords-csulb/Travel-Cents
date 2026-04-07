# Travel Cents — Reference: Execution Order, Files, API Budget

> Static reference. Load this when planning sequencing or looking up a file→task mapping.

---

## Execution Order

```
Phase 1 (Performance) — No new features, just faster
  1.1  Preload/bundle static images          ✅ done
  1.2  Fix main thread blockers              ✅ done
  1.3  Reduce logging overhead               ✅ done

Phase 2 (Event Pipeline) — Core new functionality
  2.1  Data model for multi-option events    ✅ done
  2.2  SerpAPI multi-flight extraction       ✅ done
  2.3  SerpAPI multi-hotel extraction        ✅ mostly done
  2.4  Flight empty result fallback          ✅ mostly done
  2.5  Yelp restaurant integration           ✅ mostly done
  2.6  Local image storage                   ⚠ partial
  2.7  Pipeline orchestration update         ✅ done

Phase 3 (Selection UX — original)
  3.1  Change/swap panel + drag-to-reorder   ✅ mostly done
  3.2  Trip sharing via chat                 ⚠ partial
  3.3  Event card expandable detail view     ✅ done

Phase 3 (Post-Completion Refinements) — address before Phase 4
  3.8  API response debugging                ⚠ partial  ← informs all fixes below
  3.4  FinalPlan header & navigation         ✅ done
  3.5  Card content & swap quality fixes     ⚠ partial
  3.6  No-duplicate daily recommendations    ⚠ partial
  3.7  Explicit reorder mode / jiggle mode   ○ pending
  3.9  Expanded card overlay fix             ✅ done
  3.10 Multi-photo gallery                   ○ pending
  3.11 Trip sharing navigation + read-only   ○ pending
  3.12 UI density & spacing fixes            ✅ mostly done

Phase 4 (UI Standardization) — Can run parallel to Phase 2
  4.1  Unified bottom button bar             ○ pending
  4.2  Unified text input component          ○ pending
  4.3  Consolidated color definitions        ✅ done
  4.4  Remove Save Draft                     ✅ done
  4.5  Step 5: Dietary restrictions section  ✅ done

Phase 5 (Polish) — After core features stable
  5.1  Placeholder & error states            ○ pending
  5.2  Offline support                       ○ pending
  5.3  Image cleanup                         ○ pending

Phase 6 (Chat & Messaging)
  6.1  Unified chat list (DMs + groups)      ○ pending
  6.2  Nav bar unread notification badge     ○ pending
  6.3  Delete & archive chats                ○ pending
  6.4  New group chat creation UI overhaul   ○ pending
  6.5  User profile cards in chat            ○ pending
  6.6  Shared chat input composable          ○ pending

Phase 7 (Profile & Identity)
  7.1  Profile picture upload & display      ○ pending
  7.2  Extended profile identifiers          ○ pending

Phase 8 (Settings Overhaul)
  8.1  Fix account deletion & logout         ○ pending
  8.2  Fix preferences persistence           ○ pending
  8.3  Settings reorganization & UI polish   ○ pending
```

---

## Files Touched Summary

| File | Tasks |
|---|---|
| `TravelEvent.kt` | 2.1, 2.6 |
| `EventOption.kt` *(new)* | 2.1 |
| `SerpRepository.kt` | 2.2, 2.3, 2.4 |
| `SerpModels.kt` | 2.2, 2.3 |
| `YelpRepository.kt` *(new)* | 2.5 |
| `ImageCacheManager.kt` *(new)* | 2.6, 5.3 |
| `NewTripViewModel.kt` | 2.4, 2.5, 2.7, 4.4, 4.5 |
| `TravelRequest.kt` | 2.5, 4.5 |
| `TripGeneratingPage.kt` | 2.7 |
| `FinalPlanPage.kt` | 3.1, 3.3, 5.1 |
| `EventOptionsSheet.kt` *(new)* | 3.1 |
| `EventVotingScreen.kt` *(new)* | 3.2 |
| `EditPlanScreen.kt` | 3.3 |
| `MainScaffold.kt` | 3.3, 1.2 |
| `TravelCentsTextField.kt` *(new)* | 4.2 |
| `TripWizardColors.kt` *(new)* | 4.3 |
| `WizardBottomBar.kt` *(new)* | 4.1 |
| All step pages | 4.1, 4.2, 4.3 |
| `TripStep1DestinationPage.kt` | 1.1 |
| `TripStep5InterestsPage.kt` | 1.1, 4.5 |
| `NewTripLandingPage.kt` | 1.1, 4.4 |
| `ChatsViewModel.kt` | 1.2, 6.1, 6.2, 6.3 |
| `FriendsViewModel.kt` | 1.2 |
| `ItineraryViewModel.kt` | 1.2, 3.5, 3.11 |
| `GroqRepository.kt` | 1.3, 3.6, 3.8 |
| `ItineraryScreen.kt` | 5.1 |
| `ChatsPage.kt` | 6.1, 6.2, 6.3, 6.4 |
| `CreateGroupChatPage.kt` | 6.4 |
| `ChatMessageBubble.kt` | 6.5 |
| `UserProfileSheet.kt` *(new)* | 6.5 |
| `ChatInputBox.kt` *(new)* | 6.6 |
| `ProfilePage.kt` | 7.1, 7.2 |
| `ProfileViewModel.kt` | 6.5, 7.1, 7.2 |
| `SettingsPage.kt` | 8.1, 8.2, 8.3 |
| `SettingsViewModel.kt` | 8.1, 8.2, 8.3 |
| `AuthModel.kt` | 8.1 |
| `FinalPlan.kt` | 3.4, 3.5, 3.7, 3.9, 3.10, 3.12 |
| `ExpandedEventCard.kt` | 3.9, 3.10 |
| `EventOptionsPanel.kt` | 3.5, 3.7 |
| `TravelCentsNavigation.kt` | 3.4, 3.11 |
| `ChatPage.kt` | 3.11 |
| `DirectChatPage.kt` | 3.11 |
| `Message.kt` | 3.11 |
| `FirestoreRepository.kt` | 3.11 |
| `NewTripViewModel.kt` | 3.6 |
| `YelpRepository.kt` | 3.6 |
| `SerpRepository.kt` | 3.8 |
| `ImageGalleryScreen.kt` *(new)* | 3.10 |
| `debug/test_pipeline.py` *(new, external)* | 3.8 |

---

## API Budget (Dev/Free Tier)

| API | Free Tier | Calls Per Trip | Trips Before Limit |
|-----|-----------|----------------|-------------------|
| SerpAPI | 100 searches/month | 2 (flights + hotels) | ~50/month |
| Yelp Fusion | 500 calls/day | ~7–14 (pooled, was per-day) | ~35–70/day |
| Groq | 30 req/min, 14.4K/day | 2 (metadata + activities) | ~7,200/day |
| Total per trip | — | ~11–18 API calls | — |

**Dev strategy:** Mock SerpAPI responses locally with JSON fixtures to preserve the 100/month limit. Yelp and Groq are generous enough for unrestricted dev use.

---

## Reserved Space — Future Tasks

### Phase 6 — TBD
*(Reserved for additional tasks discovered during implementation)*

- [ ] _
- [ ] _
- [ ] _
- [ ] _
- [ ] _

### Phase 7 — TBD

- [ ] _
- [ ] _
- [ ] _
- [ ] _
- [ ] _
