# Changelog (moved out of activeContext.md)

Dated history of completed work, newest first. Entries are moved here verbatim from
`activeContext.md` → 'Recent Changes' once a work stream is finished, so activeContext stays
focused on current work.

### Status uploads: schedule the upload when the file is written (2026-11-05) — app 5.46 / 63

Follow-up to the 5.45 entry below, found by testing it. A Bluetooth session that started AI detection
on ELOC_00247 wrote `status_2026-11-05-11-25-59-…json` into the queue and **nothing was ever
scheduled for it** — the phone sat in standby for ten minutes with no upload. The WorkManager table
held a single record, enqueued at 11:24:50 (app launch, a minute before the status existed) and
already SUCCEEDED; no job, no service start after the save.

**Cause**: 5.45 enqueued `StatusUploadWorker` from inside `StatusUploadService.start()`, and
`DeviceDriver.saveLocal()` only calls that for `InfoType.Status`, or for `InfoType.StatusWithConfig`
once **both** halves are saved (`if (statusSaved && configSaved)`). That session got a status back but
no usable config response, so `configSaved` stayed false and neither the service nor the worker ever
learned there was anything to send. 5.45 covered "attempted and failed"; it did not cover "never
attempted". The old wedge had been masking this, because a stranded file was picked up on the next
app launch.

**Fix**: `FileSystemHelper.saveDataFile()` calls `StatusUploadWorker.enqueue(App.instance)`
immediately after `file.writeText(contents)`. All four call sites are the queue writes in
`saveLocal()` (status, config, map), so every file that enters the queue now schedules its own
durable upload, independent of the DeviceDriver state machine. A status no longer waits on a config
response that may never arrive.

**Still worth a look (not fixed here)**: why a `getConfig` response did not complete in that session,
and more generally that `StatusWithConfig` couples the status upload to a config reply at all. That
coupling is also how the September empty-payload config file came to exist. Uploads no longer depend
on it, but the state machine is still fragile in both directions.

**Verification owed**: on-device confirmation that a job appears within seconds of a status being
written while the phone is asleep. The automated check could not be driven from adb — with the screen
off, `am start` parks on PermissionsSetupActivity and goes no further.

### Status uploads: wedged queue, no durable retry, boot crash (2026-11-05) — app 5.45 / 62

**Symptom**: a status captured on ELOC_00247 at 18:44 on 4 Nov only reached Firestore at 10:44 the
next morning, the moment the app was opened. Opening the app said "An upload is already in progress".
The phone had wifi the whole time.

**Evidence from the phone** (Pixel 7a, Android 17, adb):
- `files/upload_cache/` held one file, `config_2026-09-26-11-37-34-GMT+0200_e_ELOC_00247_r_edsteve2.json`,
  109 bytes, dated **26 September** — a `getConfig` reply whose body was `"payload" : {}`.
- `dumpsys activity services` showed `StatusUploadService` with `createTime=-16h16m` (i.e. 18:42 on
  4 Nov), `isForeground=true`: the service was alive all night and uploaded nothing.
- The live foreground notification still read **"Uploading ELOC status…"** — a string set only inside
  `doUpload()`, immediately before the call. The loop overwrites it with "Retrying in …" within a
  second of `uploadDataFiles()` returning, so the call had been blocked for 15+ minutes, live.
- 09:47 on 5 Nov: Play services was killed for a package update and took the app process with it;
  Android scheduled the service restart with a **2h39m** delay. The 10:44 upload was the user opening
  the app, nothing else.

**Root cause**: `FirestoreHelper.uploadDataFiles()` dropped unusable files with a bare `continue`
without calling `completeListener`, so `successCount + failCount` could never reach
`pendingUploads.size`, `completed` never became true, and `while (wait) { Thread.sleep(1000) }` never
exited. One empty-payload file from September therefore wedged **every** upload run from then on:
`doUpload()` never returned, the 15-minute retry never advanced, and `isRunningTask` stayed `true`
forever — which is what produced "An upload is already in progress".

