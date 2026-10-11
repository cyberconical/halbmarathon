# FAQ and glossary

## Frequently asked questions

**Do I need an account or internet?**
No. The app runs completely offline and has no account.

**How do I switch to German?**
Settings, Language, Deutsch. Everything switches at once, including the plan. See [[Settings|Settings]].

**After the update the app is in English. Did I lose my data?**
No. English is now the default. Your entries are untouched; switch the language back under Settings, Language.

**My entries are gone. What happened?**
Either the app data was cleared or the app was uninstalled. Then only a backup helps. Updates through Obtainium keep entries. See [[Backup and data|Backup-and-Data]].

**Why does the app ask for precise location?**
For GPS recording. Android 12 and newer also require the approximate location. The app only uses it during a recording.

**Why is there no street map under my route?**
Map tiles would have to be downloaded from a server, which would reveal where you run. The app only draws your own line, to scale. You can move and zoom it like a map.

**Why does my route look slightly rounder than the streets?**
GPS jumps around by a few meters. The app smooths the line so it follows your real path instead of zigzagging. Sharp 90° corners become slightly rounded.

**I ran without GPS. Can I still log it?**
Yes, every run can be logged by hand with kilometers and time.

**The race is less than 27 weeks away.**
Then you join in the matching week. See [[Training and plan|Training-and-Plan]].

**I want to move or change a session.**
Open the session, “Adjust session”. “Back to plan” restores the suggestion.

**The recording stopped at the start, or the run was gone after opening the app.**
See [[Troubleshooting|Troubleshooting]]. The diagnostic log under Settings shows what happened, without revealing anything personal.

**Can I change the app without programming?**
Yes, that is what it is built for. See [[Customizing with AI|Customizing-with-AI]] and the [[Low-code manifesto|Low-Code-Manifesto]].

**Can I add another language?**
Yes. See [[Translations|Translations]].

**Is there an iPhone version?**
No. The app is an Android app. `index.html` runs in any browser, but GPS recording in the background only exists in the Android shell.

## Glossary

| Term | Meaning |
|---|---|
| **Session** | one entry in the plan: a run or an extra session on a day |
| **Extra session** | strength, bouldering, calisthenics, stretching, yoga, swimming or cycling on Monday, Tuesday or Friday |
| **Recovery week** | “Breathe”, every fourth week with less volume |
| **Pace** | time per kilometer, for example 5:30 min/km |
| **Goal pace** | goal time divided by 21.0975 km |
| **Segment** | part of an interval session: fast, recovery or warm-up |
| **Cue** | vibration when switching between segments |
| **Fix** | a single GPS measurement |
| **Privacy zone** | area around the start whose points are never stored |
| **Smoothing** | removing GPS noise from the route with a Kalman filter |
| **Foreground service** | Android service with a visible notification that keeps running with the screen off |
| **Token** | a stored value from earlier versions, such as “Kraft” for strength; shown in your language |
| **Catalog** | the list of translations, English text to German text |
| **Capacitor** | the shell that packages `index.html` as an Android app |
| **Plugin** | Java code for things a web page can't do, here GPS in the background |
| **Obtainium** | an app that installs and updates apps straight from GitHub releases |
| **Low code** | software with little, readable code that stays changeable without programming skills, see [[Low-code manifesto|Low-Code-Manifesto]] |
