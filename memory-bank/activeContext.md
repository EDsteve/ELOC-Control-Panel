# ELOC Control Panel - Active Context

## Current Work Focus
**Best-GPS-source recording + 15 s refresh (2026-07-22, 5.43/61) — code-complete, `compileDebugKotlin`
green; needs on-device test against firmware V1.54.** Extends the 2026-07-21 GPS-accuracy work below: the
merged phone-vs-ELOC comparison previously drove only the *gauge display* — the location actually recorded
/uploaded was still always the phone's. Now the more accurate source is recorded. Changes: `driver/Gps.kt`
gains `latitude`/`longitude` (nullable, latched like the firmware fields) + `hasLocation`;
`driver/DeviceDriver.kt` parses the `gps/lat` + `gps/lon` status keys (`KEY_GPS_LAT`/`KEY_GPS_LON`, NaN
sentinel → null when absent); `DeviceActivity` factors the comparison into `chooseGpsSource()` (PHONE/ELOC
/NONE) reused by `updateGpsViews()` and a new `effectiveGpsData()` — the ELOC only wins with a live fix
AND real coordinates, ties go to phone. `effectiveGpsData()` now feeds the three location call sites (the
record-start `getElocInformation`, the display-mode `getElocInformation`, and `setRecordState`) and the
record-accuracy gate. `AUTO_REFRESH_INTERVAL_MS` 30 s → **15 s** so the ELOC side of the comparison stays
fresher during field setup (skip-if-busy gating unchanged, so a slow `getStatus` under AI load just defers
the next tick). `build.gradle` bumped 5.42/60 → 5.43/61. Backward compatible: pre-V1.54 firmware sends no
`lat`/`lon`/`hdop` → `hasLocation` false, ELOC never wins → phone-only as before. **Owed**: on-device test
of the ELOC-wins path (needs a real device fix more accurate than the phone).

---

**GPS live accuracy & time-source (2026-07-21, 5.42/60) — code-complete, `compileDebugKotlin` green;
needs on-device test against firmware V1.54.** Counterpart of the same-day firmware work (firmware repo
`memory-bank/activeContext.md` + `changelog.md`). The 30 s status auto-refresh added earlier exposed two
problems this fixes: (1) the firmware's latched `hasFix` showed "Fix (9 sats)" indoors then a
contradictory "Fix (0 sats)" — firmware V1.54 now live-gates it; (2) the GPS gauge only showed **phone**
accuracy and the ELOC Time row didn't say whether the clock was GPS- or phone-set. App changes:
`driver/Gps.kt` (`UERE_METERS=5.0`, `fixAgeS`, `hdop`, derived `accuracyMeters`), `driver/General.kt`
(`deviceTimeSource`/`deviceTzSource`), `driver/DeviceDriver.kt` (parse new keys `fixAge[s]`, `hdop`,
`timeSource`, `tzSource`), `DeviceActivity.updateGpsViews()` **merged phone-vs-ELOC gauge** (shows the
better accuracy in meters, labels the source), ELOC Time **source suffix** ("· GPS"/"· Phone") +
`updateGpsViews()` now called at the end of `setStatusInfo()` so 30 s refreshes repaint the gauge, and a
**mandatory `synchronizeTime()` redesign** — the live-gated `hasFix` broke the old skip heuristic, so it
now prefers the firmware's `timeSource`/`tzSource` markers (skip only when time is GPS-set and TZ isn't
the compile default), falling back to the old `timeSynced && hasFix` on pre-1.54 firmware. New strings:
`accuracy_source_phone/eloc`, `time_with_source`, `time_source_gps/phone`. Backward compatible: on old
firmware the new keys default to ""/0.0 → phone-only gauge, no time suffix, old skip heuristic. **R3**:
the `synchronizeTime()` redesign must ship with V1.54 (same release train) or the app pins phone TZ at
every connect. `build.gradle` was already at 5.42/60. **Owed**: on-device test (5.42 vs fw V1.54, plus
the 5.41-vs-1.54 and 5.42-vs-1.53 compat cells).

---

