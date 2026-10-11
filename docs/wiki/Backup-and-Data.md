# Backup and data

## Where your data lives

All entries live in the app's private storage, in the WebView's `localStorage`, as one single data set. There is no server and no account. Android does **not** back up the app data to the cloud; the build deliberately turns that off.

That also means: **uninstalling the app or clearing its data deletes your entries.** Updates through Obtainium keep them. A regular backup is your insurance.

## Saving a backup

At the top right it says “Save backup” when something was added since the last backup, otherwise “All saved”. Tapping it creates a file `halbmarathon.html` and opens Android's share menu. Choose Drive, Files or a messenger.

The special thing: the backup file is **the whole app with your entries inside**. You can open it in a browser and see your data, and it can be loaded back the same way. Why it is built like this is in the [[Low-code manifesto|Low-Code-Manifesto]] and under [[Architecture|Architecture]].

## Loading a backup

At the very bottom of the page is “Load backup”. Choose a backup file (`.html`, or a plain `.json` file). The app asks before replacing your current entries. If the file contains no entries, it says so. The language stored in the backup is applied as well.

Backups from older versions (German only) load without problems. The app shows their content in your current language.

## When backup and device disagree

On start the app compares the timestamp (`updatedAt`) of the data embedded in the file with that of the local data. The newer one wins. This happens for example when you open a backup file in a browser that is newer than the browser's storage.

## Important

> **Never upload a backup file to a public repository.** It contains your personal entries and would make them public. Before uploading the app to a repository, the line `<script id="saved-data" type="application/json">null</script>` must end with `null`. See [[Customizing with AI|Customizing-with-AI]].

## What is in the data set

Runs, extra sessions, replacement sessions, habits and their history, learnings, profile (including language), records, goals, free sessions, your own changes to the plan and the GPS settings. Recorded runs contain segments, kilometer splits and optionally the smoothed route without the privacy zone. Raw GPS fixes are not included. Exact schema: [[Data model|Data-Model]].
