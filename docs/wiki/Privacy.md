# Privacy

The app is built so that it **cannot send anything you don't share yourself**. This page sums up what lives where and is honest about the limits.

## Principles

- All entries live in the app's private storage. There is no server and no account.
- The app calls no servers. It uses no Google services, no ads and no trackers. The font ships with the app. The map shows your own route on a Berlin basemap that ships inside the app; no map tiles are downloaded.
- Location is used only during a recording and never sent. The app reads the device's GPS directly (Android `LocationManager`), not Google's location services.
- Android does not back up the app data to the cloud. Your entries only leave the device if you share a backup or a log yourself.
- Voice announcements (optional, off by default) are spoken by the phone's own text-to-speech. The app hands it only the sentence, for example “1 kilometre. Pace six ten. Average six eleven.”, never a position. It prefers an installed voice that works offline. If your speech engine only has an online English voice, that engine decides what it sends, not this app; install an offline English voice to avoid that.

## What lives where

| Data | Where | How long |
|---|---|---|
| Entries (runs, habits, learnings, …) and the language setting | `localStorage` of the app, key `hm2027-state` | until deleted or uninstalled |
| Raw GPS fixes | memory of the background service | until the end of the recording, never on storage |
| Running recording (UI state) | `localStorage`, key `hm2027-trk`, and memory of the service | until the end of the recording. Contains pauses and laps, no places. |
| Applied result before saving | `localStorage`, key `hm2027-trk-pend` | until you save or discard the run |
| Route of a saved run | in the run, as a smoothed, encoded line, without the privacy zone | with the run |
| Diagnostic log | `files/diagnose.log` in private storage, at most about 200 lines | until you delete it or switch it off |

## Privacy zone

An adjustable area (300 m by default) around the start is never stored. Distance and time still count in full.

- The center is the first fix of the run. The radius varies randomly by 15 percent per run.
- All points inside are removed before saving, also at the end of the run. The check runs on the smoothed line, so every stored point is guaranteed to be outside the zone.
- The center is never stored.

**Limit:** the edge of the gap reveals the scale, not the exact place. Someone who knows your route can guess the area. The privacy zone protects against the obvious and is no substitute for caution. If you want to be sure, turn off “Save route”.

## Diagnostic log

The log helps find out why a recording stops, without you revealing anything personal.

**Included:**

- event words and counters, for example “service created”, “wake lock ok”, “time to first GPS fix in seconds”, “memory warning level 15”
- the Android version
- why Android last ended the process (for example low memory or a crash), with a rough indication of how long ago
- on a crash, the class and code location of the exception, not its message
- times only as seconds since the process started

**Never included:** places, coordinates, distances, dates, clock times, names, device identifiers, model or manufacturer.

**Enforced in code:** the log only allows a small set of characters, limits every line to 100 characters and replaces anything that looks like a coordinate (a number with three or more decimals).

**Your control:** under Settings, Diagnostics you can switch it on or off, view, share and delete it. It only leaves the device when you choose “Share”. Switching it off deletes the file.

More: [[Troubleshooting|Troubleshooting]] and [[Android plugin|Android-Plugin]].

## Permissions

| Permission | Why |
|---|---|
| Precise location | GPS recording. Only requested at the first start of a recording. |
| Foreground service, type location | so recording continues while the screen is off |
| Notifications | the notice that the service is running (Android 13 and newer) |
| Vibration | cues during intervals |
| Wake lock | keep the CPU awake so cues come on time, and for the few seconds a voice announcement takes |

The build checks on every run that these permissions are in the final manifest, see [[Build and release|Build-and-Release]].

## Enforced at build time

The build removes the Google services plugin, turns off `allowBackup` and stops if either fails. This protects against Google services or cloud backup accidentally coming back.
