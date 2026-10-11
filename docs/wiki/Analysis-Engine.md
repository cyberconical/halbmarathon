# Analysis engine

The `<script id="engine">` block in `index.html` provides the object `window.HMTrk`. It contains only **pure functions**: no access to device, storage or UI, and no language. It runs the same in the browser and in Node (`globalThis`), so it can be tested on its own. Why it is built like this: [[Low-code manifesto|Low-Code-Manifesto]], principle 6.

## The functions

| Function | Input | Output |
|---|---|---|
| `parseSegments(desc, planMin)` | session description (English or German), planned minutes | segments `[{k, sec, label}]`, empty = free run |
| `cuesFor(segs, label, endText)` | segments, label function, final text | cues `[{at, v, n}]` for the service |
| `current(segs, act)` | segments, running time in s | current segment, time left, next |
| `analyze(fixes, startMs, pauses, nowMs)` | raw fixes, start, pauses | cleaned points, distance, running time |
| `distAt(pts, a)` | points, running time | distance at that moment (interpolated) |
| `intervals(pts, segs, active)` | points, segments, running time | time and meters of each **reached** segment, plus “after” |
| `lapSegs(laps, active)` | own lap marks | segments from laps |
| `splits(pts, total)` | points, running time | time per kilometer, plus the partial rest |
| `livePace(pts, active)` | points, running time | pace of roughly the last 20 seconds |
| `smoothTrack(pts, q)` | points with time and accuracy | smoothed line (Kalman + RTS) |
| `simplify(pts, tol)` | line, tolerance in m | simplified line (Douglas-Peucker) |
| `encode(pts)` / `decode(s)` | line / text | polyline encoding, precision 1e-5 |
| `buildRoute(pts, zoneM, rand)` | points, privacy zone | encoded, smoothed line parts without the privacy zone |
| `hav(a, b)` | two points | distance in meters (haversine) |

## Reading segments from text

`parseSegments` understands English and German. Labels are **language-neutral keys**; the app translates them for display.

| Words in the text | Key |
|---|---|
| run, running, laufen | `run` |
| walk, walking, gehen | `walk` |
| fast, brisk, zügig | `fast` |
| jog, jogging, traben | `jog` |
| half-marathon (pace), HMP, HM pace, HM-Tempo, Halbmarathon-Tempo | `hmp` |
| tempo, fartlek, tempo changes, Tempowechsel | `tempo` |
| easy, locker | `easy` |
| warm-up / ein, einlaufen | `warmup` |
| cool-down / aus, auslaufen | `cooldown` |

Patterns, in this order:

1. **Repetition in parentheses**: `5× (3 min easy run, 1 min walk)` → alternating `work` and `rest`, each labeled from its last word.
2. **Repetition with recovery**: `5× 2 min fast with 1:30 min jog` (German: `mit`). Without a named recovery (`3× 5 min half-marathon pace`) 2 minutes of jogging are assumed.
3. **Last minutes**: `the last 20 min at half-marathon pace` (German: `die letzten 20 Min. im HM-Tempo`). The rest of the planned time becomes an easy segment before it.
4. **Block**: `20 min fartlek`, `15 min fast`, `20 min half-marathon pace`.

Plus `N min warm-up` at the start and `N min cool-down` at the end. Durations are minutes (`3`) or minutes and seconds (`1:30`); seconds as `90 s` are not recognized. Text in parentheses is ignored except in pattern 1, for example `(about 5:41 /km)`. If fewer than two segments result, the result is empty, which means a free run. At most 60 repetitions are created.

Segment kinds: `work` (fast), `rest` (recovery), `easy` (easy, warm-up and cool-down), `lap` (own lap).

A test confirms that the English and German versions of all 54 plan runs produce identical segments (18 of them structured).

## Cues

