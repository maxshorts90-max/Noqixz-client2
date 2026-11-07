package de.ultimateclient.core;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

public abstract class Module {
    public final String name;
    public final String description;
    public final Category category;
    public final List<Setting<?>> settings = new ArrayList<>();

    private boolean enabled;
    private int key = -1; // GLFW-Keycode, -1 = nicht belegt

    protected Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    /** false = Platzhalter, wird in der GUI ausgegraut und laesst sich nicht aktivieren. */
    public boolean isImplemented() {
        return true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean on) {
        if (on == enabled) return;
        enabled = on;
        if (on) onEnable(); else onDisable();
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    protected <S extends Setting<?>> S add(S s) {
        settings.add(s);
        return s;
    }

    protected void onEnable() {}

    protected void onDisable() {}

    public void onTick(Minecraft mc) {}
}
