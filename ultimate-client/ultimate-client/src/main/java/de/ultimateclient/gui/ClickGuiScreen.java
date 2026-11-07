package de.ultimateclient.gui;

import de.ultimateclient.UltimateClient;
import de.ultimateclient.core.Category;
import de.ultimateclient.core.ConfigManager;
import de.ultimateclient.core.Module;
import de.ultimateclient.core.ModuleManager;
import de.ultimateclient.core.Setting;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * ClickGUI: Linksklick = Modul an/aus, Rechtsklick = Einstellungsfenster.
 * Hinweis: Die Input-Signaturen (MouseButtonEvent, KeyEvent, CharacterEvent) gelten ab 1.21.9;
 * falls der Compiler hier meckert, zuerst diese Methoden gegen die Mojang-Mappings pruefen.
 */
public class ClickGuiScreen extends Screen {
    private static final int ACCENT = 0xFF00E5FF;
    private static final int ACCENT2 = 0xFF1E88E5;
    private static final int BG = 0xCC1B1D21;
    private static final int HEAD = 0xE6101215;
    private static final int TEXT = 0xFFE0E0E0;
    private static final int DIM = 0xFF6A6F76;
    private static final int OFF = 0xFF3A3E44;
    private static final int[] PALETTE = {
        0xFF00E5FF, 0xFF1E88E5, 0xFFFFFFFF, 0xFFFF5252, 0xFF69F0AE, 0xFFFFD740, 0xFFE040FB, 0xFFFF9100
    };
    private static final int PW = 116, RH = 14, HH = 16, GAP = 6, TOP = 22, BOTTOM = 24;

    private interface Click { void on(int button, int mouseX); }