`cuesFor` creates one cue for every change, with the running time in seconds, the kind (`work`, `rest`, `end`) and a text for the notification, for example `Fast · 2:00`. The app passes a label function and the final text in the current language; the engine itself contains no language. The list goes to the service as JSON, which schedules it and shifts it on pauses.

## Distance from raw fixes

A raw fix is `[time ms, lat, lon, accuracy m, speed m/s]`. Missing values are `-1`. `analyze` filters and calculates:

- Fixes older than the start or going back in time are dropped.
- **Accuracy worse than 35 m** is dropped.
- Fixes during a **pause** don't count. After a pause a new line part starts.
- **Jumps faster than 9 m/s** count as outliers and are discarded, at most five in a row.
- **Distance prefers the Doppler speed** of the GPS chip: the mean of both speeds times the time gap, if both fixes have a speed and are at most 5 seconds apart. Speeds below 0.5 m/s count as standing. This is far more precise than the distance between noisy positions.
- Otherwise the **distance between points** counts, but only from 3 m of movement or 8 seconds. So standing at a traffic light doesn't count as distance.

Running time is the time since the start minus all pauses. Result: cleaned points `{a (running time), d (distance), lat, lon, brk, t, acc}`, total distance and active time.

## Intervals, splits, pace

- `intervals` measures the distance within each segment's time window. Segments are time-based. Only segments that were reached are returned; if more than 15 seconds remain after the plan, an “after” segment is added.
- `splits` returns the time of every full kilometer, interpolated exactly to the kilometer point. A rest from 100 m on is returned as a partial kilometer.
- `livePace` looks back about 20 seconds and needs at least 15 m. Otherwise the result is `null`, and the screen shows a dash.

## Smoothing the route

GPS positions scatter by a few meters around the true path. Drawn directly, that gives a zigzag line. `smoothTrack` removes most of it:

1. Positions are projected to meters around the first point.
2. A **Kalman filter** runs forward. Each fix counts according to its reported accuracy (variance = accuracy², at least 3 m), so imprecise fixes move the line less. Between fixes the uncertainty grows with the elapsed time (process noise 3 m/s).
3. A **Rauch-Tung-Striebel smoother** runs backward and corrects every point with what came after it. This removes the lag a forward-only filter would have, so corners stay where they are.
4. The result goes back to latitude and longitude.

On a simulated 400 × 300 m rectangle with realistic noise and occasional 25 m spikes, the stored line deviates from the true path by 2.5 m on average (before: 5.0 m) and at most 10.2 m (before: 23.7 m), with 43 instead of 163 points.

The map then draws the line as Catmull-Rom curves instead of straight segments. That also improves routes recorded before smoothing existed.

## Building and protecting the route

`buildRoute`:

1. Splits the points into parts at breaks (pauses, the first point).
2. Smooths every part with `smoothTrack`.
3. If a privacy zone is set, places a circle around the **first point**, radius × (0.85 to 1.15) at random.
4. Leaves out smoothed points inside the circle and splits the line there. This also applies at the end of the run. Because the check uses the smoothed points, every stored point is guaranteed to lie outside the circle.
5. Simplifies every part with 2 m tolerance and encodes it.
6. Circle center and radius are not returned and never stored.

For tests, the randomness can be fixed through the `rand` parameter.

## Testing in Node

Because the engine needs nothing but `globalThis`, it can be loaded directly:

```js
const fs = require("fs"), vm = require("vm");
const html = fs.readFileSync("index.html", "utf8");
const src = html.match(/<script id="engine">([\s\S]*?)<\/script>/)[1];
vm.runInThisContext(src);
const segs = HMTrk.parseSegments("10 min warm-up, 5× 2 min fast with 1:30 min jog, 10 min cool-down.", 40);
console.log(segs.map(s => s.k + " " + s.sec + " " + s.label).join(", "));
// easy 600 warmup, work 120 fast, rest 90 jog, work 120 fast, … work 120 fast, easy 600 cooldown
```

More under [[Contributing and testing|Contributing-and-Testing]].
