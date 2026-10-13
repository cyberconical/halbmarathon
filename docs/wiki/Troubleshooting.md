# Troubleshooting

## First aid

| Problem | What helps |
|---|---|
| “The app can't record without your precise location” | In the Android settings under Apps, Half Marathon, Permissions, allow precise location. |
| “Location is turned off” | Turn on location in quick settings and start again. |
| “A recording is already running” | The app reattaches to it. Otherwise open it via the run bar at the bottom and finish it. |
| “Searching for GPS …” takes long | Go under open sky and wait one or two minutes. The first fix takes longer after a long break. |
| “GPS inaccurate” or “No signal” | Tall buildings, forest, underpasses. Inaccurate fixes don't count towards distance. |
| “Too short” after finishing | Less than 50 m or 30 seconds recognized. Check location and reception. |
| “The recording was ended” when opening | Android ended the app completely in the background. See below. |
| “Service ended” on the run screen | The system ended the service. Tap “Finish” to keep the data so far. |
| The map can't be moved | Drag inside the map frame, not beside it. To scroll the page, swipe outside the map. |
| The route has a gap near the start | That is the privacy zone. Change it under Settings, GPS & route. |
| The app is suddenly in English | English is the default since this version. Settings, Language, Deutsch. |
| Entries are gone | App data cleared or uninstalled. Load a backup, see [[Backup and data|Backup-and-Data]]. |
| Backup can't be shared | Choose another target app in the share menu. |

## When Android ends the app in the background

Some phone makers end apps in the background despite the foreground service. Then the fixes are gone (they deliberately live only in memory). What helps:

1. In the Android settings under Apps, Half Marathon, Battery, turn off optimization (“Unrestricted” or “Don't optimize”, depending on the manufacturer).
2. Don't swipe the app away if your device is known for this.
3. Look at the diagnostic log. If it says `exit:LOW_MEMORY` or `svc:destroy running=1`, it was the system.

## Reading the diagnostic log

Under Settings, Diagnostics, “View”. It contains only technical events and counters, no places and no data, see [[Privacy|Privacy]]. The number at the front is the time in seconds since the process started. It starts again at 0 for every new process, introduced by `proc:start`.

### A normal run

Roughly what a run without problems looks like. The order within the same second can differ slightly.

```
+0s proc:start api=34
+0s plugin:load
+1s js:boot
+42s js:begin free
+42s svc:create
+42s svc:cmd running=1
+42s svc:fg ok cues=12
+42s svc:gps ok enabled=1
+42s svc:wakelock ok
+42s start:ok
+42s js:begin:ok
+51s fix:first after=9s
+542s fix:n=500
+2442s js:end fixes=2390 dead=0
+2442s svc:stop fixes=2390
```

### Typical patterns

| Pattern | Meaning |
|---|---|
| `svc:wakelock fail SecurityException …` | the `WAKE_LOCK` permission is missing. Recording continues, cues may be late. Fixed in v1.6.1. |
| `crash:…` | a crash. Class and code location help fix it. Please report it. |
| `svc:taskRemoved`, then more `fix:n=…` | app swiped away, the service kept running. All good. |
| `proc:start`, `exit:LOW_MEMORY imp=fgservice` | Android ended the process including the service because of low memory. |
| `exit:USER_STOPPED` or `exit:USER_REQUESTED` | the app was force-stopped or a cleaner app ended it. |
| `exit:CRASH` | crash in the previous process. The `crash:` line before it names the place. |
| `svc:destroy running=1` | the system ended the service, not the person. |
| `svc:trim level=…` repeatedly | memory pressure; the process ending is likely. |
| `js:restore:attached …` | the app reattached after a restart. |
| `js:restore:service-gone` | no service was running when the app opened; the run was lost. |
| `js:restore:status-error try=N` | the bridge was briefly not ready at start. The app tries again. |
| `start:reject GPS_OFF` | location was off. |
| `fix:first after=` very large or missing | poor reception at the start |

### Recording quality and map lines

| Line | Meaning |
|---|---|
| `stats fixes= used= dist= active= pauses=` | points received, points that made the line, distance in m, running time |
| `acc <=10: <=20: <=35: >35(dropped): best= med= worst=` | how accurate the GPS points were (m). Points worse than 35 m are dropped; indoors most are |
| `drop old= paused= jump= still=` | points dropped as too old, during a pause, as a jump, or as standing still |
| `speed doppler= slow(<0.5)= nospeed= max=` | points with a chip speed, how many read as standing, and the top speed |
| `route save= zone= parts= pts= km=` | whether the route is saved, a privacy zone is set, and how much line is left |
| `map:try`, `map:file`, `map:header`, `map:loaded ms=` | the basemap was attempted, the file was found, read, and drawn |
| `map:live-ready inside=1`, `map:live-off`, `map:live-fail`, `map:live-err` | the live map during a recording: drawn on the Berlin map (`inside=1`) or a plain background (`0`); `off` or `fail` means no WebGL or no map file |
| `map:nofile`, `map:nowebgl`, `map:outside`, `map:fail`, `map:err …`, `map:timeout` | why the plain line map is shown instead |

The event words are fixed technical codes and don't change with the language setting.

## Reporting a problem

1. Under Settings, Diagnostics, choose “Share”. The file is called `halbmarathon-diagnose.txt` and starts with a note on what is not in it.
2. Read it once before passing it on. You see exactly what you share.
3. Open an [issue](https://github.com/cyberconical/halbmarathon/issues): what you did, what happened, the app version (Android settings, Apps, Half Marathon) and the log.
4. Afterwards you can delete the log under Settings, Diagnostics.

For developers: [[Android plugin|Android-Plugin]], section “Events”.
