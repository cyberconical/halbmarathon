# Backup und Daten

## Wo deine Daten liegen

Alle Einträge liegen im privaten Speicher der App, genauer im `localStorage` der WebView, als ein einziger Datensatz. Es gibt keinen Server und kein Konto. Android sichert die App-Daten **nicht** automatisch in der Cloud, das hat der Build bewusst abgeschaltet.

Das heißt auch: **Beim Deinstallieren oder beim Löschen der App-Daten gehen die Einträge verloren.** Updates über Obtainium behalten sie. Ein regelmäßiges Backup ist deine Versicherung.

## Backup speichern

Oben rechts steht „Backup speichern“, wenn seit dem letzten Backup etwas dazugekommen ist, sonst „Alles gespeichert“. Ein Tipp erzeugt eine Datei `halbmarathon.html` und öffnet das Teilen-Menü von Android. Wähle Drive, Dateien oder einen Messenger.

Das Besondere: Die Backup-Datei ist **die ganze App mit deinen Einträgen darin**. Du kannst sie im Browser öffnen und siehst deine Daten, und sie lässt sich genauso wieder laden. Warum das so gebaut ist, steht im [[Low-Code-Manifest|Low-Code-Manifest]] und unter [[Architektur|Architektur]].

## Backup laden

Ganz unten auf der Seite steht „Backup laden“. Du wählst eine Backup-Datei (`.html`, oder eine reine `.json`-Datei). Die App fragt nach, bevor sie deine aktuellen Einträge ersetzt. Findet sie in der Datei keine Einträge, sagt sie es.

## Wenn Backup und Gerät sich widersprechen

Beim Start vergleicht die App den Zeitstempel (`updatedAt`) der in die Datei eingebetteten Daten mit dem der lokalen Daten. Die neueren gewinnen. Das passiert zum Beispiel, wenn du eine Backup-Datei im Browser öffnest, die neuer ist als der Browserspeicher.

## Wichtig

> **Lade niemals eine Backup-Datei in ein öffentliches Repository hoch.** Sie enthält deine persönlichen Einträge und würde sie öffentlich machen. Vor einem Upload der App in ein Repository muss die Zeile `<script id="saved-data" type="application/json">null</script>` mit `null` enden. Siehe [[Mit-KI-anpassen|Mit-KI-anpassen]].

## Was im Datensatz steckt

Läufe, Zusatzeinheiten, Ersatzeinheiten, Gewohnheiten samt Verlauf, Learnings, Profil, Rekorde, Ziele, freie Einheiten, eigene Anpassungen am Plan und die GPS-Einstellungen. Aufgezeichnete Läufe enthalten Abschnitte, Kilometer-Zeiten und optional die Route ohne Schutzbereich. Die rohen GPS-Punkte sind nicht dabei. Genaues Schema: [[Datenmodell|Datenmodell]].
