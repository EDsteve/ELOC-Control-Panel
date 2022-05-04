just use tomdebug.bat
call gradlew installDebug
if errorlevel 1 goto end 
call adb logcat elocApp:V *:S
:end
