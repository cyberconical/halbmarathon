# Datenmodell

Alle Einträge stecken in **einem** JSON-Datensatz im `localStorage` der App. Diese Seite beschreibt ihn vollständig, dazu die Regeln, die ihn über alle Versionen hinweg lesbar halten.

## Speicherschlüssel

| Schlüssel | Inhalt | Lebensdauer |
|---|---|---|
| `hm2027-state` | der Datensatz (unten) | dauerhaft |
| `hm2027-state-tab` | zuletzt geöffneter Tab | dauerhaft, nur Bequemlichkeit |
| `hm2027-trk` | laufende Aufzeichnung: `{id, startedAt, pauses, laps, native}` | bis zum Ende der Aufzeichnung |
| `hm2027-trk-pend` | übernommenes GPS-Ergebnis vor dem Speichern: `{id, km, sec, trk}` | bis der Lauf gespeichert oder verworfen ist |

Dazu kommt, nur in der Android-App, das Diagnose-Protokoll als Datei `files/diagnose.log` und ein kleiner Schalter in den `SharedPreferences` (`halbmarathon_diag`). Siehe [[Android-Plugin|Android-Plugin]].

## Der Datensatz

```js
state = {
  logs:      { "w3-wed": { km, sec, feel, knee, shoe, tod, ts, trk? } },
  strength:  { "w3-mon": true },
  alt:       { "w3-fri": { what, ts } },
  habits:    [ { id, name, type: "check" | "number", unit, created } ],
  habitLog:  { <habitId>: { "2026-10-04": "x" | "o" | 7.5 } },
  learnings: [ { id, date, ts, text, type: "insight" | "issue", tag, solved } ],
  profile:   { raceDate: "2027-04-04", goalSec: 7200, problem: "Knie",
               mon: "Kraft", tue: "Bouldern", fri: "Bouldern" },        // "Aus" = frei
  records:   [ { id, sport: "lauf"|"boulder"|"stretch"|"cali", key, name, value, date, former, ts } ],
  goals:     [ { id, sport: "allg"|"lauf"|"boulder"|"stretch"|"cali", text, due, done, ts } ],
  workouts:  [ { id, sport: "boulder"|"kraft"|"cali"|"stretch"|"other", date, min, note, ts } ],
  dur:       { "w3-tue": 75 },
  edits:     { "w3-wed": { t, d, min, off, label, skip } },
  gps:       { zone: 0|150|300|500|1000, save: true|false },
  updatedAt: <Zeitstempel ms>
}
```

## Felder im Einzelnen

### IDs der Einheiten

Die Schlüssel von `logs`, `strength`, `alt`, `dur` und `edits` sind Einheiten-IDs: `w` plus Wochennummer (1 bis 27), dann `-mon`, `-tue`, `-wed`, `-fri` oder `-sun`. Sie hängen nicht am Kalender. Ändert sich der Wettkampftag, bleiben alle Einträge ihrer Woche zugeordnet.

### `logs`: Läufe

| Feld | Typ | Bedeutung |
|---|---|---|
| `km` | Zahl | Kilometer, größer als 0 und kleiner als 60 |
| `sec` | Zahl | Zeit in Sekunden, mehr als 60 und weniger als 6 Stunden |
| `feel` | 1 bis 5 oder `null` | Zäh, Okay, Gut, Stark, Top |
| `knee` | `"gut"`, `"zieht"`, `"schmerzt"` oder `null` | die beobachtete Stelle (der Name ist historisch) |
| `shoe` | `"barfuss"`, `"normal"` oder `null` | Schuhe |
| `tod` | `"morgen"`, `"mittag"`, `"abend"`, `"nacht"` oder `null` | Tageszeit |
| `ts` | Zeitstempel | wann eingetragen |
| `trk` | Objekt, optional | Ergebnis einer GPS-Aufzeichnung, siehe unten |

### `logs[id].trk`: GPS-Ergebnis

```js
trk = {
  iv:    [ ["work"|"rest"|"easy"|"lap", "Zügig", sekunden, meter], ... ],   // Abschnitte
  sp:    [ sekundenProKm, ... ],                                            // volle Kilometer
  tail:  [meter, sekunden] | null,                                          // angebrochener letzter Kilometer
  route: [ "<kodierte Linie>", ... ] | null                                 // ohne Schutzbereich
}
```

