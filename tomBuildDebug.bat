cls
call gradlew installDebug
if errorlevel 1 goto end 


call adb logcat -b all -c
call adb logcat  elocApp:V *:S
:end