    private record Hit(int x, int y, int w, int h, Module module, Click click) {
        boolean contains(int mx, int my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private record Panel(String title, List<Module> mods) {}

    private final List<Hit> hits = new ArrayList<>();
    private final List<Hit> winHits = new ArrayList<>();
    private double scroll, maxScroll;
    private String query = "";
    private Module settingsFor;
    private Module listening;
    private boolean listeningGui;
    private boolean configsOpen;
    private Setting.Num dragging;
    private int dragX, dragW;
    private int[] winRect = new int[4];
    private Module hoverMod;
    private String status = "";
    private long statusUntil;

    public ClickGuiScreen() {
        super(Component.literal(UltimateClient.NAME));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------ Rendering

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        hits.clear();
        winHits.clear();
        hoverMod = null;
        boolean modal = settingsFor != null || configsOpen;

        g.fill(0, 0, width, height, 0xD8000000);

        // Panels in Spalten, bei Platzmangel in mehreren Reihen
        List<Panel> panels = buildPanels();
        int cols = Math.max(1, (width - GAP) / (PW + GAP));
        int totalW = cols * (PW + GAP) - GAP;
        int x0 = (width - totalW) / 2;
        int y = TOP + 4 - (int) scroll;
        for (int i = 0; i < panels.size(); i += cols) {
            int rowH = 0;
            for (int j = i; j < Math.min(i + cols, panels.size()); j++) {
                rowH = Math.max(rowH, drawPanel(g, panels.get(j), x0 + (j - i) * (PW + GAP), y, mx, my, modal));
            }
            y += rowH + GAP;
        }
        int content = y + (int) scroll - (TOP + 4);
        maxScroll = Math.max(0, content - (height - TOP - BOTTOM - 8));
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        // Leisten oben/unten decken gescrollte Panels ab
        g.fill(0, 0, width, TOP, 0xFF000000);
        g.fill(0, height - BOTTOM, width, height, 0xFF000000);
        g.fill(0, TOP - 1, width, TOP, ACCENT2);
        g.fill(0, height - BOTTOM, width, height - BOTTOM + 1, ACCENT2);

        // Suchfeld
        int sx = width / 2 - 90;
        g.fill(sx, 4, sx + 180, 18, 0xFF15171A);
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        String shown = query.isEmpty() ? "Suche..." : query + (blink ? "_" : "");
        g.drawString(font, shown, sx + 5, 7, query.isEmpty() ? DIM : TEXT, false);
        g.drawString(font, UltimateClient.NAME, 6, 7, ACCENT, false);

        // Statuszeile + Profil
        if (System.currentTimeMillis() < statusUntil) {
            g.drawString(font, status, width - font.width(status) - 6, 7, 0xFFFFD740, false);
        } else {
            String p = "Profil: " + ConfigManager.active();
            g.drawString(font, p, width - font.width(p) - 6, 7, DIM, false);
        }

        // CONFIGS-Button
        int bx = width / 2 - 32, by = height - BOTTOM + 5;
        boolean hovCfg = !modal && mx >= bx && mx < bx + 64 && my >= by && my < by + 14;
        g.fill(bx, by, bx + 64, by + 14, hovCfg ? 0xFF2A2E33 : 0xFF15171A);
        g.fill(bx, by + 13, bx + 64, by + 14, ACCENT);
        g.drawString(font, "CONFIGS", bx + 32 - font.width("CONFIGS") / 2, by + 3, TEXT, false);
        hits.add(new Hit(bx, by, 64, 14, null, (b, m) -> { configsOpen = true; settingsFor = null; }));

        if (settingsFor != null) drawSettingsWindow(g, mx, my);
        if (configsOpen) drawConfigsWindow(g, mx, my);

        // Tooltip
        if (!modal && hoverMod != null) {
            String tip = hoverMod.description + (hoverMod.isImplemented() ? "" : "  [nicht implementiert]");
            int tw = font.width(tip) + 8;
            int tx = Math.min(mx + 8, width - tw - 2), ty = my + 10;
            g.fill(tx, ty, tx + tw, ty + 14, 0xF0101215);
            g.fill(tx, ty, tx + tw, ty + 1, ACCENT);
            g.drawString(font, tip, tx + 4, ty + 3, TEXT, false);
        }
    }

    private List<Panel> buildPanels() {
        List<Panel> out = new ArrayList<>();
        String q = query.toLowerCase(Locale.ROOT).trim();
        if (q.isEmpty()) {
            for (Category c : Category.values()) out.add(new Panel(c.label, ModuleManager.of(c)));
            return out;
        }
        List<Module> found = new ArrayList<>();
        for (Module m : ModuleManager.all()) {
            if (m.name.toLowerCase(Locale.ROOT).contains(q)
                    || m.description.toLowerCase(Locale.ROOT).contains(q)
                    || m.category.label.toLowerCase(Locale.ROOT).contains(q)
                    || ModuleManager.keyName(m.getKey()).toLowerCase(Locale.ROOT).equals(q)) {
                found.add(m);
            }
        }
        out.add(new Panel("Suche (" + found.size() + ")", found));
        return out;
    }

    private int drawPanel(GuiGraphics g, Panel p, int x, int y, int mx, int my, boolean modal) {
        int h = HH + Math.max(1, p.mods().size()) * RH + 2;
        g.fill(x, y, x + PW, y + h, BG);
        g.fill(x, y, x + PW, y + HH, HEAD);
        g.fill(x, y + HH - 1, x + PW, y + HH, ACCENT);
        g.drawString(font, p.title(), x + 6, y + 4, ACCENT, false);
        int ry = y + HH;
        if (p.mods().isEmpty()) g.drawString(font, "(leer)", x + 6, ry + 3, DIM, false);
        for (Module m : p.mods()) {
            boolean visible = ry + RH > TOP && ry < height - BOTTOM;
            boolean hov = !modal && visible && mx >= x && mx < x + PW && my >= ry && my < ry + RH
                    && my > TOP && my < height - BOTTOM;
            if (hov) {
                g.fill(x, ry, x + PW, ry + RH, 0x30FFFFFF);
                hoverMod = m;
            }
            int col = !m.isImplemented() ? DIM : (m.isEnabled() ? ACCENT : TEXT);
            g.drawString(font, m.name, x + 6, ry + 3, col, false);
            boolean on = m.isEnabled() && m.isImplemented();
            pill(g, x + PW - 18, ry + 4, 12, 6, on ? ACCENT2 : OFF);
            pill(g, x + PW - (on ? 12 : 17), ry + 5, 4, 4, on ? 0xFFFFFFFF : DIM);
            if (visible) hits.add(new Hit(x, ry, PW, RH, m, null));
            ry += RH;
        }
        return h;
    }

    private void drawSettingsWindow(GuiGraphics g, int mx, int my) {
        Module m = settingsFor;
        int w = 230, rowH = 20;
        int rows = Math.max(1, m.settings.size()) + 1;
        int h = HH + rows * rowH + 8;
        int x = (width - w) / 2, y = Math.max(TOP + 4, (height - h) / 2);
        winRect = new int[] {x, y, w, h};
        g.fill(x, y, x + w, y + h, 0xF0181A1E);
        g.fill(x, y, x + w, y + HH, HEAD);
        g.fill(x, y + HH - 1, x + w, y + HH, ACCENT);
        g.drawString(font, m.name + " Settings", x + 8, y + 4, ACCENT, false);
        int cy = y + HH + 4;

        if (m.settings.isEmpty()) {
            g.drawString(font, "Keine Einstellungen", x + 8, cy + 5, DIM, false);
            cy += rowH;
        }
        for (Setting<?> s : m.settings) {
            g.drawString(font, s.name, x + 8, cy + 2, TEXT, false);
            if (s instanceof Setting.Bool b) {
                boolean on = b.get();
                pill(g, x + w - 28, cy + 1, 20, 9, on ? ACCENT2 : OFF);
                pill(g, x + w - (on ? 18 : 26), cy + 2, 7, 7, 0xFFFFFFFF);
                winHits.add(new Hit(x, cy, w, rowH, m, (btn, mxx) -> { b.set(!b.get()); ConfigManager.save(); }));
            } else if (s instanceof Setting.Num n) {
                String v = String.format(Locale.ROOT, "%.1f", n.get());
                g.drawString(font, v, x + w - 8 - font.width(v), cy + 2, ACCENT, false);
                int bx0 = x + 8, bw = w - 16;
                g.fill(bx0, cy + 13, bx0 + bw, cy + 15, OFF);
                int fw = (int) (bw * n.fraction());
                g.fill(bx0, cy + 13, bx0 + fw, cy + 15, ACCENT);
                g.fill(bx0 + fw - 1, cy + 11, bx0 + fw + 2, cy + 17, 0xFFFFFFFF);
                winHits.add(new Hit(x, cy, w, rowH, m, (btn, mxx) -> {
                    dragging = n;
                    dragX = bx0;
                    dragW = bw;
                    n.setFromFraction((mxx - bx0) / (double) bw);
                }));
            } else if (s instanceof Setting.Mode md) {
                String v = md.get();
                g.drawString(font, v, x + w - 8 - font.width(v), cy + 2, ACCENT, false);
                winHits.add(new Hit(x, cy, w, rowH, m, (btn, mxx) -> {
                    md.cycle(btn == 1 ? -1 : 1);
                    ConfigManager.save();
                }));
            } else if (s instanceof Setting.Color c) {
                g.fill(x + w - 20, cy + 1, x + w - 8, cy + 11, 0xFFFFFFFF);
                g.fill(x + w - 19, cy + 2, x + w - 9, cy + 10, c.get());
                winHits.add(new Hit(x, cy, w, rowH, m, (btn, mxx) -> {
                    int i = 0;
                    for (int k = 0; k < PALETTE.length; k++) if (PALETTE[k] == c.get()) i = k;
                    c.set(PALETTE[Math.floorMod(i + (btn == 1 ? -1 : 1), PALETTE.length)]);
                    ConfigManager.save();
                }));
            }
            cy += rowH;
        }

        // Keybind-Zeile
        g.fill(x + 8, cy - 1, x + w - 8, cy, 0xFF2A2E33);
        g.drawString(font, "Keybind", x + 8, cy + 5, TEXT, false);
        String kn = listening == m && !listeningGui ? "Taste druecken..." : ModuleManager.keyName(m.getKey());
        g.drawString(font, "[" + kn + "]", x + w - 8 - font.width("[" + kn + "]"), cy + 5, ACCENT, false);
        winHits.add(new Hit(x, cy, w, rowH, m, (btn, mxx) -> {
            if (btn == 1) { m.setKey(-1); ConfigManager.save(); } else { listening = m; listeningGui = false; }
        }));
    }

    private void drawConfigsWindow(GuiGraphics g, int mx, int my) {
        List<String> profiles = ConfigManager.profiles();
        int w = 260, rowH = 16;
        int h = HH + 22 + profiles.size() * rowH + 10;
        int x = (width - w) / 2, y = Math.max(TOP + 4, (height - h) / 2);
        winRect = new int[] {x, y, w, h};
        g.fill(x, y, x + w, y + h, 0xF0181A1E);
        g.fill(x, y, x + w, y + HH, HEAD);
        g.fill(x, y + HH - 1, x + w, y + HH, ACCENT);
        g.drawString(font, "Configs", x + 8, y + 4, ACCENT, false);

        int by = y + HH + 4;
        int bx = x + 8;
        bx = button(g, bx, by, "Speichern", mx, my, (b, m) -> { ConfigManager.save(); flash("Profil gespeichert"); });
        bx = button(g, bx + 4, by, "Neu", mx, my, (b, m) -> {
            int n = 1;
            while (!ConfigManager.create("custom-" + n)) n++;
            flash("Profil custom-" + n + " erstellt");
        });
        bx = button(g, bx + 4, by, "GUI-Taste", mx, my, (b, m) -> { listening = ModuleManager.all().get(0); listeningGui = true; });
        String gk = listening != null && listeningGui ? "Taste druecken..." : ModuleManager.keyName(ModuleManager.guiKey);
        g.drawString(font, "[" + gk + "]", bx + 6, by + 3, ACCENT, false);

        int cy = by + 22;
        for (String p : profiles) {
            boolean act = p.equals(ConfigManager.active());
            g.drawString(font, (act ? "> " : "  ") + p, x + 8, cy + 3, act ? ACCENT : TEXT, false);
            int rx = x + w - 8;
            rx = rightButton(g, rx, cy, "Loeschen", mx, my, (b, m) -> {
                if (ConfigManager.delete(p)) flash("Profil geloescht"); else flash("Aktives/letztes Profil bleibt");
            });
            rx = rightButton(g, rx - 4, cy, "Kopie", mx, my, (b, m) -> {
                String n = p + "-kopie";
                int i = 2;
                String cand = n;
                while (!ConfigManager.copy(p, cand)) cand = n + i++;
                flash("Kopiert als " + cand);
            });
            rightButton(g, rx - 4, cy, "Laden", mx, my, (b, m) -> {
                if (ConfigManager.load(p)) flash("Profil " + p + " geladen"); else flash("Laden fehlgeschlagen");
            });
            cy += rowH;
        }
    }

    private int button(GuiGraphics g, int x, int y, String label, int mx, int my, Click c) {
        int w = font.width(label) + 8;
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + 14;
        g.fill(x, y, x + w, y + 14, hov ? 0xFF2A2E33 : 0xFF15171A);
        g.fill(x, y + 13, x + w, y + 14, ACCENT2);
        g.drawString(font, label, x + 4, y + 3, TEXT, false);
        winHits.add(new Hit(x, y, w, 14, null, c));
        return x + w;
    }

    /** Button, der rechtsbuendig bei rightX endet; gibt die linke Kante zurueck. */
    private int rightButton(GuiGraphics g, int rightX, int y, String label, int mx, int my, Click c) {
        int w = font.width(label) + 8;
        int x = rightX - w;
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + 13;
        g.fill(x, y, x + w, y + 13, hov ? 0xFF2A2E33 : 0xFF15171A);
        g.drawString(font, label, x + 4, y + 3, ACCENT, false);
        winHits.add(new Hit(x, y, w, 13, null, c));
        return x;
    }

    private static void pill(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x + 1, y, x + w - 1, y + h, c);
        g.fill(x, y + 1, x + w, y + h - 1, c);
    }

