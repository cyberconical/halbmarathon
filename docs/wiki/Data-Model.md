# Data model

All entries live in **one** JSON data set in the app's `localStorage`. This page describes it completely, together with the rules that keep it readable across all versions.

## Storage keys

| Key | Content | Lifetime |
|---|---|---|
| `hm2027-state` | the data set (below) | permanent |
| `hm2027-state-tab` | last opened tab | permanent, convenience only |
| `hm2027-trk` | running recording: `{id, startedAt, pauses, laps, native}` | until the recording ends |
| `hm2027-trk-pend` | applied GPS result before saving: `{id, km, sec, trk}` | until the run is saved or discarded |

In the Android app there is also the diagnostic log as the file `files/diagnose.log` and a small switch in `SharedPreferences` (`halbmarathon_diag`). See [[Android plugin|Android-Plugin]].

## The data set

```js
state = {
  logs:      { "w3-wed": { km, sec, feel, knee, shoe, tod, ts, trk? } },
  strength:  { "w3-mon": true },
  alt:       { "w3-fri": { what, ts } },
  habits:    [ { id, name, type: "check" | "number", unit, created } ],
  habitLog:  { <habitId>: { "2026-10-04": "x" | "o" | 7.5 } },
  learnings: [ { id, date, ts, text, type: "insight" | "issue", tag, solved } ],
  profile:   { raceDate: "2027-04-04", goalSec: 7200, problem: "Knee",
               mon: "Kraft", tue: "Bouldern", fri: "Bouldern",          // "Aus" = off
               lang: "en" | "de" },
  records:   [ { id, sport: "lauf"|"boulder"|"stretch"|"cali", key, name, value, date, former, ts } ],
  goals:     [ { id, sport: "allg"|"lauf"|"boulder"|"stretch"|"cali", text, due, done, ts } ],
  workouts:  [ { id, sport: "boulder"|"kraft"|"cali"|"stretch"|"yoga"|"workout"|"swim"|"cycle"|"other", date, min, note, ts } ],
  dur:       { "w3-tue": 75 },
  edits:     { "w3-wed": { t, d, min, off, label, skip } },
  gps:       { zone: 0|150|300|500|1000, save: true|false },
  updatedAt: <timestamp ms>
}
```

## Stored tokens

The app started German-only. Several values are therefore stored as short German words. They are **tokens**: they stay exactly as they are, and the app translates them only for display. That way old data and every older backup keep working in both languages.

| Field | Tokens | Shown in English as |
|---|---|---|
| `profile.mon/tue/fri`, `edits.label`, quick logging | `Kraft`, `Bouldern`, `Calisthenics`, `Stretching`, `Yoga`, `Schwimmen`, `Rad`, `Workout`, `Aus` (`edits.label` also `Lauf`) | Strength, Bouldering, Calisthenics, Stretching, Yoga, Swimming, Cycling, Workout, Off (Run) |
| `alt.what` | `Sauna`, `Ruhetag`, `Schwimmen`, `Rad`, `Yoga`, `Krank` (or free text) | Sauna, Rest day, Swimming, Cycling, Yoga, Sick |
| `logs.knee` | `gut`, `zieht`, `schmerzt` | all good, slight pull, hurts |
| `logs.shoe` | `barfuss`, `normal` | barefoot shoes, regular shoes |
| `logs.tod` | `morgen`, `mittag`, `abend`, `nacht` | morning, midday, evening, night |
| `learnings.tag` | `Tempo`, `Ausrüstung`, `Ernährung`, `Erholung`, `Kopf`, `Sonstiges` (or the watched body part) | Pace, Gear, Nutrition, Recovery, Mind, Other |
| sport keys | `lauf`, `boulder`, `kraft`, `cali`, `stretch`, `yoga`, `workout`, `swim`, `cycle`, `other`, `allg` | Running, Bouldering, Strength, … |

Free text the person typed (a custom activity, a note, a habit name) is stored and shown as typed.

## Fields in detail

### Session IDs

The keys of `logs`, `strength`, `alt`, `dur` and `edits` are session IDs: `w` plus the week number (1 to 27), then `-mon`, `-tue`, `-wed`, `-fri` or `-sun`. They don't depend on the calendar. If race day changes, all entries stay with their week.

### `logs`: runs

| Field | Type | Meaning |
|---|---|---|
| `km` | number | kilometers, more than 0 and less than 60 |
| `sec` | number | time in seconds, more than 60 and less than 6 hours |
| `feel` | 1 to 5 or `null` | Tough, Okay, Good, Strong, Top |
| `knee` | token or `null` | the watched body part (the name is historical) |
| `shoe` | token or `null` | shoes |
| `tod` | token or `null` | time of day |
| `ts` | timestamp | when logged |
| `trk` | object, optional | result of a GPS recording, see below |

