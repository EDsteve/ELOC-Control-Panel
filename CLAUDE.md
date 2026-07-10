# CLAUDE.md — ELOC Control Panel (Android app)

This file orients Claude Code (and other AI agents) working in the Android app. For how this app fits into
the wider platform, see the [root map](../../CLAUDE.md).

## Read the memory bank first

This project already keeps authoritative long-form docs in `memory-bank/` (the Cline convention, governed
by [`.clinerules`](.clinerules)). Read them before any non-trivial task — they are the source of truth for
intent, architecture, and current state:

- `memory-bank/projectbrief.md` — scope, core functions, target devices, constraints
- `memory-bank/productContext.md` — why the app exists, UX goals
- `memory-bank/systemPatterns.md` — architecture, `DeviceDriver` singleton, command-queue & listener patterns
- `memory-bank/techContext.md` — stack, dependencies, permissions, BT constraints, JSON command/response format
- `memory-bank/activeContext.md` — current work focus, recent changes, decisions in flight
- `memory-bank/progress.md` — what works, what's left, known issues
- `memory-bank/changelog.md` — dated history of *completed* work, moved out of `activeContext.md`; read only when you need the history

Per `.clinerules`: read all six core files on every task, and after a significant change update
`activeContext.md` and `progress.md`. When a work stream is *finished*, move its dated entry from
`activeContext.md` → `changelog.md` (verbatim, newest first) so activeContext stays focused on current
work. The phrase **"update memory bank"** means review *all* memory-bank files.

Also useful: [`BLUETOOTH_PAIRING_FIX.md`](BLUETOOTH_PAIRING_FIX.md) (pairing edge cases),
[`TODO.md`](TODO.md), and [`README.md`](README.md).

## What this app is

Android control panel for **ELOC 3.0+** bioacoustic recorders. It connects to a device over **Bluetooth
Classic SPP** (not BLE) and lets a field tech configure it, start/stop recording, read live status, and
upload status/config to Firebase. Maps use **Google Maps** (the *web* dashboard uses Leaflet — don't
confuse them).

- App id: `de.eloc.eloc_control_panel_2` (namespace `de.eloc.eloc_control_panel`); version `5.41` (versionCode 59).
- Kotlin-first, with legacy Java under `app/src/main/java/.../old/`.
- compileSdk/targetSdk 35, minSdk 21 (Android 5.0+); Java 8 desugaring; View Binding + BuildConfig enabled.
- Firebase BOM pinned at **32.6.0** to keep API-21 support — don't bump without checking min-SDK impact.
- A separate legacy app (`de.eloc.eloc_control_panel`) targets ELOC 2.7; this repo is 3.0+.

## Build / run

```bash
./gradlew assembleDebug      # debug APK (minify off)
./gradlew assembleRelease    # release (R8 minify + shrinkResources)
./gradlew installDebug       # build + install on a connected device
```

Config files needed locally (not all committed):
- `app/google-services.json` — Firebase config.
- `local.properties` — Android SDK path + Google Maps API key (via the secrets-gradle-plugin).
- Release signing — see `../keystore.properties` and the `*.jks` keystores in the parent `App/` folder.

There is no automated test suite that exercises Bluetooth — **BT features require real ELOC hardware**;
verify manually on a physical device.

## Architecture in one screen

Layered, with a single Bluetooth connection owned by a Kotlin `object` singleton. Details in
`memory-bank/systemPatterns.md`.

- **`driver/DeviceDriver.kt`** — the singleton that owns the one `BluetoothSocket`, queues commands,
  processes them sequentially with timeouts, parses EOT-terminated JSON responses, and updates component
  state. New device features hang off here.
- **Component objects** under `driver/` mirror the device's config/status sections: `Microphone`,
  `Battery`, `SdCard`, `General`, `Session`, `Inference` (AI detection), `Intruder`, `Logs`, `BtConfig`,
  `Cpu`, `LoraWan` (config + signal strength), `DutyCycle`.
- **Activities** (`activities/themable/`) all extend `ThemableActivity`: `HomeActivity` (device list + map),
  `DeviceActivity` (status), `DeviceSettingsActivity` + `editors/`, `MapActivity`, account/media screens.
- **`data/helpers/`** — `BluetoothHelper`, `LocationHelper`, and `firebase/` (`AuthHelper`,
  `FirestoreHelper`, `StorageHelper`); `data/util/Preferences.kt` wraps SharedPreferences.
- **`services/StatusUploadService.kt`** — background WorkManager-style upload of cached status/config files
  to Firestore, deleting local files on success.
- **Listener maps** (`connectionChanged`, `onSetCommandCompleted`, `onGetCommandCompleted`) keep UI loosely
  coupled to the driver.

## Device command protocol (app ↔ firmware)

Classic SPP, **512-byte max** per command, **EOT (0x04)** message terminator, RFCOMM UUID
`00001101-0000-1000-8000-00805F9B34FB`. Commands are JSON `{"id", "cmd"}`; responses are EOT-terminated
JSON `{"id", "cmd", "ecode", "payload": {...}}`. Config/status values are read with path-based extraction
(e.g. `payload/microphone/MicSampleRate`). Core commands: `getStatus`, `getConfig`, `setConfig`, `setTime`,
`setRecordMode`.

**Firmware update over BT** (firmware ≥ V1.47, advertised via `getStatus` `device/fwUpdateProto`):
`setFwUpdateBegin#meta={size,sha256,version,variant,chunkSize}` switches the link into a **binary frame
mode** — the app streams `[seq:u16 LE][len:u16 LE][payload][crc32:u32 LE]` frames (CRC32 over
seq+len+payload, `java.util.zip.CRC32`; `len==0` = end-of-stream sentinel), stop-and-wait against
`cmd:"fwFrame"` EOT-JSON acks, then `getFwUpdateStatus` / `setFwUpdateAbort` / `setFwUpdateApply`.
While a transfer runs, **no normal commands may be written** (the firmware bypasses its command parser);
`DeviceDriver.firmwareTransferActive` enforces this. Engine: `driver/FirmwareUpdater.kt`; UI:
`FirmwareUpdateActivity` (Device Settings → Advanced).

Authoritative protocol specs live in the **parent `App/` folder** (alongside this project):
[`../API_Protocol.txt`](../API_Protocol.txt), [`../Design_cmdInterface.txt`](../Design_cmdInterface.txt),
[`../NewCommands-Shadow.txt`](../NewCommands-Shadow.txt). The matching firmware side is the
`Commands` library in the [firmware repo](../../Firmware/ELOC-3.0/eloc610LowPowerPartition/).

## Conventions

- Bluetooth transport is **Classic SPP** for firmware compatibility — don't migrate to BLE without a
  coordinated firmware change.
- Activities pair with `activity_*.xml`; reusable layouts are `layout_*.xml`; user-facing strings in
  `strings.xml`, technical strings in `code_strings.xml`.
- Keep the single-connection / command-queue model — don't open parallel sockets or write to the socket
  outside `DeviceDriver`.

The matching firmware lives at `../../Firmware/ELOC-3.0/eloc610LowPowerPartition/`
(absolute: `C:\Development\ELOC\Firmware\ELOC-3.0\eloc610LowPowerPartition`).
