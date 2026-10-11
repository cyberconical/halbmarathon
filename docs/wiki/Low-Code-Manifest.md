# Low-Code-Manifest

*Wie diese App gebaut ist und warum. Gedacht für alle, die sie ändern wollen, mit oder ohne Programmierkenntnisse, mit oder ohne KI.*

## Worum es geht

„Low Code“ heißt hier nicht, dass man Kästchen auf einer Oberfläche zusammenklickt. Es heißt: **so wenige bewegliche Teile, dass ein Mensch (oder eine KI) die ganze App auf einmal überblicken und verändern kann.** Die App besteht aus einer Datei, die man lesen kann, und aus Regeln, die dafür sorgen, dass Änderungen nichts kaputt machen.

Das Ziel: Wer eine Idee für die App hat, soll sie in einem Satz beschreiben, eine geänderte Datei zurückbekommen und sie am selben Abend auf dem Handy haben können, ohne dass dabei Daten verloren gehen oder Privates nach außen dringt.

## Die Grundsätze

### 1. Eine Datei, ein Gedanke

Die App ist `index.html`: HTML, CSS und JavaScript in einer Datei, rund 145 KB. Man lädt sie in einen KI-Chat hoch, und sie passt komplett hinein. Wer sie ändern will, muss nichts zusammensuchen, keine Ordnerstruktur lernen und keine Abhängigkeiten verstehen.

*Im Projekt:* `index.html` mit den drei Skriptblöcken `saved-data`, `engine` und `app`, dazu die Schrift im Ordner `fonts/`. Siehe [[Architektur|Architektur]].

### 2. Keine Frameworks, kein Build-Schritt für die Oberfläche

Kein React, kein Bundler, kein Paketmanager für den App-Code. Was in der Datei steht, läuft. Der einzige Build ist der, der die Datei in eine Android-App verpackt, und er verändert den App-Code nicht.

*Im Projekt:* Der Build kopiert `index.html` und `fonts/` unverändert in die Capacitor-Hülle. Siehe [[Build-und-Release|Build-und-Release]].

### 3. Deine Daten gehören dir

Die App arbeitet offline und speichert nur auf dem Gerät. Es gibt keinen Server, kein Konto, keine Google-Dienste, keine Tracker. Weniger Technik heißt weniger Angriffsfläche und weniger, was man erklären muss.

*Im Projekt:* `localStorage`, ein Datensatz, Cloud-Sicherung von Android abgeschaltet, Google-Services im Build entfernt. Siehe [[Datenschutz|Datenschutz]].

### 4. Die App ist ihr eigenes Backup

Das Backup ist keine Exportdatei in einem Spezialformat, sondern **die App selbst mit den Daten darin**. Die Funktion `buildFile()` nimmt eine Kopie der eigenen Seite und trägt den Datensatz in den leeren Block `saved-data` ein. Diese Datei öffnet man im Browser, lädt sie als Backup oder installiert sie als neue Version.

Das ist praktisch und eine Verpflichtung: Der Block `saved-data` bleibt im Original immer `null`, und eine Backup-Datei gehört nie in ein öffentliches Repository.

### 5. Der Plan ist Daten, nicht Code

Was sich häufig ändert, steht als lesbare Liste da: `PHASES` und `PLAN` mit Titeln, Minuten und Beschreibungen. Wer den Plan ändern will, ändert Text, nicht Logik. Eigene Anpassungen der Person liegen **getrennt über dem Plan** (`state.edits`), damit der Plan der Vorschlag bleibt und „Zurück zum Plan“ immer geht.

Sogar die Intervalle fürs GPS stehen nur im Beschreibungstext. Die Engine liest sie von dort. Wer den Text ändert, ändert das Training.

### 6. Rechnen ohne Nebenwirkungen

Alles, was rechnet, steht im Block `engine`: Abschnitte lesen, Strecke aus GPS-Punkten bestimmen, Intervalle auswerten, Route vereinfachen und kodieren. Das sind reine Funktionen ohne Zugriff auf Gerät, Speicher oder Oberfläche. Sie lassen sich getrennt verstehen, testen und ersetzen. Siehe [[Auswertungs-Engine|Auswertungs-Engine]].

### 7. Kompatibilität schlägt Eleganz

Gespeicherte Daten sind heilig. Der Speicherschlüssel `hm2027-state` und die vorhandenen Felder werden **nie umbenannt oder entfernt**. Neue Felder sind optional und haben Standardwerte. Die Funktion `normalize()` liest jeden alten oder halb kaputten Datensatz und macht daraus einen gültigen. Siehe [[Datenmodell|Datenmodell]].

### 8. Natives nur dort, wo das Web nicht reicht

Eine Webseite kann im Hintergrund kein GPS lesen. Dafür gibt es **ein** kleines Java-Plugin, `run-tracker`, das nur den Android-`LocationManager` nutzt. Alles andere bleibt Webseite. Je weniger nativer Code, desto weniger, was ein Browser-Test nicht sieht. Siehe [[Android-Plugin|Android-Plugin]].

### 9. Sparsam mit Daten, auch im Fehlerfall

Rohe GPS-Punkte bleiben im Arbeitsspeicher. Gespeichert wird das Ergebnis, auf Wunsch mit Route, und ohne den Bereich um den Start. Auch Fehlersuche folgt dieser Regel: Das Diagnose-Protokoll enthält nur technische Ereignisse, keine Orte, keine Daten, keine Namen, und die Person sieht, löscht und teilt es selbst.

