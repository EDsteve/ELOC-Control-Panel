# ELOC Control Panel - Active Context

## Current Work Focus
Memory Bank initialization - documenting the existing codebase for future development sessions.

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
