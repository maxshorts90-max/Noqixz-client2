package de.ultimateclient.core;

public enum Category {
    COMBAT("Combat"),
    PLAYER("Player"),
    MOVEMENT("Movement"),
    WORLD("World"),
    BASE_FINDER("Base Finder"),
    ESP("ESP"),
    STORAGE("Storage"),
    RENDER("Render"),
    VISUALS("Visuals"),
    INVENTORY("Inventory"),
    UTILITY("Utility"),
    AUTOMATION("Automation"),
    HUD("HUD"),
    MISC("Miscellaneous");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