### 10. Vertrauen durch Prüfbarkeit

Wer nicht selbst alles lesen kann, braucht Wächter, die es für ihn tun. Der Build bricht ab, wenn Google-Dienste auftauchen, wenn die Cloud-Sicherung nicht aus ist oder wenn im fertigen Manifest eine nötige Berechtigung fehlt. Eine Regel, die nur in einem Dokument steht, wird vergessen. Eine Regel, die den Build rot färbt, nicht.

### 11. Eine Änderung pro Anfrage

Kleine Schritte, jeweils testen, vorher ein Backup. Wer einer KI zehn Wünsche auf einmal gibt, bekommt zehn mögliche Fehler auf einmal. Die Vorlage dafür steht unter [[Mit-KI-anpassen|Mit-KI-anpassen]].

## Der Vertrag mit der Person, die ändert

Jede Änderung hält diese Regeln ein, egal ob von Hand oder durch eine KI:

1. **Eine Datei bleibt eine Datei.** Keine Framework-Einführung, keine externen Bibliotheken, keine Netzwerkaufrufe.
2. **Der Speicherschlüssel und alle vorhandenen Datenfelder bleiben.** Neue Felder sind optional.
3. **Die Skript-IDs `saved-data`, `engine` und `app` und die Backup-Funktion bleiben.** `saved-data` ist im Original `null`.
4. **Rechnen gehört in `engine`, Anzeige und Speicher in `app`.**
5. **Keine Orte, Datumsangaben oder Namen in Protokollen.**
6. **Design: schwarz-weiß, minimal, bestehende CSS-Variablen.** Bedienbar mit dem Daumen.
7. **Texte auf Deutsch.**
8. **Änderungen an Berechtigungen oder am Android-Teil werden vom Build geprüft.** Ein Browser-Test sieht sie nicht.

## Was dieser Ansatz kostet

Ehrlich gesagt gibt es Preise, und es lohnt sich, sie zu kennen.

- **Keine Typprüfung.** Ein Tippfehler im Feldnamen fällt erst beim Benutzen auf. Dagegen helfen kleine Schritte, `normalize()` und Tests im Browser.
- **Große Datei, viel Text.** Gut 2.100 Zeilen in einer Datei sind mit Gliederung gut lesbar, aber zu zweit schwer gleichzeitig zu bearbeiten. Merge-Konflikte sind wahrscheinlicher als in vielen kleinen Dateien.
- **Muster statt Struktur.** Die Intervall-Erkennung liest Freitext mit regulären Ausdrücken. Das ist biegsam, aber ein ungewöhnlicher Satz im Plan wird nicht erkannt und ergibt einen freien Lauf. Das ist gewollt als sanfter Rückfall, aber eben eine Grenze.
- **Wenig automatische Tests.** Die Rechenfunktionen sind testbar gebaut, ein vollständiger Test-Satz liegt aber noch nicht im Repository. Siehe [[Mitwirken-und-Testen|Mitwirken-und-Testen]].
- **Das Native ist die Schwachstelle.** Alles, was nur in der Android-Hülle passiert, sieht ein Test im Browser nicht.

## Ein Fall aus der Praxis

Die GPS-Aufzeichnung stürzte beim Start ab, aber nur bei Läufen mit Intervallen. Ursache: Für die pünktlichen Vibrationssignale hält der Dienst die CPU mit einem Wake-Lock wach. Die Berechtigung `WAKE_LOCK` stand nicht im Manifest, und der Aufruf war nicht abgesichert. Android warf eine `SecurityException`, die App wurde beendet, und beim Öffnen war der Lauf weg.

Was daraus folgte, zeigt die Grundsätze in Aktion:

- **Grundsatz 10:** Der Build prüft jetzt das zusammengeführte Manifest auf alle nötigen Berechtigungen. Derselbe Fehler lässt sich nicht noch einmal unbemerkt ausliefern.
- **Grundsatz 8:** Ein Fehler beim Wake-Lock stoppt die Aufzeichnung nicht mehr. Die Signale können sich dann verspäten, der Lauf geht nicht verloren.
- **Grundsatz 9:** Ein anonymes Diagnose-Protokoll zeigt künftig, was passiert ist, auch den Grund, warum Android die App zuletzt beendet hat, ohne Orte oder Namen.
- **Grundsatz 7:** Die App knüpft nach einem Neustart an den laufenden Lauf an, statt ihn still zu verwerfen.

## Wann der Ansatz kippt

Eine einzige Datei ist keine Religion. Es ist Zeit, sie zu zerlegen, wenn

- mehrere Menschen gleichzeitig an verschiedenen Bereichen arbeiten und sich ständig in die Quere kommen,
- die Datei so groß wird, dass sie nicht mehr in einen KI-Chat passt,
- die Rechenlogik so wächst, dass sie ohne automatische Tests nicht mehr sicher zu ändern ist,
- weitere native Funktionen dazukommen, die ein eigenes Test- und Prüfkonzept brauchen.

Der sinnvolle erste Schritt wäre dann, den Block `engine` in eine eigene, getestete Datei auszulagern. Er ist schon so gebaut, dass das ohne Umbau geht. Bis dahin gilt: **Einfach genug, dass man alles auf einmal verstehen kann.**

## Weiter

- [[Architektur|Architektur]]: wie die Teile zusammenpassen
- [[Mit-KI-anpassen|Mit-KI-anpassen]]: die Vorlage für Änderungswünsche
- [[Mitwirken-und-Testen|Mitwirken-und-Testen]]: wie man Änderungen prüft
