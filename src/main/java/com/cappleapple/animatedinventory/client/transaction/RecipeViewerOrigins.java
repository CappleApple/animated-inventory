package com.cappleapple.animatedinventory.client.transaction;

import com.cappleapple.animatedinventory.api.inventory.*;
import com.cappleapple.animatedinventory.client.VanillaInventoryProvider;
import com.cappleapple.animatedinventory.client.compat.bundlednotsiloed.BundledCompatibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import java.lang.ref.WeakReference;
import java.util.*;

/** A weak, menu-scoped origin retained only while JEI/EMI temporarily shows its recipe screen. */
public final class RecipeViewerOrigins {
    private WeakReference<AbstractContainerScreen<?>> screen = new WeakReference<>(null);
    private InventoryVisualSnapshot before;
    private long requested, serverRevision;
    private int width, height;

    public static boolean viewer(Screen value) {
        if (value == null) return false;
        String name = value.getClass().getName();
        return name.startsWith("dev.emi.emi.screen.") || name.startsWith("mezz.jei.gui.recipes.");
    }
    public void opening(Screen old, Screen next, InventoryVisualSnapshot snapshot) {
        if (viewer(next)) {
            if (old instanceof AbstractContainerScreen<?> container && snapshot != null) {
                screen = new WeakReference<>(container); before = snapshot;
                width = container.width; height = container.height; requested = 0;
            }
        } else if (next != screen.get()) clear();
    }
    public boolean request(double mouseX, double mouseY) {
        var mc = Minecraft.getInstance();
        AbstractContainerScreen<?> original = screen.get();
        if (!viewer(mc.screen) || original == null || before == null || mc.player == null
                || original.getMenu() != mc.player.containerMenu) { clear(); return false; }
        // Refresh quantities at the actual request, preserving the last displayed origin coordinates.
        InventoryVisualSnapshot fresh = new VanillaInventoryProvider().capture(original, before.owner(), mouseX, mouseY);
        Map<String, VisualItem> entries = new LinkedHashMap<>(fresh.items());
        for (VisualItem item : fresh.items().values()) {
            VisualItem old = before.items().get(item.id());
            if (old != null) entries.put(item.id(), new VisualItem(item.id(), item.stack(), old.bounds(), item.region(),
                    old.visible(), old.mayAnimate(), item.equipment(), old.clipRegion(), old.destinationBounds(),
                    item.transactionId(), item.overrides(), old.sourceBounds()));
        }
        before = new InventoryVisualSnapshot(before.owner(), before.providerId(), before.layoutRevision(), before.virtualized(), entries);
        serverRevision = BundledCompatibility.serverRevision(); requested = System.nanoTime();
        return true;
    }
    public SynchronizedTransfer restore(Screen active, InventoryVisualSnapshot current, long revision, String id) {
        AbstractContainerScreen<?> original = screen.get();
        if (active != original) return null;
        try {
            if (requested == 0 || before == null || current == null || active.width != width || active.height != height
                    || System.nanoTime() - requested > 750_000_000L
                    || Minecraft.getInstance().player == null || original.getMenu() != Minecraft.getInstance().player.containerMenu) return null;
            // Returning initializes a new visual owner; only this same menu may inherit the requested origins.
            var rebased = new InventoryVisualSnapshot(current.owner(), current.providerId(), current.layoutRevision(), current.virtualized(), before.items());
            return new SynchronizedTransfer(rebased, id, serverRevision, revision, requested);
        } finally { clear(); }
    }
    public void clear() { screen.clear(); before = null; requested = 0; }
}
