# Ultimate Client (Phase 1 + 2)

Fabric-Client fuer Minecraft Java **1.21.11** mit Mojang-Mappings, Java 21.

## Status: NICHT KOMPILIERT / NICHT GETESTET

Der Code wurde ohne Netzwerk und ohne Minecraft geschrieben. Er ist weder gebaut noch im Spiel
ausprobiert worden. Rechne beim ersten Build mit Compilerfehlern, vor allem in:

- `gui/ClickGuiScreen.java`: Eingabe-Signaturen (`MouseButtonEvent`, `KeyEvent`, `CharacterEvent`)
  und `GuiGraphics.drawString/fill` haben sich in 1.21.9+ geaendert.
- `core/ModuleManager.java`: `mc.getWindow().handle()` (Fenster-Handle).
- `build.gradle` / `gradle.properties`: Versionen stammen aus einer Sekundaerquelle und muessen
  gegen https://fabricmc.net/develop geprueft werden (Loader, Fabric API, Loom, Gradle-Version).

## Installation / Build

1. JDK 21 installieren.
2. Am einfachsten: auf https://fabricmc.net/develop das Template fuer 1.21.11 (Mojang-Mappings)
   erzeugen und den Ordner `src/` aus diesem Projekt hineinkopieren; dann stimmen Wrapper und Versionen.
   Alternativ Gradle installieren und im Projektordner `gradle wrapper` ausfuehren.
3. `./gradlew build` ; die JAR liegt danach in `build/libs/`.
4. JAR zusammen mit der passenden Fabric API in den `mods`-Ordner legen.

## Bedienung

- **Rechte Shift-Taste** oeffnet/schliesst das ClickGUI (aenderbar im CONFIGS-Fenster).
- **Linksklick** auf ein Modul: an/aus. **Rechtsklick**: Einstellungsfenster (inkl. Keybind).
- Tippen im GUI filtert Module nach Name, Beschreibung, Kategorie oder Taste.
- Maus ueber ein Modul: Tooltip mit Beschreibung. Mausrad: scrollen.
- **CONFIGS** (unten): Profil speichern, neu anlegen, laden, kopieren, loeschen.
- Configs liegen in `.minecraft/config/ultimateclient/*.json`.

## Module

| Modul | Status |
|---|---|
| Sprint (Movement) | implementiert |
| alle anderen in der GUI | **nicht implementiert**, ausgegraut, nicht aktivierbar |

## Noch offen (Phase 3 und folgende)

Universal Search ueber Bloecke/Items/Entities mit Texturen und 3D-Vorschau, Block-/Item-Datenbank,
HUD-System (verschiebbar/skalierbar), ESP- und Render-Module, Minimap/Weltkarte/Waypoints,
Inventar-Module, Benachrichtigungen als HUD-Overlay (aktuell nur Statuszeile im GUI),
Config-Import/Export/Umbenennen, Logo, Pixel-Icons, Animationen, Discord/Spotify.
Alle voreingestellten Profile (default, pvp, cpvp, survival, building, utility, custom) sind
anfangs identisch.
