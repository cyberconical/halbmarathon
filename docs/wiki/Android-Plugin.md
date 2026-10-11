# Android-Plugin `run-tracker`

Das einzige native Stück der App. Es liegt unter `plugins/run-tracker/` und wird beim Build eingebunden. Es tut nur, was eine Webseite nicht kann: **den Standort im Hintergrund lesen und zu festen Zeiten vibrieren.**

## Warum ein eigenes Plugin

Die fertigen Capacitor-Plugins für den Standort hängen an den Google Play Services. Die App soll ohne Google-Dienste laufen. Deshalb nutzt `run-tracker` nur den Android-`LocationManager` mit dem GPS-Anbieter.

## Dateien

| Datei | Aufgabe |
|---|---|
| `RunTrackerPlugin.java` | Brücke zu JavaScript: Methoden, Berechtigungen |
| `RunTrackerService.java` | Vordergrunddienst: GPS lesen, Punkte sammeln, Signale, Benachrichtigung |
| `RunLog.java` | anonymes Diagnose-Protokoll |
| `AndroidManifest.xml` | Berechtigungen und Dienst-Eintrag (Typ `location`) |
| `build.gradle`, `package.json` | Einbindung in Capacitor 6 |

## Methoden für JavaScript

Aufruf in `index.html` über `Capacitor.Plugins.RunTracker`.

| Methode | Parameter | Rückgabe | Zweck |
|---|---|---|---|
| `checkPermissions()` / `requestPermissions()` | `{permissions: ["location" \| "notifications"]}` | Status je Alias | aus der Basisklasse |
| `start` | `cues` (JSON-Text), `meta` (Einheiten-ID) | `{startedAt}` | Dienst starten. Fehlercodes: `PERMISSION`, `GPS_OFF`, `RUNNING`, `START_FAILED` |
| `pause` / `resume` | – | `{t}` | Signale anhalten und neu planen |
| `stop` | – | `{count}` | Aufzeichnung beenden, Punkte bleiben bis zum nächsten Start abrufbar |
| `getFixes` | `from` | `{fixes, next, running, paused, gps}` | Punkte ab Position `from`, höchstens 2.000 je Aufruf |
| `getStatus` | – | `{running, paused, gps, gpsEnabled, count, startedAt, meta, cues, state}` | Zustand zum Wieder-Anknüpfen |
| `saveState` | `state` (JSON-Text) | – | kleinen Zustand der Oberfläche im Dienst ablegen |
| `diagLog` | `e` (Text) | – | Ereignis der Oberfläche ins Protokoll schreiben |
| `getLog` | – | `{text, enabled}` | Protokoll lesen |
| `clearLog` | – | – | Protokoll löschen |
| `setLogEnabled` | `on` | – | Protokoll ein- oder ausschalten (aus löscht es) |

## Der Dienst

- Startet als **Vordergrunddienst vom Typ `location`** mit einer Benachrichtigung „Lauf wird aufgezeichnet“ im Kanal „Lauf-Aufzeichnung“ (niedrige Wichtigkeit). Ein Tipp darauf öffnet die App.
- Fordert alle **1.000 ms** einen GPS-Punkt an, ohne Mindestabstand.
- Speichert Punkte als `[Zeit, Breite, Länge, Genauigkeit, Tempo]` in einer Liste **nur im Arbeitsspeicher**, höchstens 30.000 (über 8 Stunden bei einem Punkt pro Sekunde).
- Plant die Signale auf der aktiven Laufzeit. Bei Pause werden sie gestrichen, bei Weiter neu geplant.
- Hält bei Läufen mit Signalen ein **Wake-Lock** (höchstens 4 Stunden), damit die Signale bei ausgeschaltetem Bildschirm pünktlich kommen. Ein Fehler dabei wird protokolliert und stoppt die Aufzeichnung nicht.
- Startet sich nach einem Prozess-Ende **nicht** neu (`START_NOT_STICKY`). Ohne Punkte gibt es nichts weiterzuführen.
- Muss Android den Dienst früh beenden, ruft er trotzdem `startForeground()` auf. Sonst würde Android die App wegen eines gebrochenen Versprechens beenden.

