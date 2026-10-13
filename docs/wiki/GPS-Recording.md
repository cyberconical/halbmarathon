# GPS recording

Runs can optionally be recorded with GPS: distance, time, pace and every kilometer, plus the route. The screen may stay off.

The app reads the device's GPS directly, not Google's location services. Location is used only during a recording and never sent. More under [[Privacy|Privacy]].

## How a recording works

1. Open a session and tap **“Record with GPS”**. The button appears as long as no run is logged for that session.
2. The ready screen shows how large your privacy zone is and whether the route will be saved. Tap **Start**.
3. On the first start Android asks for **precise location** and, from Android 13, **notifications**. The notification shows that the service is running, in the app's language.
4. The live screen shows **time, kilometers and current pace**.
5. **Pause** stops the clock, **Finish** ends the recording.
6. The summary shows time, distance, pace, kilometer splits and the route. **“Apply to the run”** pre-fills the session's form. You check the values and save the run as usual. **Discard** throws everything away.

If the recording is very short (under 50 m or under 30 seconds), the app says “Too short” and suggests checking location and open sky.

## One continuous recording

The recording only tracks. It does **not** read the session description, splits the run into segments or vibrates at changes, even if the plan text says “5× (5 min run, 1 min walk)”. There is no lap button either. What you get is distance, time, pace, every kilometer and the route. Whether you walk a bit in between is up to you. If you stop, use **Pause**.

Older versions recognized intervals and vibrated at each change. New recordings no longer store segments (`trk.iv` is empty). Segments of runs recorded earlier stay stored but are no longer shown.

## Privacy zone

Around the starting point lies an area whose points are **never stored**. Distance and time still count in full. You choose the size in Settings: off, 150 m, 300 m (default), 500 m or 1 km. The radius varies randomly by 15 percent per run, and the center is never stored. The zone also applies at the end of the run, when you are back at the start. It protects against the obvious but is no substitute for caution: someone who knows your route can guess the area.

## Map and route

The route appears as a map in the summary, in the logged run and, overlaid, in the stats. **No map tiles are downloaded**; the app calls no servers. A Berlin basemap (OpenStreetMap data) ships inside the app; for routes outside Berlin you see just your line, drawn to scale.

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

Under Settings you can set “Save route” to “No”. Times and distances are kept, but there is no map.

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

- If Android only ends the app's **UI** in the background, the service keeps running. When you open the app again it reattaches and shows the run right away. The service also keeps the pauses.
- If the system ends the **whole process** (for example an aggressive battery manager), the fixes are gone. This is intentional: nothing unfinished ever lies on storage. The app then tells you clearly that the recording was ended. Settings, Diagnostics shows why Android ended the app.
- If your phone maker ends background apps early, it usually helps to turn off battery optimization for this app. The setting has different names depending on the manufacturer.

Technical details: [[Android plugin|Android-Plugin]] and [[Troubleshooting|Troubleshooting]].

## Permissions

Precise location, foreground service (type location) and notifications. Location and notifications are only requested at the first start of a recording.
