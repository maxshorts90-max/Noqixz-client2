package de.ultimateclient.core;

import de.ultimateclient.gui.ClickGuiScreen;
import de.ultimateclient.modules.SprintModule;
import de.ultimateclient.modules.StubModule;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class ModuleManager {
    private static final List<Module> MODULES = new ArrayList<>();
    private static final Map<Integer, Boolean> PREV = new HashMap<>();

    /** Taste, die das ClickGUI oeffnet (einstellbar, wird in der Config gespeichert). */
    public static int guiKey = GLFW.GLFW_KEY_RIGHT_SHIFT;

    private ModuleManager() {}

    public static List<Module> all() {
        return MODULES;
    }

    public static List<Module> of(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : MODULES) if (m.category == c) out.add(m);
        return out;
    }

    public static Module byName(String name) {
        for (Module m : MODULES) if (m.name.equalsIgnoreCase(name)) return m;
        return null;
    }

    /** Anderes Modul mit derselben Taste suchen (fuer die Doppelbelegungs-Erkennung). */
    public static Module findKeyConflict(Module self, int key) {
        for (Module m : MODULES) if (m != self && m.getKey() == key) return m;
        return null;
    }

    public static void init() {
        // --- Echte Module ---
        MODULES.add(new SprintModule());

        // --- Geplante Module (nicht implementiert, ausgegraut) ---
        stub(Category.COMBAT, "Auto Totem", "Haelt automatisch einen Totem in der Offhand. (geplant)");
        stub(Category.COMBAT, "Hitbox", "Zeigt und passt Hitboxen an - nur Singleplayer/eigene Testwelt. (geplant)");
        stub(Category.COMBAT, "Shield Breaker", "Wechselt zur Axt, um Schilde zu brechen. (geplant)");
        stub(Category.COMBAT, "Mace Swap", "Wechselt beim Angriff auf die Mace. (geplant)");
        stub(Category.PLAYER, "Auto Eat", "Isst automatisch, wenn der Hunger niedrig ist. (geplant)");
        stub(Category.PLAYER, "Auto Tool", "Waehlt das beste Werkzeug fuer den Block. (geplant)");
        stub(Category.PLAYER, "Fast Place", "Entfernt die Verzoegerung beim Platzieren. (geplant)");
        stub(Category.PLAYER, "Auto Reconnect", "Verbindet dich nach einem Verbindungsabbruch erneut. (geplant)");
        stub(Category.PLAYER, "Name Protect", "Ersetzt deinen Namen in der Anzeige. (geplant)");
        stub(Category.MOVEMENT, "Elytra Glide", "Hilfen fuer den Elytra-Flug. (geplant)");
        stub(Category.MOVEMENT, "Key Pearl", "Wirft per Tastendruck eine Enderperle. (geplant)");
        stub(Category.WORLD, "Ore Finder", "Zeigt Erze in den geladenen Chunks. (geplant)");
        stub(Category.WORLD, "Biome Finder", "Zeigt Biome in der Umgebung. (geplant)");
        stub(Category.WORLD, "Waypoints", "Verwaltet eigene Wegpunkte. (geplant)");
        stub(Category.BASE_FINDER, "Saved Locations", "Verwaltet und analysiert eigene gespeicherte Standorte. (geplant)");
        stub(Category.ESP, "Player ESP", "Hebt Spieler visuell hervor. (geplant)");
        stub(Category.ESP, "Mob ESP", "Hebt Mobs visuell hervor. (geplant)");
        stub(Category.ESP, "Item ESP", "Hebt fallengelassene Items hervor. (geplant)");
        stub(Category.STORAGE, "Storage ESP", "Hebt ausgewaehlte Container visuell hervor. (geplant)");
        stub(Category.RENDER, "Fullbright", "Verbessert die Sichtbarkeit in dunklen Bereichen. (geplant)");
        stub(Category.RENDER, "Zoom", "Zoomt per Taste in die Ferne. (geplant)");
        stub(Category.RENDER, "Freecam", "Freie Kamera ausserhalb des Spielers. (geplant)");
        stub(Category.VISUALS, "Custom Crosshair", "Eigenes Fadenkreuz. (geplant)");
        stub(Category.VISUALS, "Fog Customization", "Passt den Nebel an. (geplant)");
        stub(Category.INVENTORY, "Item Search", "Durchsucht Inventar und Container. (geplant)");
        stub(Category.INVENTORY, "Item Durability", "Zeigt Haltbarkeit von Items. (geplant)");
        stub(Category.UTILITY, "Calculator", "Einfacher Taschenrechner. (geplant)");
        stub(Category.UTILITY, "Stopwatch", "Stoppuhr im Spiel. (geplant)");
        stub(Category.AUTOMATION, "Chat Macro", "Sendet gespeicherte Chat-Nachrichten per Taste. (geplant)");
        stub(Category.HUD, "FPS", "Zeigt die aktuellen FPS. (geplant)");
        stub(Category.HUD, "Coordinates", "Zeigt deine Koordinaten. (geplant)");
        stub(Category.HUD, "Keystrokes", "Zeigt gedrueckte Tasten. (geplant)");
        stub(Category.HUD, "Armor HUD", "Zeigt deine Ruestung. (geplant)");
        stub(Category.MISC, "Discord Presence", "Zeigt den Spielstatus auf Discord. (geplant)");
        stub(Category.MISC, "Spotify HUD", "Zeigt den aktuellen Titel. (geplant)");
    }

    private static void stub(Category c, String name, String desc) {
        MODULES.add(new StubModule(c, name, desc));
    }

    /** Wird jeden Client-Tick aufgerufen: Keybinds abfragen und aktive Module ausfuehren. */
    public static void tick(Minecraft mc) {
        if (mc.getWindow() == null) return;
        long handle = mc.getWindow().handle();

        boolean idle = mc.screen == null && mc.player != null;
        if (edge(handle, guiKey) && idle) {
            mc.setScreen(new ClickGuiScreen());
        }
        for (Module m : MODULES) {
            if (m.getKey() >= 0 && edge(handle, m.getKey()) && idle && m.isImplemented()) {
                m.toggle();
                ConfigManager.save();
            }
        }
        if (mc.player != null) {
            for (Module m : MODULES) {
                if (m.isEnabled()) m.onTick(mc);
            }
        }
    }

    /** true genau in dem Tick, in dem die Taste neu gedrueckt wurde. */
    private static boolean edge(long handle, int key) {
        boolean now = GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
        boolean was = PREV.getOrDefault(key, false);
        PREV.put(key, now);
        return now && !was;
    }

    public static String keyName(int key) {
        if (key < 0) return "Keine";
        if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F12) return "F" + (key - GLFW.GLFW_KEY_F1 + 1);
        switch (key) {
            case GLFW.GLFW_KEY_LEFT_SHIFT: return "L-Shift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT: return "R-Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL: return "L-Strg";
            case GLFW.GLFW_KEY_RIGHT_CONTROL: return "R-Strg";
            case GLFW.GLFW_KEY_LEFT_ALT: return "L-Alt";
            case GLFW.GLFW_KEY_RIGHT_ALT: return "R-Alt";
            case GLFW.GLFW_KEY_SPACE: return "Leertaste";
            case GLFW.GLFW_KEY_TAB: return "Tab";
            case GLFW.GLFW_KEY_ENTER: return "Enter";
            default:
                String n = GLFW.glfwGetKeyName(key, 0);
                return n != null ? n.toUpperCase() : "Taste " + key;
        }
    }
}
