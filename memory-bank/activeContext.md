# ELOC Control Panel - Active Context

## Current Work Focus
Device screen redesign (July 2026) — complete visual overhaul of `DeviceActivity` (dark gray + green with orange accent). Verify on real hardware; extend the design language to the remaining screens later.

## Recent Changes

### Settings screen: section icons + per-setting help (July 2026)
1. **Section icons** — each of the 11 settings sections has a green start icon on its header (`app:drawableStartCompat` + `app:drawableTint`): General=edit, Recorder=ic_waveform, Intruder=ic_shield, LoraWAN=ic_lora, Inference=ic_sparkle, Duty Cycle=access_time, Bluetooth=bluetooth, CPU=ic_chip, Battery=ic_battery, Logs=list_view, Advanced=run. The 11 chevron-swap blocks in `DeviceSettingsActivity` were replaced by `updateSectionHeader()`, which preserves the start icon via `compoundDrawablesRelative[0]` + `setCompoundDrawablesRelativeWithIntrinsicBounds`.
2. **Per-setting help** — `ListItem` widget gained an optional `app:helpText` attr: shows a small "?" icon between label and value; tapping it opens the styled alert dialog (title = the setting's label). Every settings row (36) has a `help_*` string in `strings.xml`. Note: helpText only wires up at inflation; icons on 21–22 devices may lose tint after chevron swaps (framework drawableTint is 23+).
3. **Follow-up tweaks** — settings footer (instructions link) removed; all toggle switches (ListItem rows + device-screen section headers) scaled to 65% via scaleX/scaleY to match the help-icon size, and ListItem switches now use the orange track tints from the design system.
4. **Help-text corrections** — node-name help carries a strong "do not change" warning (changing it can break device identity). **Intruder detection is accelerometer-based** (detects the device being moved/removed), NOT sound-based — all intruder help texts corrected accordingly.

### Redesign Round 3 — feedback from hardware test (July 2026)
1. Gauge ring thickness halved (`SimpleGauge` 0.065 → 0.0325 of smallest side; widget only used on device screen).
2. Active-section stripe now green (was orange); switch stays orange.
3. Status words (Good/Low/…) under the gauges removed (layout + code); "No SD card" now shows in the storage sub-label on error.
4. ELOC Time is device-reported only again (phone-time fallback removed at user's request) and matches the other stat font size (14sp). Old firmware shows "—" until updated.
5. `tile_gradient.xml` changed from linear to **radial anchored at bottom-right** (top-left corner darkest) since the diagonal linear read as horizontal.
6. wildlifebug.com footer removed from the device screen (home/settings instruction links remain).
7. Mode button now floats transparently over the scroll content (bottom-center, 220dp wide, text has drop shadows; ScrollView full-height with 150dp bottom padding).
8. Mode chooser bottom sheet redesigned: rounded gradient sheet, drag handle, title + dismiss chevron, option cards with colored icons (`ic_record/ic_waveform/ic_lora/ic_bolt/ic_stop`) and one-line descriptions; stop styled red; sheet container made transparent in `openModeChooser()`.

### Redesign Round 2 — feedback from hardware test (July 2026)
After testing on ELOC_00168:
1. **ELOC Time blank** — that device's firmware predates `device["time"]` in getStatus (added in `ElocCommands.cpp` alongside `epoch`). App now falls back to phone time (valid because the app syncs the device clock via setTime on every connect). Uptime parses from the same `device` section, which is why it worked.
2. **Dialogs restyled** — `layout_alert_ok/option.xml` rebuilt (rounded gradient card, green caps title, orange filled OK/positive button, outlined gray negative). `showStyledDialog()` helper in `ActivityExtensions.kt` makes the dialog window transparent and sets width to 88%.
3. **Tile gradient** — `drawable/tile_gradient.xml` (angle 315, darker top-left) applied to all cards on the device screen, settings sections, and list items.
4. **Mode button** — `layout_mode_button.xml` rebuilt: centered 72dp circular button with vertical gradient + 8dp elevation, icon inside, MODE caption + state verb below; busy state = circular progress in orange circle. `ModeButton.setButtonColor()` maps the legacy color resources to gradient drawables (`mode_circle_green/red/orange`).
5. **Home (device list)** — `layout_eloc_info.xml` rebuilt as gradient cards (BT icon green, name/address, RSSI at end) keeping all view-holder ids; refresh button orange; footer link green; toolbar shadow removed.
6. **Settings page** — all 11 section LinearLayouts in `activity_device_settings.xml` got gradient card backgrounds + bold headers (structure/ids untouched so expand/collapse code still works).
7. **Toolbar menu** — hamburger + submenu replaced by a single settings-gear action (`app_bar_device.xml`); instructions entry removed (footer link remains).
8. **Global theme** — `@color/window` now #141618; toolbar + status bar use it; toolbar subtitle green. Whole app inherits the dark base.

### Device Screen Redesign (July 2026)
**Feature**: Complete redesign of the main device screen (`activity_device.xml` + `DeviceActivity.kt`). Card-based dark UI: `ui_*` palette in `colors.xml` (window #141618, cards #1C1F23, green #8CC63F, orange accent #F58220).

**New screen structure (top → bottom)**:
1. **Gauge cards** — SD Card (free % + GB free), Battery (% + voltage), GPS Accuracy (phone GPS, gates recording) — each with a `SimpleGauge` ring and a colored status word (Good/Low/Critical…) derived from the same thresholds as the gauge colors.
2. **Key stats card** — ELOC Time (device wall clock), Sample Rate, Gain, device GPS, Uptime, Rec Since Boot, Detected Events.
3. **Toggle sections** (expandable cards with header switch, orange stripe when enabled, help `?` dialog, content dimmed to 40% alpha when off):
   - **LoRa** → `LoraWan` (enable, Region, Uplink Interval + read-only Network joined/signal RSSI rows)
   - **Scheduler** → currently backed by **DutyCycle** (enable, Awake/Sleep Duration); a real scheduler is planned to replace it later
   - **Intruder Detection** → `Intruder` (enable, Threshold, Window ms)
   - Toggles/rows write config directly via `Command.createSetConfigPropertyCommand` + `DeviceDriver.processCommandQueue`, then `refreshDeviceInfo()` re-fetches status+config; edits are blocked while recording (same gate as settings screen). Value rows reuse `TextEditorActivity`/`RangeEditorActivity`.
4. **Device Details** — plain expandable card with the remaining status rows (session ID, AI model, durations, mic/battery/file info, last location, BT during recording).
5. **Versions row** — firmware + app version.
6. **Mode button** (unchanged logic) pinned above a **footer link** to wildlifebug.com (`showInstructions()`).

**Other changes**: toolbar now uses `menu/app_bar_device.xml` (hamburger `mnu_more` with submenu: Settings, Help & Instructions) replacing the gear icon + bitmap-resize hack; `DeviceActivity.onResume()` calls `refreshDeviceInfo()` when returning from editors; new drawables `ic_lora`, `ic_calendar`, `ic_shield`, `ic_help_outline`, `ic_details`; switch tint selectors under `res/color/`. Only data already present in the driver objects is displayed (nothing invented).

### Status Document Stale Timestamp Bug Fix (March 2026)
**Issue**: When changing device recording mode on a different day than the initial status document was created, the Android app updated the existing Firestore document instead of creating a new one. The `capture_timestamp` stayed at the original date, while `upload_timestamp` was refreshed and session data was updated with the new state.

**Root Cause**: `combinedStatusAndConfigTime` in the `DeviceDriver` singleton was not being reset properly. If a `StatusWithConfig` save partially completed (status saved but config response never arrived due to disconnect/timeout), the timestamp was never cleared. Since `disconnect()` also didn't clear it, the stale timestamp persisted across connections. On the next save cycle, `saveLocal()` checked `if (combinedStatusAndConfigTime == null)` and found it NOT null, so it reused the old timestamp, generating a file with the same name as the previous one. Firestore's `document.set(data)` then overwrote the existing document.

**Fix** (two changes in `DeviceDriver.kt`):
1. **`getElocInformation()`**: Added `combinedStatusAndConfigTime = null` when `saveNextInfoResponse=true`, ensuring every new save cycle starts with a fresh timestamp
2. **`disconnect()`**: Added cleanup of all save-related state (`combinedStatusAndConfigTime`, `configSaved`, `statusSaved`, `cachedStatus`, `cachedConfig`, `infoType`) to prevent stale values from persisting across connections

**Verified via Firestore**: The document `status_2026-03-01-12-17-58-GMT+0700_e_ELOC_00244_r_edsteve2.json` was created on March 1 but updated on March 2 with March 2 device data (1m 30s uptime from device restart, new session ID, new recording state), confirming March 2 data was incorrectly saved with March 1's filename.

### Map Document New Fields (February 2026)
**Feature**: Added `batteryType` and `recordingState` fields to the Firestore map collection document (`eloc_app/uploads/map/map_{deviceName}.json`).

**Key Changes**:
1. `Session.kt` - Added `recordingStateString` property to store the raw recording state string from firmware (e.g., "recordOn_detectOn")
2. `DeviceDriver.kt` - Added parsing of `payload/session/recordingState/state` string in `parseStatus()` method
3. `DeviceDriver.kt` - Added `KEY_BATTERY_TYPE_MAP` ("batteryType") and `KEY_RECORDING_STATE_MAP` ("recordingState") constants
4. `DeviceDriver.kt` - Added both new fields to the map data JSON in `saveLocal()` method

**Technical Details**:
- `batteryType` (String): Sourced from `battery.type` which is already parsed from firmware status at `payload/battery/type`. Values: "LiPo" or "LiFePo"
- `recordingState` (String): Sourced from `session.recordingStateString` parsed from firmware status at `payload/session/recordingState/state`. Values: e.g., "recordOff_detectOff", "recordOn_detectOn", etc.
- Both values come from the `getStatus` firmware command response
- No changes needed to FirestoreHelper or FileSystemHelper since the map data flows through as raw JSON

### LoRa RSSI Signal Strength Indicator (February 2026)
**Feature**: Added LoRa signal strength indicator to the Device Status page, displaying signal quality when LoRa is enabled and connected.

**Key Changes**:
1. `LoraWan.kt` - Added `LoraSignalStrength` enum with 5 levels (Excellent, Good, Fair, Poor, VeryPoor) based on RSSI thresholds, plus new status properties: `joined`, `hasSignalInfo`, `rssi`, `snr`, and computed `signalStrength`
2. `DeviceDriver.kt` - Added LoRa status keys constants, updated `sanitize()` method to handle bracketed keys (`RSSI[dBm]`, `SNR[dB]`), added LoRa status parsing in `parseStatus()` method
3. `activity_device.xml` - Added LoRa signal container with RSSI icon and dBm value display below the "Communication" item
4. `DeviceActivity.kt` - Added `updateLoraSignalDisplay()` method that updates the UI based on LoRa status
5. `strings.xml` - Added string resources: `lora_signal`, `lora_signal_dbm`, `lora_not_joined`, `lora_no_signal`

**Technical Details**:
- LoRa RSSI thresholds: Excellent (> -90 dBm), Good (-90 to -110 dBm), Fair (-110 to -120 dBm), Poor (-120 to -130 dBm), VeryPoor (< -130 dBm)
- Uses existing RSSI drawable icons (rssi_0 through rssi_5)
- Container is hidden when LoRa is disabled
- Shows "Not Joined" when LoRa is enabled but not joined to network
- Shows "No Signal" when joined but no signal info available yet
- Shows dBm value and signal icon when valid signal is available

**JSON Status Data Parsed** (from `getStatus` response):
```json
"lora": {
    "enabled": true,
    "joined": true,
    "hasSignalInfo": true,
    "RSSI[dBm]": -85.5,
    "SNR[dB]": 7.2
}
```

### Duty Cycle Settings Implementation (February 2026)
**Feature**: Added duty cycle configuration settings to the ELOC Device Settings screen, allowing users to enable/disable duty cycling and configure sleep/awake durations.

**Key Changes**:
1. `DutyCycle.kt` - New driver component class with constants for JSON keys, min/max ranges, and default values
2. `DeviceDriver.kt` - Added `dutyCycle` property, parsing logic for duty cycle JSON config keys (`dutyCycle_enable`, `dutyCycle_sleep`, `dutyCycle_awake`)
3. `Command.kt` - Added duty cycle property cases (`setDutyCycleEnable`, `setDutyCycleSleep`, `setDutyCycleAwake`) to the set config command builder
4. `activity_device_settings.xml` - Added collapsible Duty Cycle section with enable toggle, sleep duration, and awake duration items
5. `DeviceSettingsActivity.kt` - Added duty cycle data binding, listeners (toggle + range editors), and section expand/collapse logic
6. `strings.xml` - Added string resources: `duty_cycle`, `sleep_duration`, `awake_duration`, `duty_cycle_sleep_duration`, `duty_cycle_awake_duration`

**Technical Details**:
- Sleep duration range: 10 - 86400 seconds (10s to 24h), default 300s
- Awake duration range: 10 - 86400 seconds (10s to 24h), default 1800s
- Uses RangeEditorActivity for duration settings with prettified time display
- Follows same pattern as LoraWan, Inference, and other existing settings sections

## Previous Changes

### Bluetooth Pairing Fix (January 2026)
**Issue**: Android phones experiencing connection failures and crashes when connecting to ELOC devices.

**Root Cause**: The app was attempting to establish a Bluetooth socket connection before waiting for the pairing/bonding process to complete, creating a race condition.

**Solution**: Implemented proper Bluetooth bonding state management in `DeviceDriver.kt`:
1. Check bonding state before attempting connection
2. Initiate bonding if device is unpaired
3. Wait for bonding completion via broadcast receiver
4. Only connect after successful bonding

**Key Changes**:
- Added `bondingInProgress` flag and pending callbacks
- Created `bondStateReceiver` broadcast receiver
- Modified `connect()` to check `device.bondState` first
- Added `registerBondStateReceiver()` and `proceedWithConnection()` helpers

See `BLUETOOTH_PAIRING_FIX.md` for full documentation.

### Database Upload Optimization (February 2026)
**Issue**: The app was uploading status and config data to Firestore too frequently, filling the database with unnecessary information.

**Previous Behavior**:
- When connecting to ANY ELOC → uploaded status + config to database
- When refreshing status page → uploaded status + config to database  
- When starting recording mode → uploaded status + config, then getStatus again

**Optimized Behavior**:
- **Connect to idle ELOC**: No database upload (just display in UI)
- **Connect to recording ELOC**: Upload status only (no config, no location update)
- **Start recording mode**: Upload status + config with new session ID and location
- **Refresh status page**: No database upload (just display in UI)

**Key Changes**:
1. `DeviceDriver.kt` - Removed automatic `getElocInformation()` call from `setRecordState()`
2. `DeviceActivity.kt` - Modified `onFirstLocationReceived()` to:
   - If device is recording: Call `getStatus()` for DB upload, then `getElocInformation(null, false)` for UI only
   - If device is NOT recording: Call `getElocInformation(location, false)` for UI only (no upload)
3. `DeviceActivity.kt` - Modified `setCommandCompletedCallback` to call `getElocInformation(location, true)` after starting recording mode (uploads to DB)

**Technical Details**:
- `saveNextInfoResponse` parameter in `getElocInformation()` controls DB uploads:
  - `true` = save/upload to Firestore
  - `false` = display in UI only, no upload
- `saveToDatabase` parameter in `getStatus()` controls DB uploads:
  - `true` (default) = save/upload status to Firestore
  - `false` = display in UI only, no upload

**Bug Fix (February 2026)**: Fixed issue where `getStatus` was not uploading when reconnecting to a recording device. The first `getStatus()` call in `showDeviceInfo()` (used just to check recording state) was consuming the upload slot, preventing the second `getStatus()` call in `onFirstLocationReceived()` from uploading. Solution: Added `saveToDatabase` parameter to `getStatus()` function.

### Google Sign-In Implementation (February 2026)
**Issue**: Google Sign-In option was planned but never implemented.

**Solution**: Implemented Google Sign-In using the modern Credential Manager API.

**Key Changes**:
1. `AuthHelper.kt` - Added `signInWithGoogle()`, `handleGoogleSignInResult()`, and `firebaseAuthWithGoogle()` methods
2. `activity_login.xml` - Added "Sign in with Google" button with Material Design styling
3. `activity_register.xml` - Added "Sign in with Google" button
4. `LoginActivity.kt` - Added Google Sign-In handler with `signInWithGoogle()` method
5. `RegisterActivity.kt` - Added Google Sign-In handler
6. `strings.xml` - Added `sign_in_with_google`, `google_sign_in_failed`, `google_sign_in_cancelled`, `web_client_id` strings

**Technical Details**:
- Uses `CredentialManager` API with `GetGoogleIdOption`
- Authenticates with Firebase using `GoogleAuthProvider.getCredential(idToken)`
- Google accounts are automatically verified (skip email verification flow)
- Web Client ID from Firebase: `773327231765-igglaupgt12mct4ii7kiil5ktda2nqfd.apps.googleusercontent.com`

**Configuration Required**:
- Debug SHA-1 fingerprint must be added to Firebase project settings
- SHA-1 for current debug keystore: `74:FD:1E:8A:FA:90:12:7F:4B:D8:C9:06:F0:35:83:CE:AB:E1:BA:04`

## Next Steps

Based on `TODO.md`, the following items need attention:

### Bugs
- [ ] Bluetooth ON/OFF button doesn't work in settings

### Features - Status Page
- [ ] Fix swipe-down refresh (refreshes SD card but not other values)
- [ ] Fix refresh only possible after scrolled down first
- [ ] Show remaining recording time calculation

### Features - Settings Page  
- [ ] Turn ON/OFF Bluetooth toggle not working

### Features - Map
- [ ] Auto-zoom to all ELOC markers (low priority)

### Technical Debt
- [ ] Fix view binding issue with toolbar
- [ ] Investigate Google Maps crashes on some devices (see `maps.log`)
- [ ] Migrate remaining Java code to Kotlin

## Active Decisions and Considerations

### Microphone Type Configuration
The app supports multiple microphone types that must match the ELOC hardware:
- ICS-43434
- SPH0645
- IM69D130
- ICS-43432
- INMP441 (various gain options)

The `MicrophoneType.kt` enum defines these, and settings must be coordinated with firmware.

### Firebase BOM Version
Keeping Firebase BOM at 32.6.0 to maintain API 21 (Android 5.0) support. Do not upgrade without verifying compatibility.

### Portrait-Only Orientation
All activities are locked to portrait orientation for field usability.

## Important Patterns and Preferences

### Error Handling
- Use try-catch with empty catch blocks for non-critical operations
- Log errors via `Logger.kt` with traffic direction indicators
- Show user-friendly error messages via callbacks

### Bluetooth Communication
- Always check `bluetoothSocket?.isConnected` before operations
- Use 15-second timeout for command responses
- EOT byte (0x04) terminates all messages

### State Management
- `DeviceDriver` singleton holds all device state
- UI observes via listener callbacks
- Disconnect clears all state and callbacks

## Learnings and Project Insights

### Bluetooth Pairing
- Android requires BOND_BONDED state before socket connection
- Bonding can fail silently - must handle BOND_NONE after BOND_BONDING
- Some Android ROMs (MIUI) need higher `maxSdkVersion` for legacy permissions

### ELOC Communication
- 512-byte command limit is hardware constraint
- JSON parsing must handle missing/malformed fields gracefully
- Device sends greeting JSON on connect - wait for it before commands

### User Experience
- Field conditions require simple, large UI elements
- Offline functionality is critical (no cellular in forests)
- Battery and SD card status are most-viewed information
