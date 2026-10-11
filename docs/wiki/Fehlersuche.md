# Fehlersuche

## Erste Hilfe

| Problem | Was hilft |
|---|---|
| „Ohne den genauen Standort kann die App nicht aufzeichnen“ | In den Android-Einstellungen unter Apps, Halbmarathon, Berechtigungen den genauen Standort erlauben. |
| „Der Standort ist ausgeschaltet“ | Standort in den Schnelleinstellungen einschalten und neu starten. |
| „Es läuft schon eine Aufzeichnung“ | Die App knüpft an die laufende an. Sonst über die Lauf-Leiste unten öffnen und beenden. |
| „GPS sucht …“ bleibt lange | Unter freien Himmel gehen und ein bis zwei Minuten warten. Der erste Punkt braucht nach längerer Pause länger. |
| „GPS ungenau“ oder „Kein Signal“ | Hohe Gebäude, Wald, Unterführungen. Ungenaue Punkte zählen nicht zur Strecke. |
| „Zu kurz“ nach dem Beenden | Weniger als 50 m oder 30 Sekunden erkannt. Standort und Empfang prüfen. |
| Keine Vibration bei Intervallen | Die Beschreibung der Einheit wird nicht als Intervall erkannt (siehe [[Plan-anpassen|Plan-anpassen]]) oder Vibration ist am Gerät aus. |
| „Die Aufzeichnung wurde beendet“ beim Öffnen | Android hat die App im Hintergrund ganz beendet. Siehe unten. |
| „Dienst beendet“ in der Laufansicht | Das System hat den Dienst beendet. „Beenden“ tippen, um die bisherigen Daten zu sichern. |
| Einträge sind weg | App-Daten gelöscht oder deinstalliert. Backup laden, siehe [[Backup-und-Daten|Backup-und-Daten]]. |
| Backup lässt sich nicht teilen | Eine andere Zielapp im Teilen-Menü wählen. |

## Wenn Android die App im Hintergrund beendet

Manche Hersteller beenden Apps im Hintergrund trotz Vordergrunddienst. Dann sind die Messpunkte weg (sie liegen bewusst nur im Arbeitsspeicher). Abhilfe:

1. In den Android-Einstellungen unter Apps, Halbmarathon, Akku die Optimierung abschalten („Nicht eingeschränkt“ oder „Nicht optimieren“, je nach Hersteller).
2. Die App nicht aus der Übersicht wischen, wenn das Gerät dafür bekannt ist.
3. Das Diagnose-Protokoll ansehen. Steht dort `exit:LOW_MEMORY` oder `svc:destroy running=1`, war es das System.

## Das Diagnose-Protokoll lesen

Unter Einstellungen, Diagnose, „Ansehen“. Es enthält nur technische Ereignisse und Zähler, keine Orte und keine Daten, siehe [[Datenschutz|Datenschutz]]. Die Zahl vorne ist die Zeit in Sekunden seit dem Start des Prozesses. Bei jedem neuen Prozess beginnt sie wieder bei 0, eingeleitet durch `proc:start`.

### Ein normaler Lauf mit Intervallen

So ungefähr sieht ein Lauf ohne Probleme aus. Die Reihenfolge innerhalb derselben Sekunde kann leicht abweichen.

```
+0s proc:start api=34
+0s plugin:load
+1s js:boot
+42s js:begin segs=12 cues=12
+42s svc:create
+42s svc:cmd running=1
+42s svc:fg ok cues=12
+42s svc:gps ok enabled=1
+42s svc:wakelock ok
+42s start:ok
+42s js:begin:ok
+51s fix:first after=9s
+542s fix:n=500
+2442s js:end fixes=2390 dead=0
+2442s svc:stop fixes=2390
```

### Typische Muster

| Muster | Bedeutung |
|---|---|
| `svc:wakelock fail SecurityException …` | Berechtigung `WAKE_LOCK` fehlt. Die Aufzeichnung läuft trotzdem, Signale können sich verspäten. Mit v1.6.1 behoben. |
| `crash:…` | Absturz. Klasse und Codestelle helfen beim Beheben. Bitte melden. |
| `svc:taskRemoved`, danach weiter `fix:n=…` | App weggewischt, Dienst lief weiter. Alles gut. |
| `proc:start`, `exit:LOW_MEMORY imp=fgservice` | Android hat den Prozess mitsamt Dienst wegen Speichermangel beendet. |
| `exit:USER_STOPPED` oder `exit:USER_REQUESTED` | Die App wurde „beenden erzwungen“ oder ein Aufräum-Programm hat sie beendet. |
| `exit:CRASH` | Absturz im vorherigen Prozess. Die Zeile `crash:` davor nennt die Stelle. |
| `svc:destroy running=1` | Das System hat den Dienst beendet, nicht die Person. |
| `svc:trim level=…` mehrfach | Speicherdruck, ein Ende des Prozesses ist wahrscheinlich. |
| `js:restore:attached …` | Die App hat nach einem Neustart angeknüpft. |
| `js:restore:service-gone` | Beim Öffnen lief kein Dienst mehr, der Lauf war verloren. |
| `js:restore:status-error try=N` | Die Brücke war beim Start kurz nicht bereit. Die App versucht es erneut. |
| `start:reject GPS_OFF` | Standort war aus. |
| `fix:first after=` sehr groß oder fehlt | schlechter Empfang am Start |

## Ein Problem melden

1. Unter Einstellungen, Diagnose, „Teilen“ wählen. Die Datei heißt `halbmarathon-diagnose.txt` und beginnt mit einem Hinweis, was nicht darin steht.
2. Lies sie einmal durch, bevor du sie weitergibst. Du siehst genau das, was du teilst.
3. Ein [Issue](https://github.com/cyberconical/halbmarathon/issues) anlegen: was du getan hast, was passiert ist, die Version der App (Android-Einstellungen, Apps, Halbmarathon) und das Protokoll.
4. Danach kannst du das Protokoll unter Einstellungen, Diagnose löschen.

Für Entwickler: [[Android-Plugin|Android-Plugin]], Abschnitt „Ereignisse“.