Die Route ist eine Liste von Linienstücken im Polyline-Format mit Genauigkeit 1e-5 (rund 1 m). Mehrere Stücke entstehen durch Pausen, GPS-Lücken und den Schutzbereich. Rohe Messpunkte werden nie gespeichert. Details: [[Auswertungs-Engine|Auswertungs-Engine]].

### `strength`, `alt`, `dur`

- `strength[id] = true`: Eine Zusatzeinheit des Plans ist erledigt.
- `alt[id] = {what, ts}`: Statt der Einheit war etwas anderes, zum Beispiel `"Sauna"`, `"Krank"` oder eine andere Sportart aus der Schnellerfassung.
- `dur[id]`: optionale Dauer in Minuten für eine erledigte Zusatzeinheit.

### `habits` und `habitLog`

- `type: "check"`: Im Log steht pro Tag `"x"` (geschafft) oder `"o"` (nicht geschafft).
- `type: "number"`: Im Log steht pro Tag eine Zahl, `unit` ist die Einheit.
- Die Tage sind Schlüssel im Format `JJJJ-MM-TT` in lokaler Zeit.

### `edits`: eigene Anpassungen am Plan

Anpassungen liegen **über** dem Plan und ändern ihn nicht. Gespeichert werden nur geänderte Felder, damit zum Beispiel die Zielpace in Beschreibungen weiter mitwandert.

| Feld | Bedeutung | Grenzen |
|---|---|---|
| `t` | eigener Titel | bis 40 Zeichen |
| `d` | eigene Beschreibung | bis 400 Zeichen |
| `min` | geplante Minuten | nur Läufe, nicht der Wettkampf, 1 bis 300 |
| `off` | Wochentag als Abstand zum Montag | 0 bis 6 |
| `label` | andere Art | nur Zusatzeinheiten, aus `DAYCHOICES` |
| `skip` | aus dem Plan entfernt | nur Zusatzeinheiten |

### `records` und `goals`

- Ein Rekord mit `former: true` steht unter „Früher“. Ein neuer aktueller Rekord gleichen Namens in derselben Sportart setzt den alten automatisch auf `former`.
- `key` ist bei Lauf-Vorlagen gesetzt (`longest`, `5k`, `10k`, `hm`, `m`). Für `longest` trägt die App neue Bestwerte selbst ein.
- Ziele haben ein optionales Fälligkeitsdatum `due` und `done`.

### `workouts`: freie Einheiten

Einträge außerhalb des Plans aus der Wochenübersicht oder der Schnellerfassung. `date` im Format `JJJJ-MM-TT`, `min` 0 bis 600, `note` bis 40 Zeichen.

### `gps`

`zone` ist der Radius des Schutzbereichs in Metern (Standard 300), `save` ob Routen gespeichert werden (Standard ja).

## `normalize()`: die einzige Tür

Jeder Datensatz läuft beim Laden durch `normalize()`, egal ob aus `localStorage`, aus einer Backup-Datei oder leer. Die Funktion

- setzt für jedes fehlende Feld einen Standardwert,
- prüft Werte gegen erlaubte Listen (`DAYCHOICES`, Schutzbereiche),
- erkennt Daten aus der Zeit vor den Einstellungen (`logs` vorhanden, `profile` fehlt) und ergänzt passende Werte: Wettkampf am 4. April 2027, beobachtete Stelle Knie, Bouldern am Dienstag und Freitag, zwei frühere Rekorde.

## Regeln für Änderungen

1. Der Schlüssel `hm2027-state` und alle vorhandenen Felder werden **nie umbenannt oder entfernt**.
2. Neue Felder sind **optional** und bekommen in `normalize()` einen Standardwert.
3. Neue Werte in Aufzählungen müssen mit alten Daten zurechtkommen. Ein unbekannter alter Wert darf nichts kaputt machen.
4. Vor einer Änderung am Datensatz in der App ein Backup speichern und nach dem Update prüfen, ob alles da ist.

## Woher die Daten beim Start kommen

Die App liest zuerst `localStorage`, dann den eingebetteten Block `saved-data`. Ist dessen `updatedAt` neuer, gewinnt er. So lässt sich eine Backup-Datei im Browser öffnen und zeigt sofort die Daten. Danach wird der Stand zurück in `localStorage` geschrieben. Siehe [[Backup-und-Daten|Backup-und-Daten]].
