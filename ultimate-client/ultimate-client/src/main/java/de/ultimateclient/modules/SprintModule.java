package de.ultimateclient.modules;

import de.ultimateclient.core.Category;
import de.ultimateclient.core.Module;
import de.ultimateclient.core.Setting;
import net.minecraft.client.Minecraft;

/** Erstes echtes Modul: haelt die Sprint-Taste gedrueckt, solange man vorwaerts laeuft. */
public class SprintModule extends Module {
    private final Setting.Bool onlyForward = add(new Setting.Bool("Nur vorwaerts", true));

    public SprintModule() {
        super("Sprint", "Sprintet automatisch, solange du vorwaerts laeufst.", Category.MOVEMENT);
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.screen != null) return;
        if (!onlyForward.get() || mc.options.keyUp.isDown()) {
            mc.options.keySprint.setDown(true);
        }
    }
}