**Recording Scheduler driver + UI — REVERTED, not shipped (removed 2026-07-21).** The scheduler app
work (`driver/Scheduler.kt`, `SchedulerActivity`, `ScheduleEntryEditorActivity`, the DeviceSettings
"Recording Schedule" section, `Command`/`JsonHelper` support) was code-complete but never
on-device-verified. It was surgically removed so the V1.54 GPS-accuracy/time-source work (5.42) could
ship on its own (user decision; firmware scheduler pulled at the same time). Entangled files
(`DeviceDriver.kt`, `DeviceSettingsActivity.kt`, `Command.kt`, `JsonHelper.kt`, manifest, layouts,
`strings.xml`) were reverted to HEAD with the V1.54 hunks (`isIdle`, GPS/time parsing) re-applied;
`compileDebugKotlin` green after removal. **Full pre-removal state is backed up** (git-diff patch +
untracked files) under the session scratchpad `sched-removal-backup/app/` — reapply to resurrect.

Firmware update over Bluetooth (July 2026) — Phase 2 MVP implemented plus the 2026-07-10 review
fixes (see below); needs real-hardware verification against firmware V1.51 (full matrix: happy
path, resume after BT kill incl. fully-staged resume, downgrade, refusals, rollback). Phase 3
(Firestore distribution + "update available" badge) is designed but not started. Also: device
screen redesign — extend the design language to the remaining screens later.

## Recent Changes

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
`compileDebugKotlin` green. **Owed:** on-device check — scroll-up from bottom no longer refreshes;
pull-to-refresh still fires at the top.

### GPS live accuracy & time-source (2026-07-21, 5.42/60)
See Current Work Focus above for the full description. Files: `driver/Gps.kt` (companion `UERE_METERS`,
`fixAgeS`/`hdop` fields + `accuracyMeters` derived getter, `reset()`), `driver/General.kt`
(`deviceTimeSource`/`deviceTzSource` + reset), `driver/DeviceDriver.kt` (`KEY_GPS_FIX_AGE`/`KEY_GPS_HDOP`
/`KEY_DEVICE_TIME_SOURCE`/`KEY_DEVICE_TZ_SOURCE` constants + `parseStatus` wiring; `fixAge[s]` parsed with
a −1.0 default, `hdop` with 0.0), `activities/themable/DeviceActivity.kt` (`updateGpsViews()` merged
gauge, `setStatusInfo()` time suffix + trailing `updateGpsViews()` call, `synchronizeTime()` redesign),
`res/values/strings.xml` (5 new strings). No `build.gradle` change (already 5.42/60).

### Recording Scheduler driver + UI (2026-07-14) — REVERTED 2026-07-21 (see Current Work Focus)
The scheduler app work was code-complete but never on-device-verified, and was removed before the V1.54
push. Backup under the session scratchpad `sched-removal-backup/app/`.

### GPS accuracy gauge color inversion fixed (2026-07-10)
`SimpleGauge.getValueColor()`'s errorMode branch permanently mutated the member
`criticalColor = Color.RED`. The GPS gauge inverts its scale by swapping XML attrs
(`criticalColor=green`, `normalColor=red`, since low meters = good), so any session that
*started* without a GPS fix (accuracy −1 → errorMode) destroyed the swap: once a fix arrived,
good accuracy rendered red. Fix in `widgets/SimpleGauge.kt`: errorMode now returns/paints hard
`Color.RED` without touching the configured colors. Sessions that never hit errorMode were
always correct, which is why the bug only appeared "when there was no GPS fix at first".

### Firmware update — review fixes (2026-07-10, alongside firmware V1.51)
1. **Fully-staged resume deadlock fixed** (`FirmwareUpdater`): when Begin returns
   `resumeOffset >= totalBytes` (previous session staged 100% but the final ack was lost),
   `streamFrames` used to send nothing and fail all 5 attempts while the firmware sat in binary
   mode — permanently unrecoverable. Now: firmware ≥ V1.51 answers Begin with
   `payload/state == "staged"` → skip straight to `setFwUpdateApply`; older V1.47–V1.50 firmware
   (no `state` field) → `finishFullyStagedResume()` sends the len==0 end-of-stream sentinel and
   treats its `"staged"` ack as transfer complete.
