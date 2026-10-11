# Mitwirken und Testen

## Grundregeln

Jede Änderung hält den Vertrag aus dem [[Low-Code-Manifest|Low-Code-Manifest]] ein: eine Datei, keine Frameworks, keine Netzwerkaufrufe, Daten kompatibel, Rechnen in `engine`, keine persönlichen Daten in Protokollen, Texte auf Deutsch.

## Ablauf

1. Einen Branch anlegen.
2. Ändern und lokal testen (unten).
3. Pull Request gegen `main` öffnen. Beschreiben: Problem, Ursache, Änderung, wie getestet, was bewusst nicht geändert wurde.
4. Den Build manuell auf dem Branch starten (Actions, „APK bauen“, „Run workflow“, Branch wählen). Er prüft auch den Android-Teil.
5. Die APK aus dem Lauf auf einem Gerät testen, wenn der Android-Teil oder die GPS-Aufzeichnung betroffen ist.
6. Mergen und ein Release anlegen, siehe [[Build-und-Release|Build-und-Release]].

## Lokal testen

### Im Browser

```sh
python3 -m http.server
# dann http://localhost:8000 öffnen
```

Alle Tabs, die Einstellungen und ein Eintrag pro Bereich. Die Entwicklerkonsole des Browsers sollte keine Fehler zeigen. Im Browser läuft die GPS-Aufzeichnung über den Standort des Browsers, aber nur bei geöffnetem Bildschirm, und das Diagnose-Protokoll gibt es nicht.

### Syntax prüfen

```sh
python3 - <<'PY'
import re
h=open("index.html",encoding="utf8").read()
for i,(a,b) in enumerate(re.findall(r'<script([^>]*)>(.*?)</script>',h,flags=re.S)):
    if "json" not in a and b.strip(): open(f"/tmp/blk{i}.js","w").write(b)
PY
for f in /tmp/blk*.js; do node --check "$f" && echo "$f ok"; done
```

### Die Engine testen

Die Funktionen in `HMTrk` sind rein und lassen sich in Node laden, siehe [[Auswertungs-Engine|Auswertungs-Engine]], „Testen in Node“. Gute Testfälle:

- jede Beschreibung im `PLAN` durch `parseSegments` schicken und prüfen, dass Intervall-Einheiten erkannt werden (derzeit 18 der 54 Läufe ergeben Abschnitte). Die Summe der Abschnitte muss nicht genau die geplanten Minuten treffen: Diese sind gerundet und enthalten etwas Puffer, bei manchen Einheiten liegt die Summe 4 bis 7 Minuten darunter,
- `analyze` mit einer erfundenen geraden Strecke und konstanter Geschwindigkeit, mit Pausen, Ausreißern und schlechter Genauigkeit,
- `buildRoute` mit festem `rand` und prüfen, dass kein Punkt im Schutzbereich bleibt,
- `encode` und `decode` hin und zurück.

### Die App mit gespieltem Plugin testen

Mit [jsdom](https://github.com/jsdom/jsdom) lässt sich `index.html` in Node laden und ein Plugin vortäuschen, indem man vor dem Laden `window.Capacitor = { isNativePlatform: () => true, Plugins: { RunTracker: mock } }` setzt. So wurde zum Beispiel das Wieder-Anknüpfen nach einem Neustart geprüft: Dienst läuft mit und ohne gemerkten Lauf, Dienst weg, Fehler beim Nachfragen, unbekannte Einheit, Pause. `window.scrollTo` ist in jsdom nicht vorhanden, die Meldung dazu ist harmlos.

### Auf dem Gerät

Was nur im Android-Teil passiert, sieht kein Browser-Test: Berechtigungen, Vordergrunddienst, Wake-Lock, Verhalten im Hintergrund. Dafür die APK aus dem Build installieren und

1. einen freien Lauf und einen Lauf mit Intervallen starten,
2. den Bildschirm ausschalten, auf die Signale achten,
3. die App aus der Übersicht wischen und wieder öffnen: Der Lauf muss sofort wieder da sein,
4. unter Einstellungen, Diagnose das Protokoll ansehen.

## Das Wiki bearbeiten

Die Wiki-Seiten liegen unter `docs/wiki/` im Repository. Ändern geht wie bei jeder anderen Datei per Pull Request. Nach dem Merge in `main` veröffentlicht der Workflow `wiki.yml` die Seiten ins GitHub-Wiki. Direkte Änderungen im Wiki werden dabei überschrieben.

- Dateiname = Seitenname, `-` wird zu Leerzeichen.
- Interne Links mit doppelten eckigen Klammern, erst der Anzeigetext, dann nach einem senkrechten Strich der Seitenname, zum Beispiel `[[Datenschutz|Datenschutz]]`.
- `_Sidebar.md` ist die Seitenleiste, `_Footer.md` die Fußzeile.
- Diagramme als Mermaid-Codeblöcke.

Damit der Workflow das Wiki beschreiben kann, muss es einmalig existieren: Im Reiter „Wiki“ die erste Seite anlegen, Inhalt egal.
