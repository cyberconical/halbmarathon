# Half Marathon

A lean Android app that takes you to a half marathon in 27 weeks: training plan, run log, habits and statistics in one place. It runs completely offline, and all data stays on your device.

The app is in English, with German as an option under Settings, Language. Black and white, it follows the light or dark mode of your device.

Detailed guides, the architecture and the low-code manifesto are in the [wiki](https://github.com/cyberconical/halbmarathon/wiki). Its pages live in `docs/wiki/` and are published from there.

## Features

**Training**
- A 27-week plan with two runs per week, in five phases from comeback to race week. It aligns itself to your race day.
- Log runs with kilometers, time, feel, shoes and time of day.
- Extra sessions such as strength, bouldering or yoga on three days of your choice.
- Log what you did today with one tap. Every session can be adjusted, and “Back to plan” restores the suggestion.
- Optional GPS recording: distance, time, pace, every kilometer and the route. It only tracks, and the screen may stay off.
- The route appears as a map in the run and, overlaid, in the stats. Drag to move it, pinch, double tap or use + and − to zoom, or open it full screen. GPS noise is smoothed so the line follows your path. The map is a drawn line; no map tiles are loaded.

**Habits**
- Create your own habits, check them off per day or enter a number.
- Streaks and weekly average at a glance.

**Stats**
- Weekly overview per sport, kilometers per week and pace trend.
- Comparison by shoe type and time of day.
- Record learnings and issues and mark them as solved.

**Settings**
- Language: English or German.
- Race day and goal time, which give the goal pace.
- A body part you want to keep an eye on, for example your knee.
- Records and personal goals per sport.
- GPS and route: privacy zone around the start and whether the route is saved.
- Diagnostics: an anonymous log for when a recording stops.

## Installation

The app is not in a store. The easiest way is [Obtainium](https://github.com/ImranR98/Obtainium), which detects new versions automatically:

1. Install Obtainium.
2. In Obtainium, tap “Add app” and paste `https://github.com/cyberconical/halbmarathon`.
3. Tap “Add” and then “Install”. Android asks once for permission to install apps from this source.

Alternatively, download `halbmarathon.apk` from the [latest release](https://github.com/cyberconical/halbmarathon/releases/latest) and install it by hand.

Requirement: Android 5.1 or newer.

## Getting started

1. Open the settings at the top right and enter your race day and goal time. Choose your language there too.
2. Log your first run in the **Training** tab.
3. Save a backup regularly (see below).

## Data and privacy

- All entries live in the app's private storage. There is no server and no account.
- The app calls no servers. It uses no Google services, no ads and no trackers. The font ships with the app.
- Location is used only during a recording and is never sent. The app reads the device's GPS directly, not Google's location services. Raw fixes stay in memory. Only the result is stored, with the route if you want.
- An adjustable **privacy zone** (300 m by default) around the start is never stored. Distance and time still count in full. It protects against the obvious but is no substitute for caution: someone who knows your route can guess the area.
- Under Settings there is a **diagnostic log** that helps find out why a recording stops. It only records technical events, never places, distances, dates, names or a device ID. You can view, share, delete or switch it off. It only leaves the device if you share it.
- Android does not back up the app data to the cloud. Your entries only leave the device if you share a backup yourself.
- Uninstalling the app or clearing its data deletes your entries. Updates through Obtainium keep them.

### Backup

- **Save:** Tap “Save backup” at the top and choose a place in the share menu, for example Drive or Files.
- **Restore:** Tap “Load backup” at the very bottom and choose the file.

The backup file contains all your entries and routes, unencrypted. Don't share it via public links and never upload it to a public repository.

## Development

The app is one file, `index.html`, without frameworks, plus the bundled font in `fonts`. It is packaged as an Android app with [Capacitor](https://capacitorjs.com), and GitHub Actions does the build.

To try it in the browser:

```
python3 -m http.server
```

Then open `http://localhost:8000`. The browser stores data separately from the app, so your real data stays untouched.

How to customize the app, publish a release and what the data model contains is in [docs/development.md](docs/development.md) and in the wiki.

```
index.html                    the app (English source text, German catalog in the script "i18n")
plugins/run-tracker/          GPS recording for Android (no Google services)
fonts/                        Instrument Sans font with license text
.github/workflows/build.yml   build, signing and upload to the release
.github/workflows/wiki.yml    publishes docs/wiki to the GitHub wiki
docs/development.md           customizing, data model, publishing
docs/wiki/                    the wiki pages
LICENSE                       GNU General Public License v3
README.md                     this file
```

## License

This project is licensed under the [GNU General Public License, version 3](LICENSE) or, at your option, any later version (GPL-3.0-or-later). You may use, change and share the app. Changes you share must stay open under the same license.

The [Instrument Sans](https://github.com/Instrument/instrument-sans) font is licensed under the SIL Open Font License 1.1; the license text is in `fonts/OFL.txt`.
