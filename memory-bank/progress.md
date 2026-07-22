# ELOC Control Panel - Progress

## What Works

### Core Functionality ✅
- **Bluetooth Connection** - Connect to ELOC devices via BT SPP
- **Device Pairing** - Proper bonding flow with state management (recently fixed)
- **Status Reading** - Get battery, SD card, recording state, firmware info
- **Configuration Reading** - Get all device settings
- **Configuration Writing** - Update individual device settings
- **Recording Control** - Start/stop recording sessions
- **Time Sync** - Synchronize device clock with phone

### User Interface ✅
- **Home Screen** - Device list with Bluetooth scanning
- **Device Status** - Redesigned card-based dark UI (July 2026): gauge cards (SD/battery/GPS), key-stats card, expandable LoRa/Scheduler/Intruder toggle sections with inline editing + help dialogs, device-details expander, versions row, mode button + wildlifebug.com footer, hamburger menu; pull-to-refresh kept. Needs verification on real hardware (BT features untestable in emulator). Remaining screens still use the old look.
- **Settings Editor** - Individual setting modification
- **Command Line** - Raw command interface for advanced users
- **Firmware Update over BT** - (2026-07-06, built, hardware test pending) file-picker MVP:
  `FirmwareUpdater` engine + foreground `FirmwareUpdateService` + `FirmwareUpdateActivity`
  (Device Settings → Advanced, gated on firmware `fwUpdateProto`). Resume after BT drop,
  variant guard, rollback detection. Needs firmware ≥ V1.47. Phase 3 (Firestore release
  distribution / "update available") not started.
- **Best-GPS-source recording + 15 s refresh** - (2026-07-22, 5.43/61, `compileDebugKotlin` green;
  on-device test pending). The phone-vs-ELOC comparison now drives the *recorded* location, not just the
  gauge: `Gps` gains `latitude`/`longitude`/`hasLocation`, `DeviceDriver` parses `gps/lat`+`gps/lon`, and
  `DeviceActivity.effectiveGpsData()` feeds the best fix to the record-start/display `getElocInformation`,
  `setRecordState`, and the record-accuracy gate. Auto-refresh 30 s → 15 s. Backward compatible (old
  firmware has no lat/lon → phone-only). Owed: on-device test of the ELOC-wins path.
- **GPS live accuracy & time-source** - (2026-07-21, 5.42/60, `compileDebugKotlin` green; on-device
  test pending against firmware V1.54). Merged phone-vs-ELOC accuracy gauge (`updateGpsViews()` shows the
  better of phone accuracy and ELOC HDOP-derived accuracy in meters, labelling the source), ELOC Time row
  source suffix ("· GPS"/"· Phone"), and a `synchronizeTime()` redesign that reads the firmware's new
  `timeSource`/`tzSource` markers (mandatory once fw `hasFix` became live-gated). New `Gps` fields
  (`fixAgeS`, `hdop`, `accuracyMeters`) + `General` fields (`deviceTimeSource`/`deviceTzSource`) parsed
  from new getStatus keys. Backward compatible with pre-1.54 firmware (defaults → phone-only gauge, no
  suffix, old skip heuristic). Firmware counterpart: firmware repo `changelog.md` (V1.54).
- **Recording Scheduler driver + UI** - ⏮️ REVERTED, not shipped (removed 2026-07-21). Was
  code-complete but never on-device-verified; removed before the V1.54 push so the GPS-accuracy work
  could ship alone. Backup under the session scratchpad `sched-removal-backup/app/`.
- **Map View** - Google Maps integration for device locations

### Cloud Integration ✅
- **Firebase Auth** - Email/password and Google sign-in
- **Data Upload** - Status/config upload to Firestore
- **Profile Pictures** - Firebase Storage integration
- **Background Upload** - StatusUploadService for queued uploads

### User Accounts ✅
- Sign up with email/password
- Sign up with Google
- Sign in with email/password
- Sign in with Google
- User profile (picture, display name, ranger ID)
- Change email/password
- Delete account

## What's Left to Build

### Bug Fixes 🐛
| Issue | Status | Priority |
|-------|--------|----------|
| Bluetooth ON/OFF toggle not working | Not Started | Medium |
| Refresh only works after scroll / refresh triggers when scrolling up | Fix applied 2026-07-22 (needs on-device verify) | Low |
| Google Maps crashes on some devices | Not Started | Medium |

### Features 📋
| Feature | Status | Priority |
|---------|--------|----------|
| Remaining recording time calculation | Not Started | Medium |
| Map auto-zoom to all markers | Not Started | Low |
| "App update required" message system | Not Started | Low |

### Technical Debt 🔧
| Item | Status | Priority |
|------|--------|----------|
| Fix view binding toolbar issue | Not Started | Low |
| Migrate Java code to Kotlin | Not Started | Low |

## Current Status

**Version**: 5.34 AppBeta (versionCode 55)

**Overall Health**: ✅ Stable
- Core functionality working
- Recent Bluetooth pairing fix resolved major connection issues
- Ready for field testing

## Known Issues

### Bluetooth Toggle
The Bluetooth ON/OFF button in settings doesn't work. This may be related to Android permission changes in newer API levels.

### Pull-to-Refresh Behavior — fix applied 2026-07-22 (needs on-device verify)
The swipe-to-refresh on the status page used to refresh even when scrolling up mid-page (and felt
"stuck" at the bottom). Cause: the `SwipeRefreshLayout`'s direct child is a non-scrollable
ConstraintLayout, so its default at-top check always said "at top" and grabbed every downward drag.
Fixed in `DeviceActivity.kt` by replacing the racy `isEnabled = (y <= 5)` scroll listener with
`setOnChildScrollUpCallback { _, _ -> binding.scrollView.canScrollVertically(-1) }`. See
`activeContext.md` for details. Verify on hardware, then close this out.

### Google Maps Crashes
Some devices experience crashes with Google Maps. Details in `maps.log`. May be device-specific or related to map utils version.

## Evolution of Project Decisions

### Bluetooth Connection Strategy
- **Initial**: Direct socket connection on device selection
- **Problem**: Race condition with pairing on unpaired devices
- **Current**: Bond state check → initiate bonding → wait → connect

### Firebase BOM Version
- **Current**: 32.6.0 (pinned for API 21 support)
- **Trade-off**: Newer Firebase features unavailable
- **Reason**: Support older Android devices in field use

### UI Architecture
- **Pattern**: Single Activity per screen, ThemableActivity base
- **Binding**: View Binding (modern approach)
- **Navigation**: Intent-based between activities

## Milestones

### Completed ✅
- [x] Initial app structure and UI
- [x] Bluetooth SPP connection
- [x] Device command protocol
- [x] Firebase authentication
- [x] Data upload service
- [x] Google Maps integration
- [x] User profile management
- [x] Bluetooth pairing fix (Jan 2026)
- [x] Memory Bank initialization (Feb 2026)
- [x] Database upload optimization (Feb 2026)
- [x] Google Sign-In implementation (Feb 2026)
- [x] Duty Cycle settings implementation (Feb 2026)
- [x] LoRa RSSI signal strength indicator (Feb 2026)
- [x] Map document: added batteryType and recordingState fields (Feb 2026)
- [x] Status document stale timestamp bug fix (Mar 2026)

### Upcoming 🎯
- [ ] Fix remaining bugs (BT toggle, refresh)
- [ ] Implement recording time calculation
- [ ] Kotlin migration for legacy code
- [ ] Performance testing with multiple devices
