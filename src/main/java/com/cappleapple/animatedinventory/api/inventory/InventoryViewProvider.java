package com.cappleapple.animatedinventory.api.inventory;

import net.minecraft.client.gui.screens.Screen;

/** Client-thread contract. Providers must not mutate inventory or retain removed screens. */
public interface InventoryViewProvider {
    String id();
    boolean supports(Screen screen);
    /** Cheap revision probe, called at most once per client tick. Include content and layout changes. */
    long revision(Screen screen);
    InventoryVisualSnapshot capture(Screen screen, long owner, double mouseX, double mouseY);
    /** Custom renderers must honor AnimatedInventoryApi.normalRenderStack to opt into detached copies. */
    default boolean controlsNormalRendering() { return false; }
}
