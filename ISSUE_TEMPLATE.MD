There is no specific error handling for now, neverthelless I leaved debug options enabled in the apk so:
- If use use ADB (Android Debug Bridge) it is easy thanks to `adb -d logcat "net.iopush.jarvis:*" *:S`
- If not, you need to
  - Install [this application](https://play.google.com/store/apps/details?id=com.tortel.syslog&hl=fr)
  - Start Jarvis (wait for the crash)
  - Start Syslog, then `Take log`
  - It will allow you to send an email, inside the zip please send me the file named logcat.txt.
  - Data should be anonymous (I did not verified) so if you fear about it before `Take log` you can select `grep the following log:`->`All logs` and in `Grep string` field put `jarvis` (all lower case) and have should have enough info...