2. **Variant guard hardened** (`FirmwareUpdateActivity`): file name now comes from
   `OpenableColumns.DISPLAY_NAME` (SAF `lastPathSegment` is an opaque `msf:<id>` for the
   Downloads provider, which silently reduced the ei/no-ai guard to a soft warning).
3. Firmware side (V1.51) additionally refuses images whose `project_name` doesn't match the
   running image. Deferred minor findings from the review are listed in the firmware repo's
   `memory-bank/activeContext.md`.

### Firmware update over Bluetooth — Phase 2 MVP (2026-07-06)
Counterpart of firmware V1.47 Phases 0+1 (plan: firmware repo
`README-FirmwareUpdate-From-App-Plan.md`). New flow: Device Settings → Advanced → **Firmware
Update** (entry hidden unless `getStatus` `device/fwUpdateProto ≥ 1`).

1. **`driver/FirmwareUpdater.kt`** — transfer engine (object, own worker thread):
   `setFwUpdateBegin#meta={size,sha256,version,variant,chunkSize:4096}` → streams stop-and-wait
   binary frames `[seq:u16 LE][len:u16 LE][payload][crc32:u32 LE]` (CRC via `java.util.zip.CRC32`
   over seq+len+payload; len==0 = abort sentinel) → per-frame `cmd:"fwFrame"` EOT-JSON acks
   (10 s timeout) → `setFwUpdateApply` → device double-reboots → reconnect loop (150 s) →
   `getStatus` version comparison (Success / RolledBack). BT drops or firmware NAKs recover by
   reconnect + re-Begin, which returns the device's `resumeOffset`. Also `FirmwareImage` helper:
   validates the ESP image magic locally and reads the embedded `esp_app_desc_t`
   (version/project name) + SHA-256.
2. **`DeviceDriver` additions** (single-socket rule kept): `writeRaw()` (frames only),
   `fwFrameListener` (acks routed out of `listenForData()` before normal interception),
   `firmwareTransferActive` (command processor holds queued commands back during binary mode —
   a stray command would be misparsed by the firmware as frame bytes), `deviceAddress`,
   `isConnected`; `General` gained `fwUpdateProto`/`buildVariant` (parsed from getStatus).
3. **`services/FirmwareUpdateService.kt`** — foreground service (dataSync, like
   `StatusUploadService`) with progress notification + partial wake lock (30 min cap) so
   screen-off can't kill a transfer; terminal notification for success/rollback/cancel/failure.
   New `WAKE_LOCK` permission.
4. **`FirmwareUpdateActivity`** (+ `activity_firmware_update.xml`, ~30 new strings): SAF file
   picker (copies to `cacheDir/fwupdate.bin`), shows installed vs. file version/variant/size/
   SHA-256, **variant guard** (release filename convention `…-ei/-no-ai`; refuses definite
   mismatch, warns if undeterminable), recording-must-be-off preflight with one-tap stop,
   progress + rate + ETA, does NOT auto-close on disconnect (expected during the update cycle).

**Not yet done:** hardware test of the full matrix (happy path, resume after BT kill, downgrade,
refusals, rollback); Phase 3 (Firestore `firmware_releases` distribution + "update available"
badge) is designed in the plan but not started.

### Status page toggle-section polish (July 2026)
1. **LoRa header** — when LoRa is enabled, the subtitle "Long range wireless communication" is replaced with live signal status ("Excellent · -85.5 dBm", "Not joined", or "No signal"); the antenna icon is tinted by signal strength (green/amber/orange/red via `signalColorRes()`). Disabled → static subtitle restored.
2. **Scheduler header** — when active, subtitle shows "{awake} on · {sleep} off" (from duty-cycle timings) instead of "Duty-cycled recording schedule".
3. **Section icons gray when inactive** — `applySectionState()` now also tints the header icon (LoRa/Scheduler/Intruder): green when enabled, gray (`ui_text_secondary`) when off. Icons default to gray in XML.
4. Gauge ring thickness +15% (`SimpleGauge` 0.0325 → 0.0374 of smallest side).

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

### Older completed entries have moved to `changelog.md` (same folder)
Status stale-timestamp fix (March 2026), map document new fields, LoRa RSSI indicator, duty-cycle
settings (February 2026), Bluetooth pairing fix (January 2026), database upload optimization,
Google Sign-In (February 2026).

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
