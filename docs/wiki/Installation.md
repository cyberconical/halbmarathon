# Installation

The app is not in a store. It is distributed as a signed APK file through GitHub.

## With Obtainium (recommended)

[Obtainium](https://github.com/ImranR98/Obtainium) detects new versions automatically and updates the app.

1. Install Obtainium.
2. In Obtainium, tap “Add app” and paste `https://github.com/cyberconical/halbmarathon`.
3. Tap “Add” and then “Install”. Android asks once for permission to install apps from this source.

## By hand

1. Download `halbmarathon.apk` from the [latest release](https://github.com/cyberconical/halbmarathon/releases/latest).
2. Open the file and allow the installation.

Requirement: **Android 5.1 or newer**.

On the home screen the app is called “Half Marathon”, on phones set to German “Halbmarathon”. The language inside the app is chosen separately under Settings, see [[Settings|Settings]].

## Updates and your data

- Updates through Obtainium or by installing over the existing app **keep all entries**.
- Every version is signed with the same key. Only then can an installed app be updated. See [[Build and release|Build-and-Release]].
- **Uninstalling or “Clear data” removes all entries.** Save a backup first, see [[Backup and data|Backup-and-Data]].
- Only install APK files from this repository.

## Without installing, in the browser

`index.html` can also be opened directly in a browser, on a PC by double-clicking. The browser stores data separately from the app, so your real data is not affected. That is handy for trying out changes, see [[Contributing and testing|Contributing-and-Testing]]. The browser has no background service: GPS recording uses the browser's location, but only while the screen is on.
