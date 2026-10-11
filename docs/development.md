# Development and customization

This page complements the [README](../README.md). It is for anyone who wants to customize, build or publish the app, with or without programming experience. The [wiki](https://github.com/cyberconical/halbmarathon/wiki) goes deeper; its pages live in [`docs/wiki/`](wiki/).

## Customizing the plan

You set the most important values directly in the app, under Settings (icon at the top right):

- Language
- Race day and goal time
- Body part to watch (for example knee) or none
- Extra sessions on Monday, Tuesday and Friday
- Records, earlier records and goals

The structure of the plan lives in `index.html`: `PLAN` holds a title plus the Wednesday and Sunday run for each week, `PHASES` describes the training phases. Every text there is bilingual, `bi("English", "Deutsch")`. With AI this works without coding skills, see [Download and customize with AI](#download-and-customize-with-ai). Publish a new release after a change.

New installations start with sensible defaults: English, only strength training on Monday, no watched body part, and a race day 27 weeks after next Monday.

## Download and customize with AI

The app consists of one file, `index.html`, and the font in the `fonts` folder. That makes it easy to change with AI assistants such as Claude, ChatGPT or Gemini, even without coding skills and even on a phone.

### 1. Download

- **Just the app:** In this repository, tap `index.html` and choose “Download raw file” at the top right. Without the `fonts` folder next to it, the app uses the system font.
- **The whole project:** Tap “Code” and then “Download ZIP”. With Git: `git clone https://github.com/cyberconical/halbmarathon.git`

### 2. Describe the change

1. Upload `index.html` to an AI chat. At about 170 KB it fits into current models.
2. Use the template below and describe one change per request.
3. Ask for the complete new file, not snippets.

```
You get the file index.html of my training app. It is a single HTML file
(HTML, CSS, JavaScript, no frameworks) and runs as an Android app via Capacitor.

Change the following: <your wish>

Rules:
- Return the complete, changed index.html, not just snippets.
- Saved data must stay compatible. The storage key "hm2027-state" and the
  existing fields (logs, strength, alt, habits, habitLog, learnings, profile,
  records, goals, workouts, dur, edits, gps, updatedAt) must not be renamed or
  removed. Stored values such as "Kraft", "Ruhetag" or "gut" are tokens and stay
  unchanged. New fields are optional and get a default in normalize().
- The script IDs "saved-data", "i18n", "engine" and "app" and the backup function
  stay. The script "saved-data" stays empty (null) in the original.
- Pure calculations belong in the script "engine" (HMTrk), display and storage in "app".
- All user-facing text is English inside tr("…"). Add the German translation of
  every new text to the catalog in the script "i18n". Plan texts use bi("English", "Deutsch").
- Design: black and white, minimal, use the existing CSS variables. Easy to use
  with a thumb on a phone.
- No external libraries, no network calls and no Google services (no Play
  Services, no Firebase). The font lives locally in the fonts folder.
- No places, dates or names in diagnostic logs (hmlog).
- At the end, tell me in two sentences what you changed and how I test it.
```

Ideas: another choice when logging a run (for example weather), a new chart, a different weekly plan, a new tab, a third language.

### 3. Overview for AI and humans

| Part of `index.html` | Content |
|---|---|
| `<style>` | Design, colors as CSS variables at the top |
| HTML | Top bar, three tabs (Training, Habits, Stats) and the settings. Static text carries `data-t` so it can be translated |
| Script `i18n` | German catalog: English text → German text |
| Script `engine` | `HMTrk`, the calculation core of recordings: segments from the plan text (English and German), filters, distance, intervals, splits, route smoothing, privacy zone, line simplification and encoding. Pure functions without access to device or storage |
| Script `app`, top | Language (`tr`, `bi`, `applyStatic`), dates, plan (`PLAN`, `PHASES`), profile (`applyProfile` gives `START`, `RACE`, `SESSIONS`) |
| Script `app`, middle | State (`state`, `normalize`, `persist`), rendering (`renderHero`, `renderWeek`, `renderPlan`, `renderHabits`, `renderStats`, `renderOverview`, `renderLearnings`, `renderSettings`) |
| Script `app`, GPS part | Recording (`trkBegin`, `trkTick`, `trkEnd`, `trkRestore`), run screen `#runUI`, map `mapHTML` with pan and zoom |
| Script `app`, bottom | Events, backup, quick logging, settings, `setLang` |

All entries live in one data set:

```
state = {
  logs:       { "w3-wed": { km, sec, feel, knee, shoe, tod, ts, trk? } },
  strength:   { "w3-mon": true },
  alt:        { "w3-fri": { what, ts } },
  habits:     [ { id, name, type: "check" | "number", unit, created } ],
  habitLog:   { <habitId>: { "2026-10-04": "x" | "o" | 7.5 } },
  learnings:  [ { id, date, ts, text, type: "insight" | "issue", tag, solved } ],
  profile:    { raceDate: "2027-04-04", goalSec: 7200, problem: "Knee",
                mon: "Kraft", tue: "Bouldern", fri: "Bouldern", lang: "en" | "de" },   // "Aus" = off
  records:    [ { id, sport: "lauf"|"boulder"|"stretch"|"cali", key, name, value, date, former, ts } ],
  goals:      [ { id, sport: "allg"|"lauf"|..., text, due, done, ts } ],
  workouts:   [ { id, sport: "boulder"|"kraft"|"cali"|"stretch"|"other", date, min, note, ts } ],
  dur:        { "w3-tue": 75 },   // optional duration in minutes for plan sessions
  edits:      { "w3-wed": { t, d, min, off, label, skip } },   // your own changes to the plan, only changed fields
  gps:        { zone: 0|150|300|500|1000, save: true|false },   // privacy zone in meters, save route
  updatedAt:  <timestamp>
}

// Optional field of a run that was recorded with GPS:
logs["w3-wed"].trk = {
  iv:    [ ["work"|"rest"|"easy"|"lap", "fast", seconds, meters], ... ],   // segments (label is a key, older runs: German word)
  sp:    [ secondsPerKm, ... ], tail: [meters, seconds] | null,            // kilometer splits
  route: [ "<encoded line>", ... ] | null                                  // smoothed polyline (precision 1e-5), without privacy zone
}
```

The keys of `logs`, `strength`, `alt`, `dur` and `edits` are session IDs: `w` plus the week number, then `-mon`, `-tue`, `-wed`, `-fri` or `-sun`.

Values such as `"Kraft"`, `"Bouldern"`, `"Ruhetag"`, `"gut"` or `"barfuss"` are stored tokens from earlier German-only versions. They stay as they are so old data and backups keep working; the app only translates how they are shown.

`edits` sits on top of the plan and does not change it: `t` title, `d` description, `min` planned minutes (runs only), `off` weekday as offset from Monday (0 to 6), `label` another type (extra sessions only), `skip` removed from the plan. Only changed fields are stored, so for example the goal pace in descriptions keeps following.

### 4. Test before you publish

1. Open the changed file in a browser: on a PC by double-clicking, on a phone via the Files app in Chrome.
2. Click through all three tabs and the settings, in both languages. Log a run, a habit and a learning. The browser stores data separately from the app, so your real data is not affected.
3. If a data field changed: save a backup in the app first and check after the update that everything is still there.

### 5. Publish

Upload the new `index.html` to the repository (“Add file” → “Upload files”; the same file name replaces the old one) and then create a new release, see below.

> **Important:** Never upload a file that came from “Save backup”. It contains your personal entries and would make them public. Before uploading, check that the line `<script id="saved-data" type="application/json">null</script>` ends with `null`.

### With coding agents (Claude Code, Codex, Cursor and similar)

1. Clone the repository and start the agent in the project folder.
2. Put the rules from the template above into a `CLAUDE.md` or `AGENTS.md` file so the agent knows them for every task.
3. Describe the change. Test locally, for example with `python3 -m http.server` and `http://localhost:8000` in the browser.
4. Commit on a branch, open a pull request, wait for the build, then create a release.

## Publishing a new version

1. Change `index.html` and commit.
2. Under “Releases”, create a new release with a new tag such as `v1.7.0`.
3. GitHub Actions builds the APK, signs it and attaches it to the release after about 2 minutes.

The build can also be started manually in the “Actions” tab. The APK is then available there as a download.

## Technology

| Part | Implementation |
|---|---|
| App | One file, `index.html`, with HTML, CSS and JavaScript, no frameworks |
| Languages | English source text, German catalog; more languages can be added as catalogs |
| Font | Instrument Sans (SIL Open Font License 1.1), local in `fonts/`, no calls to Google |
| Storage | `localStorage` in the WebView, one JSON data set |
| Android shell | [Capacitor](https://capacitorjs.com) 6, plugins for files and sharing, own plugin `run-tracker` for GPS |
| Build | GitHub Actions (`.github/workflows/build.yml`), Node 20, Java 17 |
| Privacy in the build | The build removes the Google services plugin, turns off Android's automatic cloud backup (`allowBackup`) and checks that all required permissions are in the merged manifest |
| Version number | `versionCode` is the running build number |
| License | GPL-3.0-or-later, see `LICENSE` |

The build creates the Android project from scratch on every run. So the repository only contains:

```
index.html                    the app
plugins/run-tracker/          GPS plugin (Java), included in the build
fonts/                        font with license text
.github/workflows/build.yml   build, signing, upload to the release
.github/workflows/wiki.yml    publishes docs/wiki to the GitHub wiki
docs/development.md           this file
docs/wiki/                    the wiki pages
LICENSE                       GNU General Public License v3
README.md                     overview for users
```

## GPS recording

Flow: the app starts the foreground service, which reads the device's GPS and collects the fixes in memory. The UI fetches them once per second and calculates time, distance and segment. While the screen is off the service keeps collecting and the UI catches up.

- **No Google services.** The `plugins/run-tracker` plugin only uses Android's `LocationManager` with the GPS provider. The ready-made Capacitor location plugins depend on Google Play Services and are therefore not an option.
- **Distance** comes from the Doppler speed of the GPS chip, or, if missing, from the distance between points. Fixes with more than 35 m inaccuracy and jumps above 9 m/s are discarded. Standing at a traffic light does not count as distance.
- **Segments** are read from the session description (for example “5× 2 min fast with 1:30 min jog”, or the German “5× 2 Min. zügig mit 1:30 Min. traben”). Both languages give the same segments. If the text matches no pattern, the run is free, and laps can be set by hand. If no recovery is named for repetitions, the app assumes 2 minutes of jogging. Segments are time-based; the distance per segment is the distance covered in that time. Only segments that were reached appear in the result. Very short segments (strides, 20 seconds) cannot be measured reliably with GPS and are not evaluated.
- **Route:** each part of the track is smoothed with a Kalman filter and a Rauch-Tung-Striebel smoother, weighting every fix by its reported accuracy. Then the privacy zone is cut out, the line is simplified (2 m tolerance) and encoded. On a noisy test track this halves the average error (5.0 m → 2.5 m) and stores about a quarter of the points. The map draws the line as curves.
- **Map:** drawn as SVG in meters around the route's center. Dragging moves the view, pinch, wheel, double tap and the + and − buttons zoom, one button fits the whole route, another opens full screen. The line keeps its width while zooming, and a scale bar follows. No map tiles, no network.
- **Privacy zone:** the center is the first fix of the run, and the radius varies randomly by 15 percent per run. All points inside it are removed before saving, also at the end of the run. The center is never stored. The edge of the gap reveals the scale, not the exact place.
- **Recording and app restart:** if Android ends the UI in the background, the service keeps running, and the app reattaches when opened and shows the run right away. The service also keeps pauses and laps in memory (`saveState`, `getStatus`), so reattaching works even without the UI's storage. If the service is running, the app always reattaches, even to a session that is no longer in the plan. A remembered run is only forgotten when the service is certainly no longer running. An error while asking is retried and deletes nothing. If the whole process is ended, the fixes are gone. This is intentional: nothing unfinished ever lies on storage. The app then says clearly that the recording was ended instead of silently starting over.
- **Permissions:** precise location, foreground service (type location), notifications (the service notification), vibration and wake lock. Location and notifications are only requested at the first start of a recording. `WAKE_LOCK` keeps the CPU awake during intervals. If it is missing from the manifest, Android throws a `SecurityException` and ends the app. The wake lock is therefore guarded (a failure there does not stop the recording), and the build checks the merged manifest for all required permissions.
- **Notification language:** the app passes its language to the service on start, so the notification is in English or German.
- **Diagnostic log:** if a recording fails, the log under Settings, Diagnostics helps. It is anonymous and only on the device (`files/diagnose.log`, at most about 200 lines). It contains only event words and counters: service created or ended, wake lock ok or failed, time to the first GPS fix, memory warnings, the Android version and why Android last ended the process. It never contains places, distances, dates, times of day, names or device identifiers. Time is only seconds since the process started. `RunLog` only allows a small set of characters and replaces anything that looks like a coordinate. On a crash, the exception class and code location are noted, not its message. The log can be viewed, shared, deleted or switched off. Add new events with `RunLog.add("…")` in the plugin or `hmlog("…")` in `index.html`, using only fixed words and small numbers.
- **Test:** start the build manually on a branch (Actions, “Build APK”, “Run workflow”). The APK is then available as an artifact. In the browser, recording works with the browser's location, but only while the screen is on.

## APK signing

The build signs the APK with a fixed key. It is not in the code but stored as repository secrets on GitHub (`KEYSTORE_BASE64` and `KEYSTORE_PASSWORD`, under Settings, Secrets and variables, Actions).

- All updates must be signed with the same key. An installed app cannot be updated with a different key. It would have to be uninstalled first, which deletes the entries, so save a backup first.
- Keep the key and password somewhere outside GitHub as well. If they are lost, existing installations get no more updates.
- A key or password must never be in the repository, not even in older versions of files.
- Only install APKs from your own, known repository.
