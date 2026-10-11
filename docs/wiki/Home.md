# Half Marathon

A lean Android app that takes you to a half marathon in 27 weeks: training plan, run log, GPS recording, habits and statistics in one place. It runs completely offline. All data stays on your device.

What makes it special is how it is built: **a single file** (`index.html`), no framework, no server, with a tiny Android shell. That is why it can be customized with AI assistants even without programming skills. The idea behind it is in the [[Low-code manifesto|Low-Code-Manifesto]].

## At a glance

| | |
|---|---|
| Plan | 27 weeks, two runs per week, five phases from comeback to race week |
| Platform | Android 5.1 or newer |
| Languages | English (default) and German, switchable under Settings |
| Data | only on the device: no account, no server, no Google services |
| GPS | optional, with interval cues by vibration, a privacy zone around the start, and a smoothed route on a movable, zoomable map (no map tiles) |
| Code | `index.html` (about 170 KB) plus a Java plugin for the GPS service |
| Design | black and white, follows the device's light or dark mode |
| License | GPL-3.0-or-later |

## Where to go

**Using the app**

- [[Installation|Installation]]: Obtainium, APK, updates
- [[Getting started|Getting-Started]]: the first five minutes
- [[Training and plan|Training-and-Plan]]: weekly rhythm, phases, adjusting sessions, logging runs
- [[GPS recording|GPS-Recording]]: distance, intervals, privacy zone, map
- [[Habits|Habits]]
- [[Stats|Stats]]: key figures, weekly overview per sport, learnings
- [[Settings|Settings]]
- [[Backup and data|Backup-and-Data]]
- [[Privacy|Privacy]]
- [[FAQ and glossary|FAQ-and-Glossary]]

**Understanding how it is built**

- [[Low-code manifesto|Low-Code-Manifesto]]: the principles behind the app
- [[Architecture|Architecture]]: building blocks, data flows, diagrams
- [[Data model|Data-Model]]: the data set, storage keys, backward compatibility
- [[Analysis engine|Analysis-Engine]]: reading segments, computing distance, smoothing and building the route
- [[Android plugin|Android-Plugin]]: foreground service, bridge, diagnostic log
- [[Translations|Translations]]: how English and German work, adding a language
- [[Build and release|Build-and-Release]]: GitHub Actions, signing, checks

**Changing it yourself**

- [[Customizing with AI|Customizing-with-AI]]: template, recipes, checklist
- [[Customizing the plan|Customizing-the-Plan]]: weeks, phases, descriptions
- [[Contributing and testing|Contributing-and-Testing]]
- [[Troubleshooting|Troubleshooting]]: when something doesn't work, including the diagnostic log
