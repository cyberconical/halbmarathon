# Android plugin `run-tracker`

The only native part of the app. It lives in `plugins/run-tracker/` and is included in the build. It only does what a web page can't: **read location in the background, vibrate at fixed times and speak while the screen is off.**

## Why its own plugin

The ready-made Capacitor location plugins depend on Google Play Services. The app is meant to run without Google services. So `run-tracker` only uses Android's `LocationManager` with the GPS provider.

## Files

| File | Job |
|---|---|
| `RunTrackerPlugin.java` | bridge to JavaScript: methods, permissions |
| `RunTrackerService.java` | foreground service: read GPS, collect fixes, cues, notification, voice announcements |
| `PaceTracker.java` | counts distance in the background with the same rules as `analyze()` and reports every 500 m or 1 km. Plain Java, testable on a computer |
| `PaceSpeech.java` | the English words of an announcement. Plain Java, testable on a computer |
| `Announcer.java` | the phone's text-to-speech: English voice, other audio turned down, short wake lock |
| `RunLog.java` | anonymous diagnostic log |
| `AndroidManifest.xml` | permissions and service entry (type `location`) |
| `build.gradle`, `package.json` | integration into Capacitor 6 |

## Methods for JavaScript

Called from `index.html` via `Capacitor.Plugins.RunTracker`.

| Method | Parameters | Returns | Purpose |
|---|---|---|---|
| `checkPermissions()` / `requestPermissions()` | `{permissions: ["location" \| "notifications"]}` | status per alias | from the base class |
| `start` | `cues` (JSON text), `meta` (session ID), `lang` (`"en"` or `"de"`), `voice` (`0`, `500` or `1000`: metres between announcements, `0` = off), `maxAcc` (accepted GPS error in metres, 35 or more in test mode) | `{startedAt}` | start the service. Error codes: `PERMISSION`, `GPS_OFF`, `RUNNING`, `START_FAILED` |
| `testVoice` | – | – | speak a sample announcement. Error codes: `NO_ENGLISH` (no English voice installed), `UNAVAILABLE` (no speech engine) |
| `pause` / `resume` | – | `{t}` | cancel and reschedule cues |
| `stop` | – | `{count}` | end the recording; fixes stay available until the next start |
| `getFixes` | `from` | `{fixes, next, running, paused, gps, voice}` | fixes from position `from`, at most 2,000 per call. `voice` is `off`, `init`, `ready`, `no_english` or `unavailable` |
| `getStatus` | – | `{running, paused, gps, gpsEnabled, count, startedAt, meta, cues, state}` | state for reattaching |
| `saveState` | `state` (JSON text) | – | store the UI's small state in the service |
| `diagLog` | `e` (text) | – | write a UI event to the log |
| `getLog` | – | `{text, enabled}` | read the log |
| `clearLog` | – | – | delete the log |
| `setLogEnabled` | `on` | – | switch the log on or off (off deletes it) |

The app shows its own, translated error messages based on the error codes. The plugin's English reject messages are only for developers.

## The service

- Starts as a **foreground service of type `location`** with a notification in the channel “Run recording” (low importance). Title, text and channel name follow the app's language (`lang` passed with `start`). Tapping the notification opens the app.
- Requests a GPS fix every **1,000 ms**, without a minimum distance.
- Stores fixes as `[time, lat, lon, accuracy, speed]` in a list **only in memory**, at most 30,000 (over 8 hours at one fix per second).
- Schedules the cues on active running time. On pause they are cancelled, on resume rescheduled. The cue texts come from the app in its language.
- Holds a **wake lock** (at most 4 hours) for runs with cues, so cues come on time with the screen off. A failure there is logged and does not stop the recording.
- **Voice announcements** (if `voice` is 500 or 1000): every fix also goes to `PaceTracker`, which uses the same distance rules as the app (accuracy limit, jump filter, GPS speed first, noise limit, test mode). When a mark is crossed, `Announcer` speaks it in English, see [[GPS recording|GPS-Recording]]. The speech engine is started with the recording and released when it ends. If it is missing or has no English voice, `getFixes` reports it and the recording continues without speech. Nothing is counted or spoken while paused.
- Does **not** restart after the process ended (`START_NOT_STICKY`). Without fixes there is nothing to continue.
- If Android starts the service only to end it right away, it still calls `startForeground()`. Otherwise Android would end the app for a broken promise.

