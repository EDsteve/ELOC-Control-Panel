 Bugs:
 
 Status page
- Recording shows <1min even when not recording.
- App version doesn't show under MISC
- Bluetooth Recording ON/OFF doesn't show
- the link on the bottom (wildlifebug.com) crashes the app

 
 - Low priority - // todo: fix view binding issue; Why can't it see the toolbar




To-Do:

Start Page
- Map feature: It should zoom to all the ELOC markers automatically - If too complicated. Forget it :)
- "Please update your ELOC Controll Panel app. Your version is not supported any more" - This should show when i choose so in the code (Not a priority and maybe never needed hopefully)

Status page:
- Change all colors to white in the MISC section. No green or red ---> Only if it takes a few clicks for you
- Swipe down refresh problems:
	- It does refresh the SD card but not the recording or other values from the EP32? A manual reconnect refreshes all.
	- refresh only possible after scrolled down first.
- Show possible recording time (calculating from Sample rate and SD space available)
- Bluetooth Recording (On / OFF) not showing but i think my friend who programs the firmware changed something there. Will ask him about it

Settings page:
- Under Advanced Options: Current "Device Name" should show in the "Change Device Name" input field
- Add an "Update firmware" button to advanced menu which sends the command #settings#update with a popup saying: Are you sure? etc... Cancel... OK
