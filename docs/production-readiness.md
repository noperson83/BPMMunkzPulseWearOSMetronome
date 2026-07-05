# BPM Munkz Wear OS Production Readiness

Last updated: 2026-07-03

## Command Center

This tracker is the working source of truth for moving the six Wear OS apps from testing to production. Play Console state still needs live verification after the Chrome connector is repaired.

## Chrome / Play Console Access

- Chrome is installed and running.
- Codex Chrome Extension is installed and enabled in Chrome profile `Default`.
- Blocker: Windows native messaging host registry key is missing:
  `HKCU\Software\Google\Chrome\NativeMessagingHosts\com.openai.codexextension`
- Supported fix: reinstall or repair the Chrome plugin from the Codex plugin UI. Do not hand-edit the registry key.

## Apps

| App | Package | Local Version | Current Known Play Lane | Production Status | Next Action |
| --- | --- | ---: | --- | --- | --- |
| BPM Munkz Pulse | `bpm.munkz.pulse_wear.os.metronome` | 6 | Closed testing, in review | Needs rebuilt artifact and device smoke pass | Rebuild from current tree, verify ongoing activity on watch |
| BPM Munkz Setlist | `bpm.munkz.pulse_wear.os.playlist` | 9 | Closed testing, in review | Closest to production, large-font risk being hardened | Apply main screen font cap, rebuild v9 or bump if already uploaded |
| BPM Munkz Pulse Pro | `bpm.munkz.pulse_wear.os.pro` | 7 | Internal testing | Blocked by Pro launch/runtime and product model decision | Test direct launch with logcat; decide paid app vs free + IAP |
| BPM Munkz Rhythm | `bpm.munkz.pulse_wear.os.rhythm` | 2 | Internal testing | Not upload-ready from current tree | Rebuild, verify permissions/data safety and Wear layouts |
| BPM Munkz Tuner | `bpm.munkz.pulse_wear.os.tuner` | 2 | Internal testing | Compile/lint pass, test blocker found | Fix `ProTrialGateTest`, run test lane, screenshot mic/audio flows |
| Munkz Fidget Toy | `bpm.munkz.pulse_wear.os.fidgettoy` | 1 | Draft, in review | Needs Play setup and manifest cleanup review | Complete closed test setup, verify inherited permissions/services |

## Completed Worker Lanes

- Test worker: updated `ProTrialGateTest.kt` for the current `proFeatureControlsEnabled` signature and added explicit purchase-unlocked coverage.
  - Verified: `:app:testBpmDebugUnitTest --tests bpm.munkz.pulse_wear.os.bpm.presentation.ProTrialGateTest` passed.
- Setlist UI worker: hardened `PlaylistPage.kt` main clock text for large Wear OS font settings.
  - Verified: `:app:compilePlaylistDebugKotlin` passed.

## Immediate Next Worker Lanes

- Rebuild Setlist release after the main clock large-font patch. Bump version only if v9 was already uploaded to Play.
- Pro launch worker: run direct Pro startup on watch, capture `AndroidRuntime`/`ActivityTaskManager` logs, and decide paid app vs free + IAP model.
- Manifest cleanup worker: add flavor-specific manifest removals for apps that should not inherit mic, tile, complication, billing, or foreground-service declarations.
- Fidget Toy worker: compile `fidgettoy`, verify versioning, and decide whether donation products stay in-app purchases or wait until after closed testing.

## Upload Rule

Before any AAB is uploaded:

1. Confirm the version code is higher than the latest Play artifact.
2. Build the exact flavor release from the current tree.
3. Verify package id, version code, label, watch feature, and required permissions with `aapt`.
4. Run at least one watch smoke test, including large font for any UI-rejected app.
5. In Play Console, deactivate/not-include the rejected bundle in the new release.
