package com.nerodev.grandline.dev;

import com.nerodev.grandline.GrandLine;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * CI helper: with {@code -Dgrandline.autotest.server=true} a dedicated server generates its world and shuts down
 * straight away, so the client autotest has a world to open. Does nothing otherwise.
 */
public final class AutoTestServer {
    private AutoTestServer() {}

    public static void register() {
        if (!Boolean.getBoolean("grandline.autotest.server")) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event -> {
            GrandLine.LOGGER.info("[autotest] world generated, stopping server");
            event.getServer().execute(() -> event.getServer().halt(false));
        });
    }
}
