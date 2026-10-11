# Halbmarathon

Eine schlanke Android-App, die dich in 27 Wochen zum Halbmarathon begleitet: Trainingsplan, Lauftagebuch, GPS-Aufzeichnung, Gewohnheiten und Statistik an einem Ort. Sie läuft komplett offline. Alle Daten bleiben auf deinem Gerät.

Das Besondere ist, wie sie gebaut ist: **eine einzige Datei** (`index.html`), ohne Framework, ohne Server, mit einem winzigen Android-Rahmen. Sie lässt sich deshalb auch ohne Programmierkenntnisse mit KI-Assistenten anpassen. Wie das gedacht ist, steht im [[Low-Code-Manifest|Low-Code-Manifest]].

## In Kürze

| | |
|---|---|
| Plan | 27 Wochen, zwei Läufe pro Woche, fünf Phasen vom Wiedereinstieg bis zur Wettkampfwoche |
| Plattform | Android 5.1 oder neuer |
| Daten | nur auf dem Gerät, kein Konto, kein Server, keine Google-Dienste |
| GPS | optional, mit Intervall-Signalen per Vibration, Schutzbereich um den Start und Route als gezeichnete Linie (keine Kartenkacheln) |
| Code | `index.html` (rund 145 KB, gut 2.100 Zeilen), dazu ein Java-Plugin für den GPS-Dienst |
| Design | schwarz-weiß, folgt dem hellen oder dunklen Modus des Geräts |
| Sprache | Deutsch |
| Lizenz | GPL-3.0-or-later |

## Wegweiser

**Die App benutzen**

- [[Installation|Installation]]: Obtainium, APK, Updates
- [[Erste-Schritte|Erste-Schritte]]: die ersten fünf Minuten
- [[Training-und-Plan|Training-und-Plan]]: Wochenrhythmus, Phasen, Einheiten anpassen, Läufe eintragen
- [[GPS-Aufzeichnung|GPS-Aufzeichnung]]: Strecke, Intervalle, Schutzbereich, Route
- [[Gewohnheiten|Gewohnheiten]]
- [[Statistik|Statistik]]: Kennzahlen, Wochenübersicht je Sportart, Learnings
- [[Einstellungen|Einstellungen]]
- [[Backup-und-Daten|Backup-und-Daten]]
- [[Datenschutz|Datenschutz]]
- [[FAQ-und-Glossar|FAQ-und-Glossar]]

**Verstehen, wie es gebaut ist**

- [[Low-Code-Manifest|Low-Code-Manifest]]: die Grundsätze hinter der App
- [[Architektur|Architektur]]: Bausteine, Datenflüsse, Diagramme
- [[Datenmodell|Datenmodell]]: der Datensatz, Speicherschlüssel, Abwärtskompatibilität
- [[Auswertungs-Engine|Auswertungs-Engine]]: Abschnitte lesen, Strecke rechnen, Route bauen
- [[Android-Plugin|Android-Plugin]]: Vordergrunddienst, Bridge, Diagnose-Protokoll
- [[Build-und-Release|Build-und-Release]]: GitHub Actions, Signierung, Prüfungen

**Selbst ändern und mitmachen**

- [[Mit-KI-anpassen|Mit-KI-anpassen]]: Vorlage, Rezepte, Checkliste
- [[Plan-anpassen|Plan-anpassen]]: Wochen, Phasen, Beschreibungen
- [[Mitwirken-und-Testen|Mitwirken-und-Testen]]
- [[Fehlersuche|Fehlersuche]]: wenn etwas nicht klappt, inklusive Diagnose-Protokoll
