# Contributing and testing

## Ground rules

Every change follows the contract from the [[Low-code manifesto|Low-Code-Manifesto]]: one file, no frameworks, no network calls, compatible data, calculation in `engine`, English text with German translation, no personal data in logs.

## Flow

1. Create a branch.
2. Change and test locally (below).
3. Open a pull request against `main`. Describe: problem, cause, change, how tested, what was deliberately not changed.
4. The build starts by itself for the pull request (check `build`) and also verifies the Android part. It must be green before merging.
5. Test the APK from that run (artifact `halbmarathon-apk`) on a device if the Android part or GPS recording is affected.
6. Merge and create a release, see [[Build and release|Build-and-Release]].

## Testing locally

### In the browser

```sh
python3 -m http.server
# then open http://localhost:8000
```

All tabs, the settings and one entry per area, in English and in German. The browser's developer console should show no errors. In the browser, GPS recording uses the browser's location, but only while the screen is on, and the diagnostic log doesn't exist. The map can be tested with the mouse: drag, wheel, double click, buttons.

### Checking syntax

```sh
python3 - <<'PY'
import re
h=open("index.html",encoding="utf8").read()
for i,(a,b) in enumerate(re.findall(r'<script([^>]*)>(.*?)</script>',h,flags=re.S)):
    if "json" not in a and b.strip(): open(f"/tmp/blk{i}.js","w").write(b)
PY
for f in /tmp/blk*.js; do node --check "$f" && echo "$f ok"; done
```

### Checking translations

The script under [[Translations|Translations]], “Checking completeness”, lists missing German texts and placeholder mismatches.

### Testing the engine

The functions in `HMTrk` are pure and can be loaded in Node, see [[Analysis engine|Analysis-Engine]], “Testing in Node”. Good test cases:

- send every `PLAN` description through `parseSegments` **in both languages** and check that they give identical segments (currently 18 of the 54 runs give segments). The sum of the segments doesn't have to match the planned minutes exactly: those are rounded and include some buffer; for some sessions the sum is 4 to 7 minutes less,
- `analyze` with an invented straight track at constant speed, with pauses, outliers and poor accuracy,
- `buildRoute` on a noisy track: the smoothed line should be closer to the true path than the raw fixes, and with a fixed `rand` no point may lie inside the privacy zone,
- `encode` and `decode` round trip.

### Testing the app with a simulated plugin

With [jsdom](https://github.com/jsdom/jsdom), `index.html` can be loaded in Node and a plugin simulated by setting `window.Capacitor = { isNativePlatform: () => true, Plugins: { RunTracker: mock } }` before loading. That way these were checked, among others:

- English by default, switching to German and back, language kept after a restart
- old German data opens in English and keeps its stored tokens
- reattaching after a restart: service running with and without a remembered run, service gone, errors while asking, unknown session, pause
- a full recording with simulated fixes: summary with map, only reached segments, pace from the precise distance
- the map: dragging, zoom buttons, pinch, wheel, full screen and Escape, scale bar, curved line

To find missing translations at runtime, replace `window.HM_I18N.de` with a `Proxy` before loading and record every key the app asks for that the catalog doesn't contain. `window.scrollTo` and `scrollIntoView` don't exist in jsdom; stub them.

### On a device

What only happens in the Android part is invisible to browser tests: permissions, foreground service, wake lock, behavior in the background, the notification language. Install the APK from the build and

1. start a free run and an interval run,
2. turn the screen off and listen for the cues,
3. swipe the app away and open it again: the run must be back immediately,
4. after finishing, move and zoom the map in the summary and open it full screen,
5. switch the language and start another recording: the notification follows,
6. look at the log under Settings, Diagnostics.

## Editing the wiki

The wiki pages live in `docs/wiki/` in the repository. Change them like any other file, through a pull request. After merging to `main`, the workflow `wiki.yml` publishes them to the GitHub wiki. Direct edits in the wiki are overwritten.

- File name = page name; `-` becomes a space.
- Internal links use double square brackets, first the display text, then after a vertical bar the page name, for example `[[Privacy|Privacy]]`.
- `_Sidebar.md` is the sidebar, `_Footer.md` the footer.
- Diagrams as Mermaid code blocks.

For the workflow to write to the wiki, the wiki must exist once: create the first page in the “Wiki” tab (any content).