Der Zustand liegt in statischen Feldern im selben Prozess wie das Plugin. Stirbt nur die Oberfläche (WebView, Activity), bleibt er erhalten. Stirbt der Prozess, ist er weg.

## Berechtigungen

| Berechtigung | Wofür |
|---|---|
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | GPS. Android 12 und neuer verlangt den ungefähren Standort mit. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION` | Dienst im Vordergrund vom Typ Standort |
| `POST_NOTIFICATIONS` | die Benachrichtigung des Dienstes ab Android 13 |
| `VIBRATE` | Signale |
| `WAKE_LOCK` | CPU wach halten für pünktliche Signale. **Fehlt sie, beendet Android die App beim Start einer Aufzeichnung mit Intervallen.** |

Der Build prüft das zusammengeführte Manifest auf genau diese Liste, siehe [[Build-und-Release|Build-und-Release]].

## Diagnose-Protokoll (`RunLog`)

Ein kleines, anonymes Protokoll für die Fehlersuche. Die Regeln sind im Code erzwungen, nicht nur dokumentiert:

- **Ort:** `files/diagnose.log` im privaten Speicher der App. Höchstens 200 Zeilen, dann werden die ältesten auf 150 gekürzt.
- **Zeit:** nur Sekunden seit dem Start des Prozesses (`+12s`), keine Uhrzeit und kein Datum.
- **Zeichen:** nur Buchstaben a bis z, Ziffern, Leerzeichen und `_ : . = + - @ $ ( )`. Alles andere wird zu `?`. Höchstens 100 Zeichen pro Zeile.
- **Koordinaten-Schutz:** Jede Zahl mit drei oder mehr Nachkommastellen wird durch `#` ersetzt.
- **Abstürze:** Klasse und Codestelle der Ausnahme (`SecurityException top=… app=RunTrackerService.startTracking:212`), nie die Meldung. Danach läuft der normale Absturzweg weiter.
- **Grund des letzten Prozess-Endes:** ab Android 11 über `ApplicationExitInfo`, zum Beispiel `exit:LOW_MEMORY imp=fgservice vor=3min`. Jeder Grund wird nur einmal vermerkt.
- **Schalter:** in `SharedPreferences` (`halbmarathon_diag`). Ausschalten löscht die Datei.

### Ereignisse

| Ereignis | Bedeutung |
|---|---|
| `proc:start api=N` | neuer Prozess, Android-API-Stufe |
| `exit:REASON imp=… vor=Nmin` | warum Android den vorherigen Prozess beendet hat |
| `plugin:load` | Plugin geladen (Oberfläche gestartet) |
| `start:ok`, `start:reject CODE`, `start:fail …` | Start einer Aufzeichnung |
| `svc:create`, `svc:cmd running=…` | Dienst erstellt und gestartet |
| `svc:fg ok cues=N`, `svc:fg fail …` | Vordergrund erreicht oder nicht |
| `svc:gps ok enabled=…`, `svc:gps fail …` | GPS angefordert |
| `svc:wakelock ok`, `svc:wakelock fail …` | Wake-Lock |
| `fix:first after=Ns`, `fix:n=N` | erster Punkt nach N Sekunden, Zähler alle 500 Punkte |
| `gps:off`, `gps:back` | GPS ausgeschaltet oder wieder da |
| `svc:pause`, `svc:resume`, `svc:stop fixes=N` | Bedienung |
| `svc:taskRemoved` | App aus der Übersicht gewischt, Dienst läuft weiter |
| `svc:trim level=N`, `svc:lowMemory` | Speicherdruck |
| `svc:destroy running=1` | **das System** hat den Dienst beendet (bei `running=0` war es die Person) |
| `crash:…` | Absturz mit Klasse und Codestelle |
| `js:boot`, `js:begin …`, `js:restore:…`, `js:end …` | Ereignisse der Oberfläche |

Wie man ein Protokoll liest, steht unter [[Fehlersuche|Fehlersuche]].

### Eigene Ereignisse hinzufügen

- In Java: `RunLog.add("bereich:was n=" + zahl);`
- In `index.html`: `hmlog("bereich:was n=" + zahl);`

Nur feste Wörter und kleine Zahlen. Nie Orte, Zeitpunkte, Texte von Personen oder Bezeichner von Einheiten.
