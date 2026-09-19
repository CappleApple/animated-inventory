package com.cappleapple.animatedinventory.client.compat.bundlednotsiloed;
import com.cappleapple.animatedinventory.api.inventory.VisualItem;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import java.util.List;
/** The original adapter requires NeoForge player attachments, unavailable on Fabric. */
public final class BundledCompatibility {
    public record Projection(long revision, boolean virtualized, boolean customRendering, List<Integer> slots) {
        private static final Projection NONE = new Projection(0, false, false, List.of());
    }
    public static void initialize() { }
    public static boolean active() { return false; }
    public static Projection projection() { return Projection.NONE; }
    public static long contentsRevision() { return 0; }
    public static long serverRevision() { return -1; }
    public static void appendStowedDestinations(AbstractContainerScreen<?> screen, Projection projection, List<VisualItem> items) { }
    public static boolean customProjection(Slot slot, Projection projection) { return false; }
    private BundledCompatibility() { }
}
