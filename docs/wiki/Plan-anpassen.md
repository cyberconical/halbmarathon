# Plan anpassen

Es gibt zwei Wege: **in der App** für dich allein, oder **im Code** für alle, die die App installieren.

## In der App

- **Wettkampftag und Zielzeit** in den Einstellungen verschieben den Kalender und die Zielpace.
- **Zusatzeinheiten** für Montag, Dienstag und Freitag in den Einstellungen.
- **Einzelne Einheiten** über „Einheit anpassen“: Tag, Titel, Beschreibung, Minuten, Art, entfernen. „Zurück zum Plan“ stellt den Vorschlag wieder her.

Ändert man die Beschreibung eines Laufs, ändern sich auch die Intervalle der GPS-Aufzeichnung, weil die App sie aus dem Text liest. Siehe [[GPS-Aufzeichnung|GPS-Aufzeichnung]].

## Im Code

Die Struktur des Plans steht in `index.html`, Skript `app`, ganz oben.

### `PHASES`

```js
{ name:"Aufbau", weeks:[11,18], text:"Intervalle kommen dazu, der lange Lauf geht Richtung 90 Minuten." }
```

Name, erste und letzte Woche, Beschreibung. Die Phasen müssen alle 27 Wochen lückenlos abdecken.

### `PLAN`

Eine Zeile pro Woche, genau 27 Zeilen:

```js
["Erste Intervalle",
  R("Intervalle", 40, "10 Min. ein, 5× 2 Min. zügig mit 1:30 Min. traben, 10 Min. aus."),
  R("Langer Lauf", 60, "Eine Stunde. Das erste große Ding.")]
```

- Erster Wert: Titel der Woche.
- Zweiter Wert: Lauf am **Mittwoch**, dritter: Lauf am **Sonntag**.
- `R(Titel, Minuten, Beschreibung)`. Minuten `0` heißt ohne Zeitvorgabe (nur beim Wettkampf).
- `{HMP}` in einer Beschreibung wird durch die Zielpace ersetzt, zum Beispiel `im Halbmarathon-Tempo (ca. {HMP} /km)`.
- Der Sonntag der letzten Woche ist der Wettkampf.

### Beschreibungen, die die GPS-Engine versteht

Damit Intervalle erkannt werden, schreibe sie in einer dieser Formen:

| Form | Beispiel |
|---|---|
| Wiederholung in Klammern | `5× (3 Min. locker laufen, 1 Min. gehen)` |
| Wiederholung mit Pause | `5× 2 Min. zügig mit 1:30 Min. traben` |
| Wiederholung ohne Pause (2 Min. Traben angenommen) | `3× 5 Min. HM-Tempo` |
| Block | `20 Min. Tempowechsel`, `15 Min. zügig`, `20 Min. HM-Tempo` |
| Schluss | `die letzten 20 Min. im HM-Tempo` |
| Rahmen | `10 Min. ein, …, 10 Min. aus.` |

Mehr dazu unter [[Auswertungs-Engine|Auswertungs-Engine]]. Ein Satz, der nicht passt, ergibt einen freien Lauf. Das ist kein Fehler.

### Was beim Ändern zu beachten ist

- Die **Zahl der Wochen** (27) steckt an mehreren Stellen (Planstart, Wochenzähler, Diagramm). Sie zu ändern ist eine größere Aufgabe.
- Die **IDs** der Einheiten (`w3-wed`) dürfen sich nicht ändern, sonst passen gespeicherte Einträge nicht mehr.
- Die Wochentitel 13 und 14 werden je nach Kalender ersetzt („Weihnachtswoche“, „Neues Jahr“).
- Nach der Änderung ein neues Release veröffentlichen, siehe [[Build-und-Release|Build-und-Release]].

Für die Änderung mit KI: [[Mit-KI-anpassen|Mit-KI-anpassen]].
