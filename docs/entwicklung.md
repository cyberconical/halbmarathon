# Entwicklung und Anpassung

Diese Seite ergänzt die [README](../README.md). Sie richtet sich an alle, die die App anpassen, bauen oder veröffentlichen wollen, mit oder ohne Programmierkenntnisse.

## Plan anpassen

Die wichtigsten Werte stellst du direkt in der App ein, unter Einstellungen (Symbol oben rechts):

- Wettkampftag und Zielzeit
- Beobachtete Stelle (zum Beispiel Knie) oder keine
- Zusatzeinheiten am Montag, Dienstag und Freitag
- Rekorde, frühere Rekorde und Ziele

Die Struktur des Plans selbst steckt in `index.html`: `PLAN` enthält pro Woche einen Titel sowie den Mittwochs- und den Sonntagslauf, `PHASES` beschreibt die Trainingsphasen. Mit KI geht das ohne Code-Kenntnisse, siehe [Herunterladen und mit KI anpassen](#herunterladen-und-mit-ki-anpassen). Nach einer Änderung ein neues Release veröffentlichen.

Neue Installationen starten mit sinnvollen Standardwerten: nur Krafttraining am Montag, keine beobachtete Stelle und ein Wettkampftag 27 Wochen nach dem nächsten Montag.

## Herunterladen und mit KI anpassen

Die ganze App besteht aus einer einzigen Datei, `index.html`. Deshalb lässt sie sich gut mit KI-Assistenten wie Claude, ChatGPT oder Gemini ändern, auch ohne Programmierkenntnisse und auch am Handy.

### 1. Herunterladen

- **Nur die App:** In diesem Repository auf `index.html` tippen und oben rechts auf „Download raw file“ gehen.
- **Das ganze Projekt:** Auf „Code“ und dann „Download ZIP“ tippen. Mit Git: `git clone https://github.com/cyberconical/halbmarathon.git`

### 2. Änderung beschreiben

1. Die Datei `index.html` in einen KI-Chat hochladen. Mit rund 65 KB passt sie in aktuelle Modelle.
2. Mit der folgenden Vorlage eine Änderung pro Anfrage beschreiben.
3. Die vollständige neue Datei zurückverlangen, keine Ausschnitte.

```
Du bekommst die Datei index.html meiner Trainings-App. Sie ist eine einzelne
HTML-Datei (HTML, CSS, JavaScript, keine Frameworks) und läuft als Android-App
über Capacitor.

Ändere Folgendes: <dein Wunsch>

Regeln:
- Gib die vollständige, geänderte index.html zurück, nicht nur Ausschnitte.
- Gespeicherte Daten müssen kompatibel bleiben. Der Speicherschlüssel
  "hm2027-state" und die vorhandenen Felder (logs, strength, alt, habits,
  habitLog, learnings, profile, records, goals, workouts, dur, edits, updatedAt) dürfen nicht umbenannt oder entfernt
  werden. Neue Felder sind optional und brauchen Standardwerte.
- Die Skript-IDs "saved-data" und "app" und die Backup-Funktion bleiben
  erhalten. Das Skript "saved-data" bleibt im Original leer (null).
- Design: schwarz-weiß, minimal, bestehende CSS-Variablen nutzen. Gut
  bedienbar mit dem Daumen auf einem Handy.
- Keine externen Bibliotheken und keine Netzwerkaufrufe, außer der
  bestehenden Schrift.
- Texte auf Deutsch.
```

Beispiele für Wünsche: eine weitere Auswahl beim Lauf (zum Beispiel Wetter), ein neues Diagramm, ein anderer Wochenplan, ein neuer Tab.

### 3. Überblick für KI und Mensch

| Bereich in `index.html` | Inhalt |
|---|---|
| `<style>` | Design, Farben als CSS-Variablen ganz oben |
| HTML | Kopfleiste, drei Tabs (Training, Gewohnheiten, Statistik) und die Einstellungen |
| Skript `app`, oben | Plan (`PLAN`, `PHASES`), Profil-Auswertung (`applyProfile`, daraus entstehen `START`, `RACE`, `SESSIONS`) |
| Skript `app`, Mitte | Zustand (`state`), Speichern, Anzeige-Funktionen (`renderHero`, `renderWeek`, `renderPlan`, `renderHabits`, `renderStats`, `renderOverview`, `renderLearnings`, `renderSettings`) |
| Skript `app`, unten | Klick-Ereignisse, Backup speichern und laden |

Alle Einträge stecken in einem Datensatz:

```
state = {
  logs:       { "w3-wed": { km, sec, feel, knee, shoe, tod, ts } },
  strength:   { "w3-mon": true },
  alt:        { "w3-fri": { what, ts } },
  habits:     [ { id, name, type: "check" | "number", unit, created } ],
  habitLog:   { <habitId>: { "2026-10-04": "x" | "o" | 7.5 } },
  learnings:  [ { id, date, ts, text, type: "insight" | "issue", tag, solved } ],
  profile:    { raceDate: "2027-04-04", goalSec: 7200, problem: "Knie",
                mon: "Kraft", tue: "Bouldern", fri: "Bouldern" },   // "Aus" = frei
  records:    [ { id, sport: "lauf"|"boulder"|"stretch"|"cali", key, name, value, date, former, ts } ],
  goals:      [ { id, sport: "allg"|"lauf"|..., text, due, done, ts } ],
  workouts:   [ { id, sport: "boulder"|"kraft"|"cali"|"stretch"|"other", date, min, note, ts } ],
  dur:        { "w3-tue": 75 },   // optionale Dauer in Minuten für Plan-Einheiten
  edits:      { "w3-wed": { t, d, min, off, label, skip } },   // eigene Anpassungen am Plan, nur geänderte Felder
  updatedAt:  <Zeitstempel>
}
```

Die Schlüssel von `logs`, `strength`, `alt`, `dur` und `edits` sind Einheiten-IDs: `w` plus Wochennummer, dann `-mon`, `-tue`, `-wed`, `-fri` oder `-sun`.

`edits` liegt über dem Plan und ändert ihn nicht: `t` Titel, `d` Beschreibung, `min` geplante Minuten (nur Läufe), `off` Wochentag als Abstand zum Montag (0 bis 6), `label` andere Art (nur Zusatzeinheiten), `skip` aus dem Plan entfernt. Nur geänderte Felder werden gespeichert, damit zum Beispiel die Zielpace in Beschreibungen weiter mitwandert.

### 4. Testen, bevor du veröffentlichst

1. Die geänderte Datei im Browser öffnen, am PC per Doppelklick, am Handy über die Dateien-App in Chrome.
2. Alle drei Tabs durchklicken und einen Lauf, eine Gewohnheit und ein Learning eintragen. Der Browser speichert getrennt von der App, deine echten Daten sind also nicht betroffen.
3. Wurde ein Datenfeld geändert, vorher in der App ein Backup sichern und nach dem Update prüfen, ob alles noch da ist.

### 5. Veröffentlichen

Die neue `index.html` im Repository hochladen („Add file“ → „Upload files“, gleicher Dateiname ersetzt die alte) und danach ein neues Release anlegen, siehe unten.

> **Wichtig:** Lade niemals eine Datei hoch, die aus „Backup speichern“ stammt. Sie enthält deine persönlichen Einträge und würde sie öffentlich machen. Prüfe vor dem Hochladen, dass in der Datei die Zeile `<script id="saved-data" type="application/json">null</script>` mit `null` endet.

### Mit Coding-Agenten (Claude Code, Codex, Cursor und ähnliche)

1. Repository klonen und im Projektordner den Agenten starten.
2. Die Regeln aus der Vorlage oben in eine Datei `CLAUDE.md` oder `AGENTS.md` legen, damit der Agent sie bei jeder Aufgabe kennt.
3. Den Wunsch beschreiben. Lokal testen, zum Beispiel mit `python3 -m http.server` und dem Aufruf von `http://localhost:8000` im Browser.
4. Committen, pushen und ein Release anlegen.

## Neue Version veröffentlichen

1. `index.html` ändern und committen.
2. Unter „Releases“ ein neues Release anlegen, mit neuem Tag wie `v1.1.0`.
3. GitHub Actions baut die APK, signiert sie und hängt sie nach etwa 5 bis 8 Minuten an das Release an.

Der Build lässt sich auch manuell über den Reiter „Actions“ starten. Die APK steht dann dort als Download zur Verfügung.

## Technik

| Teil | Umsetzung |
|---|---|
| App | Eine einzige Datei, `index.html`, mit HTML, CSS und JavaScript ohne Frameworks |
| Speicher | `localStorage` im WebView, ein JSON-Datensatz |
| Android-Hülle | [Capacitor](https://capacitorjs.com) 6, Plugins für Dateien und Teilen |
| Build | GitHub Actions (`.github/workflows/build.yml`), Node 20, Java 17 |
| Versionsnummer | `versionCode` entspricht der laufenden Build-Nummer |

Der Build erzeugt das Android-Projekt bei jedem Lauf neu. Im Repository liegen deshalb nur:

```
index.html                    die App
.github/workflows/build.yml   Bauanleitung, Signierung, Upload ans Release
docs/entwicklung.md           diese Datei
README.md                     Überblick für Nutzer
```

## Signatur der APK

Der Build signiert die APK mit einem festen Schlüssel. Er steht nicht im Code, sondern als Repository-Secret bei GitHub (`KEYSTORE_BASE64` und `KEYSTORE_PASSWORD`, unter Settings, Secrets and variables, Actions).

- Alle Updates müssen mit demselben Schlüssel signiert sein. Mit einem anderen Schlüssel lässt sich eine installierte App nicht mehr aktualisieren. Sie müsste erst deinstalliert werden, dabei gehen die Einträge verloren, also vorher ein Backup sichern.
- Bewahre Schlüssel und Passwort zusätzlich an einem Ort außerhalb von GitHub auf. Gehen sie verloren, gibt es keine Updates mehr für bestehende Installationen.
- Ein Schlüssel oder Passwort darf nie im Repository stehen, auch nicht in älteren Versionen von Dateien.
- Installiere APKs nur aus deinem eigenen, bekannten Repository.
