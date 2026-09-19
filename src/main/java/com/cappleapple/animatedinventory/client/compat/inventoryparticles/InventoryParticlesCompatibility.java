package com.cappleapple.animatedinventory.client.compat.inventoryparticles;

import com.cappleapple.animatedinventory.client.ClientConfig;
import com.cappleapple.animatedinventory.client.Platform;

/** No undocumented particle hooks, fake input, or extra particle render invocations. */
public final class InventoryParticlesCompatibility {
    private static boolean present;
    public static void initialize() { present = Platform.isLoaded("inventory_particles"); }
    /** Prefer one native render for whole-stack moves; confirmed partial transfers use detached models. */
    public static boolean inlineOnly() { return present && ClientConfig.PARTICLES.get(); }
    private InventoryParticlesCompatibility() { }
}
