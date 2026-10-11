# Datenschutz

Die App ist so gebaut, dass sie **nichts verschicken kann, was du nicht selbst teilst**. Diese Seite fasst zusammen, was wo liegt, und sagt ehrlich, wo die Grenzen sind.

## Grundsätze

- Alle Einträge liegen im privaten Speicher der App. Es gibt keinen Server und kein Konto.
- Die App ruft keine Server auf. Sie nutzt keine Google-Dienste, keine Werbung und keine Tracker. Die Schrift liegt in der App.
- Der Standort wird nur während einer Aufzeichnung genutzt und nie gesendet. Die App verwendet dafür das GPS des Geräts direkt (Android-`LocationManager`), nicht die Google-Standortdienste.
- Android sichert die App-Daten nicht in der Cloud. Deine Einträge verlassen das Gerät nur, wenn du selbst ein Backup oder ein Protokoll teilst.

## Was wo liegt

| Daten | Ort | Wie lange |
|---|---|---|
| Einträge (Läufe, Gewohnheiten, Learnings, …) | `localStorage` der App, Schlüssel `hm2027-state` | bis zum Löschen oder Deinstallieren |
| Rohe GPS-Punkte | Arbeitsspeicher des Hintergrunddienstes | bis zum Ende der Aufzeichnung, nie auf dem Datenträger |
| Laufende Aufzeichnung (Zustand der Oberfläche) | `localStorage`, Schlüssel `hm2027-trk`, und Arbeitsspeicher des Dienstes | bis zum Ende der Aufzeichnung. Enthält Pausen und Runden, keine Orte. |
| Übernommenes Ergebnis vor dem Speichern | `localStorage`, Schlüssel `hm2027-trk-pend` | bis du den Lauf speicherst oder verwirfst |
| Route eines gespeicherten Laufs | im Lauf, als kodierte Linie, ohne Schutzbereich | mit dem Lauf |
| Diagnose-Protokoll | `files/diagnose.log` im privaten Speicher, höchstens rund 200 Zeilen | bis du es löschst oder ausschaltest |

## Schutzbereich

Ein einstellbarer Bereich (Standard 300 m) um den Start wird nie gespeichert. Strecke und Zeit zählen aber vollständig.

- Mittelpunkt ist der erste Messpunkt des Laufs. Der Radius wird pro Lauf um 15 Prozent zufällig verändert.
- Alle Punkte darin fallen vor dem Speichern weg, auch am Ende des Laufs.
- Der Mittelpunkt wird nirgends abgelegt.

**Grenze:** Der Rand der Lücke verrät die Größenordnung, nicht den genauen Ort. Wer deine Route kennt, kann die Gegend erahnen. Der Schutzbereich schützt vor dem Naheliegenden und ersetzt keine Vorsicht. Wer sicher gehen will, schaltet „Route speichern“ aus.

## Diagnose-Protokoll

Das Protokoll hilft zu klären, warum eine Aufzeichnung abbricht, ohne dass du etwas Persönliches preisgibst.

**Enthalten:**

- Ereigniswörter und Zähler, zum Beispiel „Dienst erstellt“, „Wake-Lock ok“, „Zeit bis zum ersten GPS-Punkt in Sekunden“, „Speicherwarnung Stufe 15“
- die Android-Version
- der Grund, warum Android den Prozess zuletzt beendet hat (zum Beispiel Speichermangel oder Absturz), mit einer groben Angabe, wie lange das her ist
- bei einem Absturz die Klasse und die Codestelle der Ausnahme, nicht ihre Meldung
- Zeiten nur als Sekunden seit dem Start des Prozesses

**Nie enthalten:** Orte, Koordinaten, Strecken, Datum, Uhrzeit, Namen, Gerätekennungen, Modell oder Hersteller.

**Technisch erzwungen:** Das Protokoll lässt nur eine kleine Zeichenmenge zu, begrenzt jede Zeile auf 100 Zeichen und ersetzt alles, was wie eine Koordinate aussieht (eine Zahl mit drei oder mehr Nachkommastellen).

**Deine Kontrolle:** Unter Einstellungen, Diagnose kannst du es einschalten oder ausschalten, ansehen, teilen und löschen. Es verlässt das Gerät nur, wenn du „Teilen“ wählst. Ausschalten löscht die Datei.

Mehr: [[Fehlersuche|Fehlersuche]] und [[Android-Plugin|Android-Plugin]].

## Berechtigungen

| Berechtigung | Wofür |
|---|---|
| Genauer Standort | GPS-Aufzeichnung. Wird erst beim ersten Start einer Aufzeichnung abgefragt. |
| Vordergrunddienst, Typ Standort | damit die Aufzeichnung bei ausgeschaltetem Bildschirm weiterläuft |
| Benachrichtigungen | die Anzeige, dass der Dienst läuft (ab Android 13) |
| Vibration | Signale bei Intervallen |
| Wake-Lock | CPU wach halten, damit die Signale pünktlich kommen |

Der Build prüft bei jedem Lauf, dass diese Berechtigungen im fertigen Manifest stehen, siehe [[Build-und-Release|Build-und-Release]].

## Beim Bauen erzwungen

Der Build entfernt das Google-Services-Plugin, schaltet `allowBackup` ab und bricht ab, wenn eins von beidem nicht gelingt. Das schützt davor, dass versehentlich Google-Dienste oder die Cloud-Sicherung wieder auftauchen.
