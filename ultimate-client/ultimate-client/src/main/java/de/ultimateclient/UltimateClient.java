package de.ultimateclient;

import de.ultimateclient.core.ConfigManager;
import de.ultimateclient.core.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UltimateClient implements ClientModInitializer {
    public static final String NAME = "Ultimate Client";
    public static final Logger LOG = LoggerFactory.getLogger("UltimateClient");

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        ConfigManager.init();
        ClientTickEvents.END_CLIENT_TICK.register(ModuleManager::tick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save());
        LOG.info("{} geladen: {} Module", NAME, ModuleManager.all().size());
    }
}
