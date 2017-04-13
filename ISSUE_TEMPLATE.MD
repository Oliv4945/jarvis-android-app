There is no specific error handling for now, nevertheless debug options are enabled in the apk so:
- If you have ADB (Android Debug Bridge) it is easy thanks to `adb -d logcat "net.iopush.jarvis:*" *:S`
- If not, you need to
  - Install [this application](https://play.google.com/store/apps/details?id=com.tortel.syslog&hl=fr)
  - Start Jarvis (wait for the crash)
  - Start Syslog, then `Take log`
  - It will allow you to send an email, inside the zip please send me the file named logcat.txt.
  - Data should be anonymous if the thickbox is checked (I did not verified) so, if you fear about it before `Take log` you can select `grep the following log:`->`All logs` and in `Grep string` field put `jarvis` (all lower case) and have should have enough info...



To anonymize the log, please consider to use regex below:
- ` \b(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(?1)){3}\b ` # Find valid IPv4 addresses
- ` ^([0-9a-fA-F][0-9a-fA-F]:){5}([0-9a-fA-F][0-9a-fA-F])$ ` # Find valid MAC addresses
