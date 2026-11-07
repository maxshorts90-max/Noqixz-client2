package de.ultimateclient.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Basisklasse aller Einstellungen. Jede Einstellung kann als JSON gespeichert und geladen werden. */
public abstract class Setting<T> {
    public final String name;
    protected T value;

    protected Setting(String name, T def) {
        this.name = name;
        this.value = def;
    }

    public T get() {
        return value;
    }

    public void set(T v) {
        this.value = v;
    }

    public abstract JsonElement toJson();

    public abstract void fromJson(JsonElement e);

    public static class Bool extends Setting<Boolean> {
        public Bool(String name, boolean def) {
            super(name, def);
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(value);
        }

        @Override
        public void fromJson(JsonElement e) {
            value = e.getAsBoolean();
        }
    }

    public static class Num extends Setting<Double> {
        public final double min, max, step;

        public Num(String name, double def, double min, double max, double step) {
            super(name, def);
            this.min = min;
            this.max = max;
            this.step = step;
        }

        public void setFromFraction(double t) {
            t = Math.max(0, Math.min(1, t));
            double raw = min + t * (max - min);
            value = Math.max(min, Math.min(max, Math.round(raw / step) * step));
        }

        public double fraction() {
            return (value - min) / (max - min);
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(value);
        }

        @Override
        public void fromJson(JsonElement e) {
            value = Math.max(min, Math.min(max, e.getAsDouble()));
        }
    }

    public static class Mode extends Setting<String> {
        public final String[] modes;

        public Mode(String name, String def, String... modes) {
            super(name, def);
            this.modes = modes;
        }

        public void cycle(int dir) {
            int i = 0;
            for (int k = 0; k < modes.length; k++) if (modes[k].equals(value)) i = k;
            value = modes[Math.floorMod(i + dir, modes.length)];
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(value);
        }

        @Override
        public void fromJson(JsonElement e) {
            String s = e.getAsString();
            for (String m : modes) if (m.equals(s)) value = s;
        }
    }

    /** Farbe als ARGB-Integer. */
    public static class Color extends Setting<Integer> {
        public Color(String name, int argb) {
            super(name, argb);
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(value);
        }

        @Override
        public void fromJson(JsonElement e) {
            value = e.getAsInt();
        }
    }
}
