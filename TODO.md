 - Low priority - // todo: fix view binding issue; Why can't it see the toolbar
Start Page
- Map feature: It should zoom to all ELCOs and not only to a single device
- "Please update your ELOC Controll Panel app. Your version is not supported any more" - This should show when i choose so in the code

Status page:
- Change all colors to white in the MISC section. No green or red ---> no need
- Swipe down problems:
	- It does refresh the SD card but not the recording or other values from the 	ESP32? A manual reconnect refreshes all.
	- refresh only possible when scrolled down first.
- Show possible recording time (calculating from Sample rate and SD space available)
- make the recording time etc to readable time

Settings page:
- Current Device Name should show in the "Change Device Name" input field
- Add an "Update firmware" button which sends the command #settings#update to advanced menu with a popup saying: