package de.ultimateclient.modules;

import de.ultimateclient.core.Category;
import de.ultimateclient.core.Module;

/**
 * Geplantes, noch NICHT implementiertes Modul. Es erscheint in der GUI (ausgegraut),
 * kann aber nicht aktiviert werden - so gibt es keine Schalter ohne Funktion.
 */
public class StubModule extends Module {
    public StubModule(Category category, String name, String description) {
        super(name, description, category);
    }

    @Override
    public boolean isImplemented() {
        return false;
    }

    @Override
    public void setEnabled(boolean on) {
        // bewusst leer
    }
}