The state lives in static fields in the same process as the plugin. If only the UI dies (WebView, activity), it survives. If the process dies, it is gone.

## Permissions

| Permission | Why |
|---|---|
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | GPS. Android 12 and newer require the approximate location too. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION` | service in the foreground of type location |
| `POST_NOTIFICATIONS` | the service notification on Android 13 and newer |
| `VIBRATE` | cues |
| `WAKE_LOCK` | keep the CPU awake for punctual cues, and for the few seconds a voice announcement takes. **Without it, Android ends the app when an interval recording starts.** |

The build checks the merged manifest for exactly this list, see [[Build and release|Build-and-Release]].

Besides permissions the manifest has a `<queries>` entry for `android.intent.action.TTS_SERVICE`. From Android 11 an app can only find the phone's speech engine if it asks for it there. Without it, voice announcements would silently not work.

## Diagnostic log (`RunLog`)

A small, anonymous log for troubleshooting. The rules are enforced in code, not just documented:

- **Where:** `files/diagnose.log` in the app's private storage. At most 200 lines, then trimmed to the newest 150.
- **Time:** only seconds since the process started (`+12s`), no clock time and no date.
- **Characters:** only letters a to z, digits, space and `_ : . = + - @ $ ( )`. Everything else becomes `?`. At most 100 characters per line.
- **Coordinate guard:** every number with three or more decimals is replaced with `#`.
- **Crashes:** class and code location of the exception (`SecurityException top=… app=RunTrackerService.startTracking:212`), never its message. The normal crash path then continues.
- **Why the last process ended:** from Android 11 via `ApplicationExitInfo`, for example `exit:LOW_MEMORY imp=fgservice vor=3min`. Each reason is noted only once.
- **Switch:** in `SharedPreferences` (`halbmarathon_diag`). Switching off deletes the file.

### Events

| Event | Meaning |
|---|---|
| `proc:start api=N` | new process, Android API level |
| `exit:REASON imp=… vor=Nmin` | why Android ended the previous process, N minutes ago |
| `plugin:load` | plugin loaded (UI started) |
| `start:ok`, `start:reject CODE`, `start:fail …` | start of a recording |
| `svc:create`, `svc:cmd running=…` | service created and started |
| `svc:fg ok cues=N`, `svc:fg fail …` | foreground reached or not |
| `svc:gps ok enabled=…`, `svc:gps fail …` | GPS requested |
| `svc:wakelock ok`, `svc:wakelock fail …` | wake lock |
| `fix:first after=Ns`, `fix:n=N` | first fix after N seconds, counter every 500 fixes |
| `gps:off`, `gps:back` | GPS switched off or back |
| `svc:voice step=N maxacc=M`, `svc:voice fail …` | voice announcements started (or failed) |
| `voice:ready lang=en_US`, `voice:no-english`, `voice:init-fail` | the speech engine is ready, has no English voice, or did not start |
| `voice:say n=N` | announcement number N was spoken (a counter only) |
| `voice:test` | the voice test was pressed |
| `voice:error …`, `voice:speak-error …`, `voice:setup-error …` | a problem with speech. The recording continues without it |
| `svc:pause`, `svc:resume`, `svc:stop fixes=N` | controls |
| `svc:taskRemoved` | app swiped away from recents, service keeps running |
| `svc:trim level=N`, `svc:lowMemory` | memory pressure |
| `svc:destroy running=1` | **the system** ended the service (with `running=0` it was the person) |
| `crash:…` | crash with class and code location |
| `js:boot`, `js:begin …`, `js:restore:…`, `js:end …` | events of the UI |

(`vor` is German for “ago”; event words stay unchanged so older logs read the same.)

How to read a log: [[Troubleshooting|Troubleshooting]].

### Adding your own events

- In Java: `RunLog.add("area:what n=" + number);`
- In `index.html`: `hmlog("area:what n=" + number);`

Only fixed words and small numbers. Never places, points in time, text people typed or session identifiers.
