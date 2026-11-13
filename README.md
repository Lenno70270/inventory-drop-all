# Inventory Drop All

Eine Fabric-Client-Mod für Minecraft 26.2 und 26.3. Ein Knopf in jedem Inventar-Menü wirft dein ganzes Inventar
samt Hotbar auf einmal. Rüstung und gesperrte Slots bleiben, wo sie sind.

Die Mod muss nur bei dir installiert sein, nicht auf dem Server.

## Bedienen

| Eingabe | Was passiert |
| --- | --- |
| Knopf **Drop All** im Inventar | Wirft alles, was nicht gesperrt ist |
| **Mittlere Maustaste** auf einen Slot | Slot sperren oder entsperren, gesperrte Slots bekommen ein Schloss |
| Eigene Taste (frei belegbar) | Ganzes Inventar werfen, ohne den Knopf zu klicken |
| Eigene Taste (frei belegbar) | Drop-All-Menü öffnen |

Die Tasten stehen unter Optionen, Steuerung, Tastenbelegung in der Kategorie „Drop All“.

## Einstellungen

Im Drop-All-Menü oder über das Zahnrad in [Mod Menu](https://modrinth.com/mod/modmenu):

- Knopf anzeigen und Position (unter dem Inventar oder rechts daneben)
- Was geworfen wird: Hotbar, Inventar, Zweithand, Rüstung (Rüstung ist standardmäßig aus)
- Sicherheitsabfrage für die Taste: erst beim zweiten Drücken wird geworfen

Die Einstellungen liegen in `config/dropallplus.json`, gesperrte Slots in `config/dropallplus-locks.txt`.

Im Kreativ-Inventar wirft Drop All bewusst nichts, weil der Server Kreativ-Drops drosselt und Items sonst
verschwinden würden.

## Voraussetzungen

- Minecraft 26.2 oder 26.3
- Fabric Loader 0.19.3 oder neuer
- [Fabric API](https://modrinth.com/mod/fabric-api)
- Java 25
- Optional: [Mod Menu](https://modrinth.com/mod/modmenu) für das Zahnrad

## Bauen

Mod Menu wird nur zum Kompilieren gebraucht und liegt nicht im Repository. Lade `modmenu-20.0.3.jar` von
[Modrinth](https://modrinth.com/mod/modmenu) und leg sie nach `libs/`. Dann:

```bash
./gradlew build          # für 26.2
./gradlew build -Pmc=26.3
```

Die fertige Datei liegt danach in `build/libs/`.

## Lizenz

MIT, siehe [LICENSE](LICENSE).