### `logs[id].trk`: GPS result

```js
trk = {
  iv:    [ ["work"|"rest"|"easy"|"lap", "fast", seconds, meters], ... ],   // always [] in new recordings; old ones keep theirs, but it isn't shown
  sp:    [ secondsPerKm, ... ],                                            // full kilometers
  tail:  [meters, seconds] | null,                                         // last, partial kilometer
  route: [ "<encoded line>", ... ] | null                                  // smoothed, without privacy zone
}
```

The segment label (second value) is a language-neutral key: `run`, `walk`, `fast`, `jog`, `hmp`, `tempo`, `easy`, `warmup`, `cooldown`, `after` or `lap:N`. Runs recorded with older versions contain German words here (`Zügig`, `Traben`, `Runde 2`, …); the app recognizes and translates both. Older versions also stored segments that were never reached (with 0:00); new recordings only store reached ones.

The route is a list of line parts in polyline format with precision 1e-5 (about 1 m). Several parts come from pauses, GPS gaps and the privacy zone. Since this version the line is smoothed before it is stored. Raw fixes are never stored. Details: [[Analysis engine|Analysis-Engine]].

### `strength`, `alt`, `dur`

- `strength[id] = true`: an extra session of the plan is done.
- `alt[id] = {what, ts}`: something else happened instead of the session, for example `"Sauna"`, `"Krank"` or another sport from quick logging.
- `dur[id]`: optional duration in minutes for a completed extra session.

### `habits` and `habitLog`

- `type: "check"`: the log holds `"x"` (done) or `"o"` (not done) per day.
- `type: "number"`: the log holds a number per day; `unit` is the unit.
- Days are keys in the format `YYYY-MM-DD` in local time.

### `edits`: own changes to the plan

Changes lie **on top of** the plan and don't change it. Only changed fields are stored, so for example the goal pace in descriptions keeps following.

| Field | Meaning | Limits |
|---|---|---|
| `t` | own title | up to 40 characters |
| `d` | own description | up to 400 characters |
| `min` | planned minutes | runs only, not the race, 1 to 300 |
| `off` | weekday as offset from Monday | 0 to 6 |
| `label` | another type | any session except the race, a token from `SPORTCHOICES` (`DAYCHOICES` plus `Lauf`). A run slot with another label stops being a run, a non-run slot with `Lauf` becomes one. `logs[id]` is never deleted by this |
| `skip` | removed from the plan | extra sessions only |

A title or description is only stored when it differs from the plan's text in the current language. Typed text stays in the language it was typed in.

### `records` and `goals`

- A record with `former: true` is listed under “Earlier”. A new current record of the same discipline in the same sport sets the old one to `former` automatically. Records with the same `key` count as the same discipline, whatever their name.
- `key` is set for running templates (`longest`, `5k`, `10k`, `hm`, `m`). For records with a key, the app shows the template name in the current language. For `longest` it also adds new bests itself.
- Goals have an optional due date `due` and `done`.

### `workouts`: free sessions

Entries outside the plan from the weekly overview or quick logging. `date` in the format `YYYY-MM-DD`, `min` 0 to 600, `note` up to 40 characters.

### `gps`

`zone` is the radius of the privacy zone in meters (default 300), `save` whether routes are stored (default yes), `test` whether test mode for inaccurate GPS is on (default no), `voice` the distance between voice announcements in meters: `0` (off, default), `500` or `1000`. Other values become `0`.

### `profile.lang`

`"en"` (default) or `"de"`. Data without this field, from versions before the language setting, opens in English.

## `normalize()`: the only door

Every data set passes through `normalize()` on loading, whether from `localStorage`, from a backup file or empty. The function

- sets a default for every missing field,
- checks values against allowed lists (`DAYCHOICES`, privacy zones, languages),
- recognizes data from before the settings existed (`logs` present, `profile` missing) and adds fitting values: race on April 4, 2027, watched body part knee, bouldering on Tuesday and Friday, two earlier records.

## Rules for changes

1. The key `hm2027-state` and all existing fields are **never renamed or removed**.
2. New fields are **optional** and get a default in `normalize()`.
3. Stored tokens are **not translated in storage**. New tokens may be English; they then need an entry in the display table.
4. New values in lists must cope with old data. An unknown old value must not break anything.
5. Before changing the data set, save a backup in the app and check after the update that everything is there.

## Where data comes from at start

The app reads `localStorage` first, then the embedded `saved-data` block. If the latter's `updatedAt` is newer, it wins. That way a backup file opened in a browser shows its data right away. The result is then written back to `localStorage`. See [[Backup and data|Backup-and-Data]].