    private void flash(String s) {
        status = s;
        statusUntil = System.currentTimeMillis() + 2500;
    }

    // ---------------------------------------------------------------- Input

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        int mx = (int) e.x(), my = (int) e.y(), b = e.button();
        if (listening != null) {
            listening = null;
            return true;
        }
        if (settingsFor != null || configsOpen) {
            boolean inside = mx >= winRect[0] && mx < winRect[0] + winRect[2]
                    && my >= winRect[1] && my < winRect[1] + winRect[3];
            if (!inside) {
                settingsFor = null;
                configsOpen = false;
                return true;
            }
            for (int i = winHits.size() - 1; i >= 0; i--) {
                Hit h = winHits.get(i);
                if (h.contains(mx, my)) {
                    h.click().on(b, mx);
                    return true;
                }
            }
            return true;
        }
        for (Hit h : hits) {
            if (!h.contains(mx, my)) continue;
            if (h.module() == null) {
                h.click().on(b, mx);
                return true;
            }
            if (my <= TOP || my >= height - BOTTOM) continue;
            moduleClick(h.module(), b);
            return true;
        }
        return super.mouseClicked(e, doubleClick);
    }

    private void moduleClick(Module m, int button) {
        if (button == 0) {
            if (!m.isImplemented()) {
                flash(m.name + " ist noch nicht implementiert");
                return;
            }
            m.toggle();
            ConfigManager.save();
            flash(m.name + (m.isEnabled() ? " aktiviert" : " deaktiviert"));
        } else if (button == 1) {
            settingsFor = m;
            configsOpen = false;
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        if (dragging != null) {
            dragging.setFromFraction((e.x() - dragX) / (double) dragW);
            return true;
        }
        return super.mouseDragged(e, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        if (dragging != null) {
            dragging = null;
            ConfigManager.save();
            return true;
        }
        return super.mouseReleased(e);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (settingsFor == null && !configsOpen) {
            scroll = Math.max(0, Math.min(maxScroll, scroll - scrollY * 18));
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        int key = e.key();
        if (listening != null) {
            if (listeningGui) {
                if (key != GLFW.GLFW_KEY_ESCAPE && key != GLFW.GLFW_KEY_BACKSPACE) {
                    Module clash = ModuleManager.findKeyConflict(null, key);
                    if (clash != null) {
                        flash("Doppelt belegt: " + clash.name);
                    } else {
                        ModuleManager.guiKey = key;
                        ConfigManager.save();
                    }
                }
            } else if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) {
                listening.setKey(-1);
                ConfigManager.save();
            } else if (key == ModuleManager.guiKey) {
                flash("Taste ist die GUI-Taste");
            } else {
                Module clash = ModuleManager.findKeyConflict(listening, key);
                if (clash != null) {
                    flash("Doppelt belegt: " + clash.name);
                } else {
                    listening.setKey(key);
                    ConfigManager.save();
                    flash(listening.name + " -> " + ModuleManager.keyName(key));
                }
            }
            listening = null;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (settingsFor != null || configsOpen) {
                settingsFor = null;
                configsOpen = false;
            } else {
                onClose();
            }
            return true;
        }
        if (key == ModuleManager.guiKey) {
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) {
            query = query.substring(0, query.length() - 1);
            scroll = 0;
            return true;
        }
        return super.keyPressed(e);
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        if (listening != null || settingsFor != null || configsOpen) return false;
        int cp = e.codepoint();
        if (cp >= 32 && cp != 127 && query.length() < 24) {
            query += new String(Character.toChars(cp));
            scroll = 0;
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        super.onClose();
    }
}
