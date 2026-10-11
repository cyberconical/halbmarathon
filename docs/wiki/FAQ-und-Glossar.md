# FAQ und Glossar

## Häufige Fragen

**Brauche ich ein Konto oder Internet?**
Nein. Die App läuft komplett offline und hat kein Konto.

**Meine Einträge sind weg. Was ist passiert?**
Entweder wurden die App-Daten gelöscht oder die App wurde deinstalliert. Dann hilft nur ein Backup. Updates über Obtainium behalten die Einträge. Siehe [[Backup-und-Daten|Backup-und-Daten]].

**Warum fragt die App nach genauem Standort?**
Für die GPS-Aufzeichnung. Android 12 und neuer verlangt dafür auch den ungefähren Standort mit. Die App nutzt ihn nur während einer Aufzeichnung.

**Ich habe einen Lauf ohne GPS gemacht. Kann ich ihn eintragen?**
Ja, jeder Lauf lässt sich von Hand mit Kilometern und Zeit eintragen.

**Der Wettkampf liegt weniger als 27 Wochen entfernt.**
Dann steigst du in der passenden Woche ein. Siehe [[Training-und-Plan|Training-und-Plan]].

**Ich möchte eine Einheit verschieben oder ändern.**
Einheit aufklappen, „Einheit anpassen“. „Zurück zum Plan“ stellt den Vorschlag wieder her.

**Die Aufzeichnung ist beim Start abgebrochen oder der Lauf war nach dem Öffnen weg.**
Siehe [[Fehlersuche|Fehlersuche]]. Das Diagnose-Protokoll unter Einstellungen zeigt, was passiert ist, ohne etwas Persönliches zu verraten.

**Kann ich die App ändern, ohne programmieren zu können?**
Ja, dafür ist sie gebaut. Siehe [[Mit-KI-anpassen|Mit-KI-anpassen]] und das [[Low-Code-Manifest|Low-Code-Manifest]].

**Gibt es eine iPhone-Version?**
Nein. Die App ist eine Android-App. Die Datei `index.html` läuft zwar in jedem Browser, die GPS-Aufzeichnung im Hintergrund gibt es aber nur in der Android-Hülle.

## Glossar

| Begriff | Bedeutung |
|---|---|
| **Einheit** | ein Eintrag im Plan: Lauf oder Zusatzeinheit an einem Tag |
| **Zusatzeinheit** | Kraft, Bouldern, Calisthenics, Stretching, Yoga, Schwimmen oder Rad an Montag, Dienstag oder Freitag |
| **Entlastungswoche** | „Durchatmen“, jede vierte Woche mit weniger Umfang |
| **Pace** | Zeit pro Kilometer, zum Beispiel 5:30 min/km |
| **Zielpace** | Zielzeit geteilt durch 21,0975 km |
| **Abschnitt** | Teil eines Intervalltrainings: Tempo, Pause oder Einlaufen |
| **Cue / Signal** | Vibration beim Wechsel zwischen Abschnitten |
| **Fix** | ein einzelner GPS-Messpunkt |
| **Schutzbereich** | Bereich um den Start, dessen Punkte nie gespeichert werden |
| **Vordergrunddienst** | Android-Dienst mit sichtbarer Benachrichtigung, der auch bei ausgeschaltetem Bildschirm weiterläuft |
| **Capacitor** | die Hülle, die `index.html` als Android-App verpackt |
| **Plugin** | Java-Baustein für Dinge, die eine Webseite nicht kann, hier das GPS im Hintergrund |
| **Obtainium** | App, die Apps direkt aus GitHub-Releases installiert und aktualisiert |
| **Low Code** | Software mit wenig, lesbarem Code, die auch ohne Programmierkenntnisse veränderbar bleibt, siehe [[Low-Code-Manifest|Low-Code-Manifest]] |
