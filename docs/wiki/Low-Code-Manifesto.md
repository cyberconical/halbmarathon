# Low-code manifesto

*How this app is built and why. For anyone who wants to change it, with or without programming skills, with or without AI.*

## What this is about

“Low code” here doesn't mean clicking boxes together in a visual editor. It means: **so few moving parts that one person (or an AI) can see the whole app at once and change it.** The app is one file you can read, plus rules that make sure changes don't break anything.

The goal: anyone with an idea for the app should be able to describe it in one sentence, get back a changed file and have it on their phone the same evening, without losing data or leaking anything private.

## The principles

### 1. One file, one thought

The app is `index.html`: HTML, CSS and JavaScript in one file, about 170 KB. You upload it to an AI chat and it fits completely. Whoever wants to change it doesn't have to hunt for pieces, learn a folder structure or understand dependencies.

*In the project:* `index.html` with the four script blocks `saved-data`, `i18n`, `engine` and `app`, plus the font in `fonts/`. See [[Architecture|Architecture]].

### 2. No frameworks, no build step for the UI

No React, no bundler, no package manager for the app code. What is in the file runs. The only build is the one that packages the file into an Android app, and it does not change the app code.

*In the project:* the build copies `index.html` and `fonts/` unchanged into the Capacitor shell. See [[Build and release|Build-and-Release]].

### 3. Your data belongs to you

The app works offline and stores only on the device. No server, no account, no Google services, no trackers, no map tiles. Less technology means less attack surface and less to explain.

*In the project:* `localStorage`, one data set, Android cloud backup turned off, Google services removed in the build, the route drawn from your own points. See [[Privacy|Privacy]].

### 4. The app is its own backup

The backup is not an export file in a special format but **the app itself with the data inside**. The function `buildFile()` takes a copy of its own page and writes the data set into the empty `saved-data` block. That file can be opened in a browser, loaded as a backup or installed as a new version.

That is practical and an obligation: the `saved-data` block always stays `null` in the original, and a backup file never belongs in a public repository.

### 5. The plan is data, not code

What changes often is written as a readable list: `PHASES` and `PLAN` with titles, minutes and descriptions, in both languages next to each other (`bi("English", "Deutsch")`). Whoever wants to change the plan changes text, not logic. The person's own changes lie **separately on top of the plan** (`state.edits`), so the plan stays the suggestion and “Back to plan” always works.

Even the intervals for GPS live only in the description text. The engine reads them from there, in English or German. Change the text and you change the training.

### 6. Calculation without side effects

Everything that calculates lives in the `engine` block: reading segments, determining distance from GPS fixes, evaluating intervals, smoothing, simplifying and encoding the route. These are pure functions without access to device, storage or UI. They can be understood, tested and replaced on their own. See [[Analysis engine|Analysis-Engine]].

### 7. Compatibility beats elegance

Stored data is sacred. The storage key `hm2027-state` and the existing fields are **never renamed or removed**. New fields are optional and have defaults. The function `normalize()` reads any old or half-broken data set and turns it into a valid one. See [[Data model|Data-Model]].

When the app became English, the stored values stayed German tokens (“Kraft”, “Ruhetag”, “gut”). Translating them in storage would have broken every older backup; translating them on display costs one small lookup table.

### 8. One language in the code, any language for people

The code is written in English, the source text is English, and translations are a catalog keyed by that English text. Adding a language means adding a catalog, not touching logic. A missing translation falls back to English instead of breaking. See [[Translations|Translations]].

### 9. Native only where the web is not enough

A web page can't read GPS in the background. For that there is **one** small Java plugin, `run-tracker`, which only uses Android's `LocationManager`. Everything else stays a web page. The less native code, the less a browser test can't see. See [[Android plugin|Android-Plugin]].

### 10. Frugal with data, also when things go wrong

Raw GPS fixes stay in memory. Only the result is stored, with the route if wanted, smoothed and without the area around the start. Troubleshooting follows the same rule: the diagnostic log contains only technical events, no places, no dates, no names, and the person sees, deletes and shares it themselves.

### 11. Trust through checks

