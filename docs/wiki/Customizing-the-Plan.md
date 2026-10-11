# Customizing the plan

There are two ways: **in the app**, just for you, or **in the code**, for everyone who installs the app.

## In the app

- **Race day and goal time** in Settings move the calendar and the goal pace.
- **Extra sessions** for Monday, Tuesday and Friday in Settings.
- **Single sessions** via “Adjust session”: day, title, description, minutes, type, remove. “Back to plan” restores the suggestion.

If you change the description of a run, the intervals of the GPS recording change too, because the app reads them from the text. See [[GPS recording|GPS-Recording]].

## In the code

The plan's structure is at the top of the `app` script in `index.html`. Every text exists in English and German, written next to each other with `bi("English", "Deutsch")`.

### `PHASES`

```js
{ name:bi("Build","Aufbau"), weeks:[11,18],
  text:bi("Intervals join in, the long run heads towards 90 minutes.",
          "Intervalle kommen dazu, der lange Lauf geht Richtung 90 Minuten.") }
```

Name, first and last week, description. The phases must cover all 27 weeks without gaps.

### `PLAN`

One entry per week, exactly 27 entries:

```js
[bi("First intervals","Erste Intervalle"),
  R(INTERVALS, 40, bi("10 min warm-up, 5× 2 min fast with 1:30 min jog, 10 min cool-down.",
                      "10 Min. ein, 5× 2 Min. zügig mit 1:30 Min. traben, 10 Min. aus.")),
  R(LONG, 60, bi("One hour. The first big one.", "Eine Stunde. Das erste große Ding."))]
```

- First value: the week's title.
- Second value: the **Wednesday** run, third: the **Sunday** run.
- `R(title, minutes, description)`. Common titles are constants: `RW` (run & walk), `EASY`, `STEADY`, `LONG`, `STRIDES`, `FARTLEK`, `INTERVALS`, `HMP`, `BREATHE`. Minutes `0` mean no time target (only the race).
- `{HMP}` in a description is replaced with the goal pace, for example `at half-marathon pace (about {HMP} /km)`.
- The Sunday of the last week is the race.

### Descriptions the GPS engine understands

So that intervals are recognized, write them in one of these forms. Both languages work; keep the English and German versions structurally identical.

| Form | English | German |
|---|---|---|
| Repetition in parentheses | `5× (3 min easy run, 1 min walk)` | `5× (3 Min. locker laufen, 1 Min. gehen)` |
| Repetition with recovery | `5× 2 min fast with 1:30 min jog` | `5× 2 Min. zügig mit 1:30 Min. traben` |
| Repetition without recovery (2 min jog assumed) | `3× 5 min half-marathon pace` | `3× 5 Min. HM-Tempo` |
| Last minutes | `the last 20 min at half-marathon pace` | `die letzten 20 Min. im HM-Tempo` |
| Block | `20 min fartlek`, `15 min fast`, `20 min half-marathon pace` | `20 Min. Tempowechsel`, `15 Min. zügig`, `20 Min. HM-Tempo` |
| Frame | `10 min warm-up, …, 10 min cool-down.` | `10 Min. ein, …, 10 Min. aus.` |

More under [[Analysis engine|Analysis-Engine]]. A sentence that doesn't match gives a free run. That is not an error.

### Things to keep in mind

- The **number of weeks** (27) is used in several places (plan start, week counter, chart). Changing it is a bigger task.
- Session **IDs** (`w3-wed`) must not change, otherwise stored entries no longer match.
- The titles of weeks 13 and 14 are replaced depending on the calendar (“Christmas week”, “New year”).
- After a change, check that both languages still give the same intervals (see [[Contributing and testing|Contributing-and-Testing]]) and publish a new release, see [[Build and release|Build-and-Release]].

For changing it with AI: [[Customizing with AI|Customizing-with-AI]].
