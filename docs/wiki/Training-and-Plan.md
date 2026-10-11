# Training and plan

## Weekly rhythm

The plan has 27 weeks. Every week follows the same pattern:

| Day | Session |
|---|---|
| Monday | extra session (default: strength) |
| Tuesday | extra session (default: off) |
| Wednesday | **run** (short, tempo or intervals) |
| Thursday | rest |
| Friday | extra session (default: off) |
| Saturday | rest |
| Sunday | **long run** |

For Monday, Tuesday and Friday you choose in Settings: strength, bouldering, calisthenics, stretching, yoga, swimming, cycling or off. In race week there is no extra session on Friday.

## The five phases

| Phase | Weeks | Content |
|---|---|---|
| Comeback | 1 to 4 | Alternate running and walking. Joints, tendons and ligaments get used to the load again. Pace doesn't matter. |
| Base | 5 to 10 | Run without breaks, the long runs grow, first playful changes of pace. |
| Build | 11 to 18 | Intervals join in, the long run heads towards 90 minutes. |
| Half marathon shape | 19 to 25 | Pace in the target range, longest runs up to 2 hours. |
| Taper | 26 to 27 | Less volume, so you stand at the start line rested on race day. |

Every fourth week is a **recovery week** (“Breathe”, weeks 4, 8, 12, 16, 20 and 24). The longest run of 2 hours is in week 23. Week 27 ends with the half marathon itself: 21.1 km, start slowly, from km 15 you may push.

Week titles adapt to the calendar. If week 13 does not contain December 24, it is called “Stay calm” instead of “Christmas week”. If week 14 does not contain January 1, it is called “New momentum” instead of “New year”.

The whole plan exists in English and German. Switching the language under Settings changes titles and descriptions; the interval structure stays identical.

## Goal pace in descriptions

When a description mentions half-marathon pace, the app calculates it from your goal time: goal time divided by 21.0975 km. If the goal time changes, the pace in all descriptions follows.

## Logging runs

Tap a session to open it. For runs you enter:

- **kilometers** (for example 5.2, or 5,2 in German) and **time** (mm:ss or h:mm:ss)
- **feel** on a scale from “Tough” via “Okay”, “Good” and “Strong” to “Top”
- **the watched body part**: all good, slight pull or hurts (only if you entered one in Settings)
- **shoes**: barefoot shoes or regular shoes
- **time of day**: morning, midday, evening or night

The app then answers with a fitting line, for example for your first run, a new record or your fastest pace. If the watched body part hurts, it suggests 2 to 3 days of rest and repeating the previous week.

A new **longest run** is added to your records automatically, provided you already have a record for it.

If you prefer recording with GPS, see [[GPS recording|GPS-Recording]]. The result lands pre-filled in the same form; you check the values and save.

## Something else instead of the run

If a run doesn't happen, you can record what you did instead: sauna, rest day, swimming, cycling, yoga or sick. The app comments kindly and advises not to make up the run.

## “Done today?” Quick logging

In the “This week” view there are buttons for run and your extra sessions. One tap logs what you did today:

- If it's **in today's plan**, it's checked off.
- **Something else** replaces today's session.
- If the session was planned **on another day this week**, it moves to today.
- Otherwise it is added as an **extra session**.

“Undo” in the message reverts it.

## Adjusting sessions

Every session can be changed without changing the plan itself. Your changes lie as a layer on top of the plan:

- another weekday
- your own title and description
- different planned minutes (runs only, not the race)
- a different type for extra sessions
- extra sessions can be removed from the plan; runs cannot

“Back to plan” restores the suggestion at any time. Technical details: [[Data model|Data-Model]], section `edits`.

## Strength and stability

The “Strength & stability” section shows your weekly rhythm and seven exercises: squats, reverse lunges, step-ups, single-leg glute bridge, side steps with a mini band, calf raises, and plank with side plank. It also gives a rule for the watched body part: a slight pull up to 2 out of 10 is okay. If it gets stronger or is worse the next morning, take 2 to 3 days of rest and repeat the previous week.

## When race day is not exactly 27 weeks away

The app calculates the plan start backwards from race day. If the race is less than 27 weeks away, you join in the matching week. If it is further away, the plan only begins at the plan start. If race day is not a Sunday, the race still sits on the Sunday of race week. Settings shows a note about this.