Whoever can't read everything themselves needs guards that do it for them. The build stops if Google services appear, if cloud backup is not off, or if a required permission is missing from the final manifest. A rule that only lives in a document gets forgotten. A rule that turns the build red doesn't.

### 12. One change per request

Small steps, test each, back up first. Whoever gives an AI ten wishes at once gets ten possible errors at once. The template is under [[Customizing with AI|Customizing-with-AI]].

## The contract with whoever changes the app

Every change follows these rules, whether by hand or by an AI:

1. **One file stays one file.** No frameworks, no external libraries, no network calls.
2. **The storage key and all existing data fields stay.** New fields are optional. Stored tokens are not translated.
3. **The script IDs `saved-data`, `i18n`, `engine` and `app` and the backup function stay.** `saved-data` is `null` in the original.
4. **Calculation belongs in `engine`, display and storage in `app`.**
5. **All user-facing text is English in `tr("…")`, with its German translation in the catalog.** Plan texts use `bi()`.
6. **No places, dates or names in logs.**
7. **Design: black and white, minimal, existing CSS variables.** Usable with a thumb.
8. **Changes to permissions or the Android part are checked by the build.** A browser test doesn't see them.

## What this approach costs

Honestly, there are costs, and it is worth knowing them.

- **No type checking.** A typo in a field name only shows when it is used. Small steps, `normalize()` and browser tests help.
- **A big file.** About 2,500 lines in one file read well with structure, but are hard for two people to edit at the same time. Merge conflicts are more likely than with many small files.
- **Patterns instead of structure.** Interval recognition reads free text with regular expressions. That is flexible, but an unusual sentence in the plan isn't recognized and becomes a free run. That is a deliberate soft fallback, but a limit.
- **Two languages to keep in sync.** Every new text needs a German translation. A small script can check that every key has one (see [[Translations|Translations]]); without it, missing translations quietly show English.
- **Few automated tests.** The calculation functions are built to be testable, and tests were run for every change, but a full test suite is not yet in the repository. See [[Contributing and testing|Contributing-and-Testing]].
- **The native part is the weak spot.** Whatever only happens in the Android shell, a browser test doesn't see.

## Two cases from practice

**The crash at start.** GPS recording crashed at start, but only for runs with intervals. Cause: for punctual vibration cues the service keeps the CPU awake with a wake lock. The `WAKE_LOCK` permission was not in the manifest, and the call was not guarded. Android threw a `SecurityException`, the app was ended, and on opening the run was gone. What followed shows the principles at work:

- **Principle 11:** the build now checks the merged manifest for all required permissions. The same mistake can't ship unnoticed again.
- **Principle 9:** a failure at the wake lock no longer stops the recording. The cues may then be late; the run is not lost.
- **Principle 10:** an anonymous diagnostic log now shows what happened, including why Android last ended the app, without places or names.
- **Principle 7:** after a restart the app reattaches to the running run instead of silently dropping it.

**English with German as an option.** The app started German-only. Making it English without breaking anything followed the same rules: English became the source text, German a catalog (principle 8); stored German tokens stayed and are only translated on display (principle 7); the GPS engine learned English plan text and returns language-neutral segment keys, and a test confirmed that the English and German plans produce identical intervals (principles 5 and 6); a script checks that every one of the 524 texts has a German translation (principle 11).

## When the approach tips over

A single file is not a religion. It is time to split it when

- several people work on different areas at the same time and constantly get in each other's way,
- the file gets so big that it no longer fits into an AI chat,
- the calculation logic grows so much that it can't be changed safely without automated tests,
- more native features arrive that need their own testing and checking concept,
- more than two or three languages make the catalog worth its own file.

The sensible first step would then be to move the `engine` block into its own tested file. It is already built so this works without restructuring. The `i18n` catalog could follow as JSON files per language. Until then: **simple enough to understand everything at once.**

## Further

- [[Architecture|Architecture]]: how the parts fit together
- [[Customizing with AI|Customizing-with-AI]]: the template for change requests
- [[Contributing and testing|Contributing-and-Testing]]: how to check changes