**Fixes**
1. `FirestoreHelper.uploadDataFiles()` rewritten: unusable files (empty/unreadable payload, unknown
   type) are now **discarded** with a warning instead of skipped, writes are dispatched in parallel
   rather than chained head-to-tail, and the wait is `Tasks.await(whenAllComplete, 60s)`. A run now
   always returns. `getMapDataTimestamp()` lost its spin loop for a bounded `Tasks.await(…, 20s)`.
   Note for future readers: a Firestore `set()` Task completes **only** on server acknowledgement —
   offline it never completes, which is why an unbounded wait was fatal.
2. `FileSystemHelper.saveDataFile()` refuses to queue a response with no content
   (`hasUploadableContent`), so junk cannot enter the queue in the first place.
3. **New `StatusUploadWorker`** (WorkManager, `NetworkType.CONNECTED`, unique work, LINEAR backoff of
   `Preferences.statusUploadInterval`). This is the durable half: WorkManager persists its queue to
   disk, so a pending upload now survives process death and reboot. `work-runtime` was already a
   dependency and was entirely unused.
4. `StatusUploadService` is now a **single attempt** that hands retries to the worker, wrapped in
   `finally { abortService() }` so `isRunningTask` can never stick. It returns `START_NOT_STICKY`, and
   `startForeground()` is wrapped in try/catch. The old retry loop both could not survive a kill and
   burned the Android 15+ six-hour daily `dataSync` budget while it ran.
5. `BootReceiver` enqueues the worker instead of starting the foreground service. Logcat showed the
   app had been **hard-crashing on every reboot** since at least 22 Oct:
   `ForegroundServiceStartNotAllowedException: FGS type dataSync not allowed to start from
   BOOT_COMPLETED!` at `StatusUploadService.onStartCommand(StatusUploadService.kt:176)`.
6. `HomeActivity.prepareStatusUpload()`: the manual **"Upload ELOC status" menu item did nothing**
   unless an upload happened to be running already — the start call sat behind `else if (inBackground)`.
   Fixed; the "already in progress" snack is now only shown when the person asked for it themselves.

**Verification** (hardware, Pixel 7a): the real September file was replanted in `upload_cache` against
the rebuilt app. Logcat: `discarding '…' — empty/unreadable payload` → `nothing to send
(discarded=1 skipped=0)` → `doWork: pending uploads drained` → `Worker result SUCCESS`. Queue empty,
no service left running. **Still owed**: a happy-path upload from a real Bluetooth session, and a
reboot to confirm the boot crash is gone (adb cannot send `BOOT_COMPLETED`; fixed by construction
since the receiver no longer starts a foreground service).

**User-visible change**: the upload notification no longer shows a "Retrying in mm:ss" countdown. A
failed attempt now says "Upload will be retried when there is a connection" and the retry happens in
the background by itself. No wiki page covers upload behaviour today (the app wiki is lockout +
manual user management only) — a page was drafted but not pushed.

### Status-page pull-to-refresh vs. scroll conflict fixed (2026-07-22)
On the device status screen, scrolling up from the bottom often triggered a pull-to-refresh instead
of scrolling — and felt "stuck." Root cause: in `activity_device.xml` the `SwipeRefreshLayout`'s
**direct child is a (non-scrollable) ConstraintLayout**, not the `ScrollView`, so its default
`canChildScrollUp()` always reports "at top" and treats every downward drag as a refresh pull. The
prior workaround toggled `swipeRefreshLayout.isEnabled = (y <= 5)` from an `OnScrollChangeListener` —
racy (read at touch-down before the scroll delta lands), API-gated to M+ (broken on API 21–22), and it
fought the connection-state `isEnabled` toggles. **Fix** (`DeviceActivity.kt`): removed the scroll
listener + its registration + now-unused `import android.os.Build`; added
`swipeRefreshLayout.setOnChildScrollUpCallback { _, _ -> binding.scrollView.canScrollVertically(-1) }`
so refresh only arms when the ScrollView is genuinely at the very top, evaluated at interception time,
on all API levels. Connection-state `isEnabled` toggles (Pending/Inactive/onElocInfoReceived) left as-is.
`compileDebugKotlin` green. **On-device verification still owed** (moved here at user request before the
hardware check): scroll-up from bottom no longer refreshes; pull-to-refresh still fires at the top.

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
