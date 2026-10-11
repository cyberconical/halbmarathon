# Auswertungs-Engine

Der Block `<script id="engine">` in `index.html` stellt das Objekt `window.HMTrk` bereit. Er enthält nur **reine Funktionen**: kein Zugriff auf Gerät, Speicher oder Oberfläche. Er läuft im Browser und in Node gleich (`globalThis`) und lässt sich daher getrennt testen. Warum das so gebaut ist: [[Low-Code-Manifest|Low-Code-Manifest]], Grundsatz 6.

## Die Funktionen

| Funktion | Eingabe | Ausgabe |
|---|---|---|
| `parseSegments(desc, planMin)` | Beschreibung der Einheit, geplante Minuten | Abschnitte `[{k, sec, label}]`, leer = freier Lauf |
| `cuesFor(segs)` | Abschnitte | Signale `[{at, v, n}]` für den Dienst |
| `current(segs, act)` | Abschnitte, Laufzeit in s | aktueller Abschnitt, Restzeit, nächster |
| `analyze(fixes, startMs, pauses, nowMs)` | Rohpunkte, Start, Pausen | bereinigte Punkte, Strecke, Laufzeit |
| `distAt(pts, a)` | Punkte, Laufzeit | Strecke zu diesem Zeitpunkt (interpoliert) |
| `intervals(pts, segs, active)` | Punkte, Abschnitte, Laufzeit | Zeit und Meter je Abschnitt, plus „Danach“ |
| `lapSegs(laps, active)` | eigene Rundenmarken | Abschnitte aus Runden |
| `splits(pts, total)` | Punkte, Laufzeit | Zeit je Kilometer, plus angebrochener Rest |
| `livePace(pts, active)` | Punkte, Laufzeit | Pace der letzten rund 20 Sekunden |
| `simplify(pts, tol)` | Linie, Toleranz in m | vereinfachte Linie (Douglas-Peucker) |
| `encode(pts)` / `decode(s)` | Linie / Text | Polyline-Kodierung, Genauigkeit 1e-5 |
| `buildRoute(pts, zoneM, rand)` | Punkte, Schutzbereich | kodierte Linienstücke ohne Schutzbereich |
| `hav(a, b)` | zwei Punkte | Abstand in Metern (Haversine) |

## Abschnitte aus dem Text lesen

`parseSegments` erkennt diese Muster, in dieser Reihenfolge:

1. **Wiederholung in Klammern**: `5× (3 Min. locker laufen, 1 Min. gehen)` → abwechselnd `work` und `rest`, Bezeichnung jeweils aus dem letzten Wort.
2. **Wiederholung mit Pause**: `5× 2 Min. zügig mit 1:30 Min. traben`. Ohne genannte Pause (`3× 5 Min. HM-Tempo`) gelten 2 Minuten Traben.
3. **Block**: `20 Min. Tempowechsel`, `15 Min. zügig`, `20 Min. HM-Tempo`.
4. **Schluss**: `die letzten 20 Min. im HM-Tempo`. Der Rest der geplanten Zeit wird ein lockerer Abschnitt davor.

Dazu kommen `N Min. ein` beziehungsweise `einlaufen` am Anfang und `N Min. aus` beziehungsweise `auslaufen` am Ende. Zeiten sind Minuten (`3`) oder Minuten und Sekunden (`1:30`). Text in Klammern wird außer bei Muster 1 ignoriert, etwa `(ca. 5:41 /km)`. Ergibt sich weniger als zwei Abschnitte, ist das Ergebnis leer, dann ist es ein freier Lauf. Höchstens 60 Wiederholungen werden gebildet.

Abschnittsarten: `work` (Tempo), `rest` (Pause), `easy` (locker, Ein- und Auslaufen), `lap` (eigene Runde).

## Signale

`cuesFor` erzeugt für jeden Wechsel ein Signal mit Laufzeit in Sekunden, Art (`work`, `rest`, `end`) und Text für die Benachrichtigung, etwa `Zügig · 2:00`. Am Ende steht `Geschafft. Locker austraben.`. Die Liste geht als JSON an den Dienst, der sie plant und bei Pausen verschiebt.

## Strecke aus Rohpunkten

Ein Rohpunkt ist `[Zeit ms, Breite, Länge, Genauigkeit m, Tempo m/s]`. Fehlende Werte sind `-1`. `analyze` filtert und rechnet:

- Punkte, die älter als der Start sind oder in der Zeit zurückgehen, fallen weg.
- **Genauigkeit über 35 m** fällt weg.
- Punkte in einer **Pause** zählen nicht. Nach einer Pause beginnt ein neues Linienstück.
- **Sprünge über 9 m/s** gelten als Ausreißer und werden verworfen, höchstens fünf am Stück.
- **Strecke bevorzugt aus der Doppler-Geschwindigkeit** des GPS-Chips: Mittel der beiden Geschwindigkeiten mal Zeitabstand, wenn beide Punkte eine Geschwindigkeit haben und höchstens 5 Sekunden auseinanderliegen. Geschwindigkeiten unter 0,5 m/s zählen als Stehen. Das ist viel genauer als der Abstand verrauschter Positionen.
- Sonst zählt der **Abstand der Punkte**, aber erst ab 3 m Bewegung oder 8 Sekunden. So zählt Stehen an der Ampel nicht als Strecke.

Die Laufzeit ist die Zeit seit dem Start minus alle Pausen. Ergebnis: bereinigte Punkte `{a (Laufzeit), d (Strecke), lat, lon, brk}`, Gesamtstrecke und aktive Zeit.

## Intervalle, Splits, Pace

- `intervals` misst je Abschnitt die Strecke in dessen Zeitfenster. Sie sind zeitbasiert. Bleibt nach dem Plan mehr als 15 Sekunden übrig, kommt ein Abschnitt „Danach“ dazu.
- `splits` liefert die Zeit je vollem Kilometer, interpoliert genau auf den Kilometerpunkt. Ein Rest ab 100 m wird als angebrochener Kilometer geliefert.
- `livePace` schaut rund 20 Sekunden zurück und braucht mindestens 15 m Strecke. Sonst ist das Ergebnis `null`, die Anzeige zeigt dann einen Strich.

## Route bauen und schützen

`buildRoute`:

1. Ist ein Schutzbereich eingestellt, wird ein Kreis um den **ersten Punkt** gelegt, Radius × (0,85 bis 1,15) zufällig.
2. Punkte im Kreis werden weggelassen, das Linienstück wird dort getrennt. Das gilt auch am Ende des Laufs.
3. Jedes Stück wird mit 4 m Toleranz vereinfacht und kodiert.
4. Kreismittelpunkt und Radius werden nicht zurückgegeben und nirgends gespeichert.

Für Tests lässt sich der Zufall über den Parameter `rand` festlegen.

## Testen in Node

Weil die Engine nichts außer `globalThis` braucht, kann man sie direkt laden:

```js
const fs = require("fs"), vm = require("vm");
const html = fs.readFileSync("index.html", "utf8");
const src = html.match(/<script id="engine">([\s\S]*?)<\/script>/)[1];
vm.runInThisContext(src);
const segs = HMTrk.parseSegments("10 Min. ein, 5× 2 Min. zügig mit 1:30 Min. traben, 10 Min. aus.", 40);
console.log(segs.map(s => s.k + " " + s.sec).join(", "));
// easy 600, work 120, rest 90, work 120, … work 120, easy 600
```

Mehr dazu unter [[Mitwirken-und-Testen|Mitwirken-und-Testen]].
