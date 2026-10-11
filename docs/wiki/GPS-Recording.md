# GPS recording

Runs can optionally be recorded with GPS: distance, time, pace, every segment of an interval session and every kilometer. For intervals the phone vibrates at each change. The screen may stay off.

The app reads the device's GPS directly, not Google's location services. Location is used only during a recording and never sent. More under [[Privacy|Privacy]].

## How a recording works

1. Open a session and tap **“Record with GPS”**. The button appears as long as no run is logged for that session.
2. The ready screen shows what the app recognized in the plan, how large your privacy zone is and whether the route will be saved. Tap **Start**.
3. On the first start Android asks for **precise location** and, from Android 13, **notifications**. The notification shows that the service is running, in the app's language.
4. The live screen shows **time, kilometers, current pace** and, for intervals, the current segment with “mm:ss left” and the next one.
5. **Pause** stops the clock, **Lap** sets your own mark (in free runs), **Finish** ends the recording.
6. The summary shows time, distance, pace, the segments you reached, kilometer splits and the route. **“Apply to the run”** pre-fills the session's form. You check the values and save the run as usual. **Discard** throws everything away.

If the recording is very short (under 50 m or under 30 seconds), the app says “Too short” and suggests checking location and open sky.

## Recognizing intervals

The app reads segments from the **session description**, in English or German. Examples it understands:

| English | German |
|---|---|
| `5× (3 min easy run, 1 min walk)` | `5× (3 Min. locker laufen, 1 Min. gehen)` |
| `10 min warm-up, 5× 2 min fast with 1:30 min jog, 10 min cool-down.` | `10 Min. ein, 5× 2 Min. zügig mit 1:30 Min. traben, 10 Min. aus.` |
| `2× 10 min at half-marathon pace with 3 min jog` | `2× 10 Min. im Halbmarathon-Tempo mit 3 Min. traben` |
| `20 min fartlek by feel` | `20 Min. Tempowechsel nach Gefühl` |
| `105 min, the last 20 min at half-marathon pace.` | `105 Min., die letzten 20 Min. im HM-Tempo.` |

If the text matches no pattern, the run is a **free run**. Then laps by hand are possible, and the summary shows your laps as segments. If repetitions name no recovery, the app assumes 2 minutes of jogging. Durations are minutes (`3`) or minutes and seconds (`1:30`). Segments are time-based; the distance per segment is the distance covered in that time. Very short segments (strides, 20 seconds) cannot be measured reliably with GPS and are not evaluated.

If you change a description (see [[Training and plan|Training-and-Plan]], “Adjusting sessions”), the app reads the new text at the next start.

## Vibration cues

Cues for changes in the plan come from the background service and therefore work with the screen off. The lap mark is set by the open app:

| Change | Cue |
|---|---|
| to a fast segment | two short |
| to a recovery | one long |
| end of the plan | three short |
| lap (free run) | one very short |

The notification also shows the new segment.

## Privacy zone

Around the starting point lies an area whose points are **never stored**. Distance and time still count in full. You choose the size in Settings: off, 150 m, 300 m (default), 500 m or 1 km. The radius varies randomly by 15 percent per run, and the center is never stored. The zone also applies at the end of the run, when you are back at the start. It protects against the obvious but is no substitute for caution: someone who knows your route can guess the area.

## Map and route

The route appears as a map in the summary, in the logged run and, overlaid, in the stats. **No map tiles are loaded**; the app calls no servers. What you see is your line, drawn to scale.

| Gesture | Effect |
|---|---|
| Drag with one finger or the mouse | move the map |
| Pinch with two fingers, mouse wheel | zoom around the fingers or the pointer |
| Double tap | zoom in at that spot |
| **+** / **−** | zoom in or out |
| Target button | show the whole route again |
| Corners button | full screen; the same button or Escape leaves it |

The line keeps its width at every zoom level, and a scale bar at the bottom left shows the distance. In the stats, several runs are drawn semi-transparent on top of each other, so streets you run often appear darker.

**Why the line is smooth:** GPS positions jump around by a few meters. Before saving, the app smooths each part of the track with a Kalman filter that trusts precise fixes more than imprecise ones, then draws curves instead of corners. On a noisy test track this halves the average deviation from the real path (5.0 m → 2.5 m) and removes most of the zigzag. Details: [[Analysis engine|Analysis-Engine]]. Runs recorded before this change keep their stored line but are also drawn with curves.

Under Settings you can set “Save route” to “No”. Times, distances and intervals are kept, but there is no map.

## What the GPS label means

| Label | Meaning |
|---|---|
| Searching for GPS … | no fix received yet |
| GPS ok | fresh fix, accuracy better than 25 m |
| GPS inaccurate (±N m) | poor reception |
| No signal | no fix for more than 12 seconds |
| Paused | the recording is paused |
| Service ended | the system ended the background service |

## The app in the background or closed

- If Android only ends the app's **UI** in the background, the service keeps running. When you open the app again it reattaches and shows the run right away. The service also keeps the pauses and laps.
- If the system ends the **whole process** (for example an aggressive battery manager), the fixes are gone. This is intentional: nothing unfinished ever lies on storage. The app then tells you clearly that the recording was ended. Settings, Diagnostics shows why Android ended the app.
- If your phone maker ends background apps early, it usually helps to turn off battery optimization for this app. The setting has different names depending on the manufacturer.

Technical details: [[Android plugin|Android-Plugin]] and [[Troubleshooting|Troubleshooting]].

## Permissions

Precise location, foreground service (type location), notifications, vibration and wake lock. Location and notifications are only requested at the first start of a recording. The wake lock keeps the CPU awake during intervals so the cues come on time.
