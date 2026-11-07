package de.ultimateclient.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.ultimateclient.UltimateClient;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;

/** Profile als JSON-Dateien in .minecraft/config/ultimateclient/. */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String[] PRESETS = {"default", "pvp", "cpvp", "survival", "building", "utility", "custom"};

    private static Path dir;
    private static String active = "default";

    private ConfigManager() {}

    public static void init() {
        dir = FabricLoader.getInstance().getConfigDir().resolve("ultimateclient");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            UltimateClient.LOG.error("Config-Ordner konnte nicht erstellt werden", e);
            return;
        }
        // Voreingestellte Profile mit dem aktuellen (Standard-)Zustand anlegen, falls sie fehlen.
        for (String p : PRESETS) {
            if (!Files.exists(file(p))) write(file(p), snapshot());
        }
        Path a = dir.resolve("active.txt");
        try {
            if (Files.exists(a)) active = Files.readString(a, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            UltimateClient.LOG.warn("active.txt nicht lesbar", e);
        }
        if (!Files.exists(file(active))) active = "default";
        load(active);
    }

    public static String active() {
        return active;
    }

    public static List<String> profiles() {
        List<String> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir)) {
            s.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".json"))
                    .forEach(n -> out.add(n.substring(0, n.length() - 5)));
        } catch (IOException e) {
            UltimateClient.LOG.warn("Profile nicht lesbar", e);
        }
        Collections.sort(out);
        return out;
    }

    public static void save() {
        if (dir == null) return;
        write(file(active), snapshot());
        writeActive();
    }

    public static boolean load(String name) {
        Path f = file(name);
        if (!Files.exists(f)) return false;
        try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            apply(JsonParser.parseReader(r).getAsJsonObject());
            active = name;
            writeActive();
            return true;
        } catch (Exception e) {
            UltimateClient.LOG.error("Profil '{}' konnte nicht geladen werden", name, e);
            return false;
        }
    }

    public static boolean create(String name) {
        if (Files.exists(file(name))) return false;
        write(file(name), snapshot());
        return true;
    }

    public static boolean copy(String from, String to) {
        try {
            if (!Files.exists(file(from)) || Files.exists(file(to))) return false;
            Files.copy(file(from), file(to));
            return true;
        } catch (IOException e) {
            UltimateClient.LOG.error("Kopieren fehlgeschlagen", e);
            return false;
        }
    }

    public static boolean delete(String name) {
        if (name.equals(active) || profiles().size() <= 1) return false;
        try {
            return Files.deleteIfExists(file(name));
        } catch (IOException e) {
            UltimateClient.LOG.error("Loeschen fehlgeschlagen", e);
            return false;
        }
    }

    // ---------------------------------------------------------------- intern

    private static Path file(String name) {
        return dir.resolve(name.replaceAll("[^A-Za-z0-9 _-]", "_") + ".json");
    }

    private static void writeActive() {
        try {
            Files.writeString(dir.resolve("active.txt"), active, StandardCharsets.UTF_8);
        } catch (IOException e) {
            UltimateClient.LOG.warn("active.txt nicht schreibbar", e);
        }
    }

    private static void write(Path f, JsonObject o) {
        try {
            Files.writeString(f, GSON.toJson(o), StandardCharsets.UTF_8);
        } catch (IOException e) {
            UltimateClient.LOG.error("Config konnte nicht gespeichert werden", e);
        }
    }

    private static JsonObject snapshot() {
        JsonObject root = new JsonObject();
        root.addProperty("guiKey", ModuleManager.guiKey);
        JsonObject mods = new JsonObject();
        for (Module m : ModuleManager.all()) {
            if (!m.isImplemented()) continue;
            JsonObject mo = new JsonObject();
            mo.addProperty("enabled", m.isEnabled());
            mo.addProperty("key", m.getKey());
            JsonObject so = new JsonObject();
            for (Setting<?> s : m.settings) so.add(s.name, s.toJson());
            mo.add("settings", so);
            mods.add(m.name, mo);
        }
        root.add("modules", mods);
        return root;
    }

    private static void apply(JsonObject root) {
        if (root.has("guiKey")) ModuleManager.guiKey = root.get("guiKey").getAsInt();
        JsonObject mods = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
        for (Module m : ModuleManager.all()) {
            if (!m.isImplemented() || !mods.has(m.name)) continue;
            JsonObject mo = mods.getAsJsonObject(m.name);
            if (mo.has("key")) m.setKey(mo.get("key").getAsInt());
            if (mo.has("settings")) {
                JsonObject so = mo.getAsJsonObject("settings");
                for (Setting<?> s : m.settings) {
                    JsonElement e = so.get(s.name);
                    if (e != null) {
                        try {
                            s.fromJson(e);
                        } catch (Exception ex) {
                            UltimateClient.LOG.warn("Ungueltiger Wert fuer {}.{}", m.name, s.name);
                        }
                    }
                }
            }
            if (mo.has("enabled")) m.setEnabled(mo.get("enabled").getAsBoolean());
        }
    }
}
