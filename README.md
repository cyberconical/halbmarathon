# Halbmarathon

Eine schlanke Android-App, die dich in 27 Wochen zum Halbmarathon begleitet: Trainingsplan, Lauftagebuch, Gewohnheiten und Statistik an einem Ort. Sie läuft komplett offline, alle Daten bleiben auf deinem Gerät.

## Funktionen

**Training**
- Ein Plan über 27 Wochen mit zwei Läufen pro Woche, in fünf Phasen vom Wiedereinstieg bis zur Wettkampfwoche. Er richtet sich nach deinem Wettkampftag.
- Läufe mit Kilometern, Zeit, Gefühl, Schuhen und Tageszeit eintragen.
- Zusatzeinheiten wie Kraft, Bouldern oder Yoga an drei frei wählbaren Wochentagen.
- Mit einem Tipp festhalten, was du heute gemacht hast. Jede Einheit lässt sich anpassen, „Zurück zum Plan“ stellt den Vorschlag wieder her.

**Gewohnheiten**
- Eigene Gewohnheiten anlegen, pro Tag abhaken oder eine Zahl eintragen.
- Serien und Wochendurchschnitt auf einen Blick.

**Statistik**
- Wochenübersicht je Sportart, Kilometer pro Woche und Pace-Verlauf.
- Vergleich nach Schuhtyp und Tageszeit.
- Learnings und Probleme festhalten und als gelöst markieren.

**Einstellungen**
- Wettkampftag und Zielzeit, daraus wird die Zielpace berechnet.
- Eine Körperstelle, die du im Blick behalten willst, zum Beispiel das Knie.
- Rekorde und persönliche Ziele je Sportart.

Die App ist auf Deutsch. Das Design ist schwarz-weiß und folgt dem hellen oder dunklen Modus des Geräts.

## Installation

Die App gibt es nicht in einem Store. Am einfachsten geht es mit [Obtainium](https://github.com/ImranR98/Obtainium), das neue Versionen automatisch erkennt:

1. Obtainium installieren.
2. In Obtainium „App hinzufügen“ tippen und `https://github.com/cyberconical/halbmarathon` einfügen.
3. „Hinzufügen“ und dann „Installieren“ tippen. Android fragt einmalig nach der Erlaubnis, Apps aus dieser Quelle zu installieren.

Alternativ lädst du `halbmarathon.apk` aus dem [neuesten Release](https://github.com/cyberconical/halbmarathon/releases/latest) herunter und installierst sie von Hand.

Voraussetzung: Android 5.1 oder neuer.

## Erste Schritte

1. Oben rechts die Einstellungen öffnen und Wettkampftag und Zielzeit eintragen.
2. Im Tab **Training** deinen ersten Lauf eintragen.
3. Regelmäßig ein Backup speichern (siehe unten).

## Daten und Datenschutz

- Alle Einträge liegen im privaten Speicher der App. Es gibt keinen Server und kein Konto.
- Die App ruft keine Server auf. Sie nutzt keine Google-Dienste, keine Werbung und keine Tracker. Die Schrift liegt in der App.
- Android sichert die App-Daten nicht automatisch in der Cloud. Deine Einträge verlassen das Gerät nur, wenn du selbst ein Backup teilst.
- Beim Deinstallieren oder beim Löschen der App-Daten gehen die Einträge verloren. Updates über Obtainium behalten sie.

### Backup

- **Sichern:** Oben auf „Backup speichern“ tippen und im Teilen-Menü einen Ort wählen, zum Beispiel Drive oder Dateien.
- **Wiederherstellen:** Ganz unten auf „Backup laden“ tippen und die Datei auswählen.

Die Backup-Datei enthält alle deine Einträge. Teile sie nicht über öffentliche Links und lade sie nicht in ein öffentliches Repository hoch.

## Entwicklung

Die App besteht aus einer Datei, `index.html`, ohne Frameworks, und der mitgelieferten Schrift im Ordner `fonts`. Als Android-App wird sie mit [Capacitor](https://capacitorjs.com) verpackt, den Build übernimmt GitHub Actions.

Zum Ausprobieren im Browser:

```
python3 -m http.server
```

Danach `http://localhost:8000` öffnen. Der Browser speichert getrennt von der App, deine echten Daten bleiben unberührt.

Wie du die App anpasst, ein Release veröffentlichst und was das Datenmodell enthält, steht in [docs/entwicklung.md](docs/entwicklung.md).

```
index.html                    die App
fonts/                        Schrift Instrument Sans mit Lizenztext
.github/workflows/build.yml   Build, Signierung und Upload ans Release
docs/entwicklung.md           Anpassen, Datenmodell, Veröffentlichen
LICENSE                       GNU General Public License v3
README.md                     diese Datei
```

## Lizenz

Dieses Projekt steht unter der [GNU General Public License, Version 3](LICENSE) oder, nach deiner Wahl, einer späteren Version (GPL-3.0-or-later). Du darfst die App nutzen, ändern und weitergeben. Änderungen, die du weitergibst, müssen unter derselben Lizenz offen bleiben.

Die Schrift [Instrument Sans](https://github.com/Instrument/instrument-sans) steht unter der SIL Open Font License 1.1, der Lizenztext liegt in `fonts/OFL.txt`.
