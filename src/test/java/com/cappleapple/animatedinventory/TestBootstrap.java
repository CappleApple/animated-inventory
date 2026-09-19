package com.cappleapple.animatedinventory;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraftforge.network.NetworkEvent;

/** JUnit loads untransformed Forge classes; initialize the event's native listener cache explicitly. */
final class TestBootstrap {
    private static boolean initialized;
    static synchronized void initialize() {
        if (initialized) return;
        new NetworkEvent(() -> null).getListenerList();
        new NetworkEvent.GatherLoginPayloadsEvent(new java.util.ArrayList<>(), false).getListenerList();
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        initialized = true;
    }
}
