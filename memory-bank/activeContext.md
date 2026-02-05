# ELOC Control Panel - Active Context

## Current Work Focus
Google Sign-In implementation - completed reimplementation of Google Sign-In using the modern Credential Manager API.

## Recent Changes

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
