# GPS-Aufzeichnung

Läufe lassen sich optional per GPS aufzeichnen: Strecke, Zeit, Pace, jeder Abschnitt eines Intervalltrainings und jeder Kilometer. Bei Intervallen vibriert das Handy zum Wechsel. Der Bildschirm darf dabei aus bleiben.

Die App nutzt dafür das GPS des Geräts direkt, nicht die Google-Standortdienste. Der Standort wird nur während einer Aufzeichnung genutzt und nie gesendet. Mehr dazu unter [[Datenschutz|Datenschutz]].

## So läuft eine Aufzeichnung ab

1. Eine Einheit aufklappen und **„Mit GPS aufzeichnen“** tippen. Die Schaltfläche erscheint, solange für die Einheit noch kein Lauf eingetragen ist.
2. Der Bereitschaftsbildschirm zeigt, was die App im Plan erkannt hat, wie groß dein Schutzbereich ist und ob die Route gespeichert wird. **Start** tippen.
3. Beim ersten Start fragt Android nach dem **genauen Standort** und, ab Android 13, nach **Benachrichtigungen**. Die Benachrichtigung zeigt, dass der Dienst läuft.
4. Die Laufansicht zeigt **Zeit, Kilometer, aktuelle Pace** und, bei Intervallen, den aktuellen Abschnitt mit „noch mm:ss“ und dem nächsten.
5. **Pause** hält die Zeit an, **Runde** setzt eine eigene Marke (bei freien Läufen), **Beenden** schließt die Aufzeichnung ab.
6. Die Zusammenfassung zeigt Zeit, Strecke, Pace, Abschnitte, Kilometer-Zeiten und die Route. **„In den Lauf übernehmen“** füllt das Formular der Einheit vor. Du prüfst die Werte und speicherst den Lauf wie gewohnt. **Verwerfen** wirft alles weg.

Ist die Aufzeichnung sehr kurz (unter 50 m oder unter 30 Sekunden), meldet die App „Zu kurz“ und rät, den Standort und freien Himmel zu prüfen.

## Intervalle erkennen

Die App liest die Abschnitte aus der **Beschreibung der Einheit**. Beispiele für Texte, die sie versteht:

- `5× (3 Min. locker laufen, 1 Min. gehen)`
- `10 Min. ein, 5× 2 Min. zügig mit 1:30 Min. traben, 10 Min. aus.`
- `10 Min. ein, 2× 10 Min. im Halbmarathon-Tempo mit 3 Min. traben, 10 Min. aus.`
- `10 ein, 20 Min. Tempowechsel nach Gefühl, 10 aus.`
- `105 Min., die letzten 20 Min. im HM-Tempo.`

Passt der Text zu keinem Muster, gilt der Lauf als **freier Lauf**. Dann sind Runden von Hand möglich, und die Zusammenfassung zeigt deine Runden als Abschnitte. Steht bei Wiederholungen keine Pause im Text, rechnet die App mit 2 Minuten Traben. Die Abschnitte sind zeitbasiert, die Strecke je Abschnitt ist die Strecke in dieser Zeit. Sehr kurze Abschnitte (Steigerungen, 20 Sekunden) sind per GPS nicht verlässlich messbar und werden nicht ausgewertet.

Wenn du eine Beschreibung änderst (siehe [[Training-und-Plan|Training-und-Plan]], „Einheiten anpassen“), erkennt die App den neuen Text beim nächsten Start.

## Vibrationssignale

Die Signale für Wechsel im Plan kommen vom Hintergrunddienst und funktionieren deshalb auch bei ausgeschaltetem Bildschirm. Die Rundenmarke setzt die geöffnete App:

| Wechsel | Signal |
|---|---|
| zu einem Tempo-Abschnitt | zweimal kurz |
| zu einer Pause | einmal lang |
| Ende des Plans | dreimal kurz |
| Runde (freier Lauf) | einmal sehr kurz |

Zusätzlich zeigt die Benachrichtigung den neuen Abschnitt an.

## Schutzbereich

Um den Startpunkt liegt ein Bereich, dessen Punkte **nie gespeichert** werden. Strecke und Zeit zählen trotzdem vollständig. Du wählst die Größe in den Einstellungen: aus, 150 m, 300 m (Standard), 500 m oder 1 km. Der Radius wird pro Lauf um 15 Prozent zufällig verändert, und der Mittelpunkt wird nirgends abgelegt. Der Schutzbereich gilt auch am Ende des Laufs, wenn du zurück am Start bist. Er schützt vor dem Naheliegenden, ersetzt aber keine Vorsicht: Wer deine Route kennt, kann die Gegend erahnen.

## Route

Die Route erscheint als gezeichnete Linie im Lauf und, übereinandergelegt, in der Statistik. **Es werden keine Kartenkacheln geladen**, die App ruft keine Server auf. In den Einstellungen kannst du „Route speichern“ auf „Nein“ stellen. Dann bleiben Zeiten, Strecken und Intervalle erhalten, aber es gibt keine Karte.

## Was die Anzeige „GPS“ bedeutet

| Anzeige | Bedeutung |
|---|---|
| GPS sucht … | noch kein Punkt empfangen |
| GPS ok | frischer Punkt, Genauigkeit unter 25 m |
| GPS ungenau (±N m) | schlechter Empfang |
| Kein Signal | seit mehr als 12 Sekunden kein Punkt |
| Pausiert | Aufzeichnung ist in Pause |
| Dienst beendet | das System hat den Hintergrunddienst beendet |

## Die App im Hintergrund oder geschlossen

- Beendet Android nur die **Oberfläche** der App im Hintergrund, läuft der Dienst weiter. Öffnest du die App wieder, knüpft sie an und zeigt den Lauf sofort. Die Pausen und Runden hält der Dienst zusätzlich bereit.
- Beendet das System den **ganzen Prozess** (zum Beispiel ein aggressiver Akkumanager), sind die Messpunkte weg. Das ist gewollt, es liegt nie etwas Unfertiges auf dem Datenträger. Die App sagt dir dann klar, dass die Aufzeichnung beendet wurde. Unter Einstellungen, Diagnose steht, warum Android die App beendet hat.
- Wenn dein Hersteller Apps im Hintergrund früh beendet, hilft es meist, die Akku-Optimierung für diese App auszuschalten. Die Einstellung heißt je nach Hersteller etwas anders.

Technisch dazu [[Android-Plugin|Android-Plugin]] und [[Fehlersuche|Fehlersuche]].

## Berechtigungen

Genauer Standort, Vordergrunddienst (Typ Standort), Benachrichtigungen, Vibration und Wake-Lock. Standort und Benachrichtigungen werden erst beim ersten Start einer Aufzeichnung abgefragt. Das Wake-Lock hält bei Intervallen die CPU wach, damit die Signale pünktlich kommen.
