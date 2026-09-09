package com.cappleapple.animatedinventory.client.compat.bundlednotsiloed;

import com.cappleapple.animatedinventory.client.ClientConfig;
import com.cappleapple.animatedinventory.api.animation.Bounds;
import com.cappleapple.animatedinventory.api.inventory.VisualItem;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.ItemStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.attachment.AttachmentType;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Supplier;

/** Normal BNS cells use native Slots. Read the acknowledged page only to distinguish paging from transfers. */
public final class BundledCompatibility {
    public record Projection(long revision, boolean virtualized, boolean customRendering, List<Integer> slots) {
        private static final Projection NONE = new Projection(0, false, false, List.of());
    }
    private static boolean present;
    private static Supplier<AttachmentType<Object>> attachment;
    private static Method inventoryWindow, active, identityView, logicalSlots, inventory, revision, extent, syntheticStack, clientSync, serverRevision;

    @SuppressWarnings("unchecked")
    public static void initialize() {
        present = ModList.get().isLoaded("bundlednotsiloed");
        if (!present) return;
        try {
            // All members are public. No compile-time optional dependency or private screen access.
            attachment = (Supplier<AttachmentType<Object>>)Class.forName(
                    "com.cappleapple.bundlednotsiloed.data.ModAttachments").getField("PLAYER_DATA").get(null);
            inventoryWindow = Class.forName("com.cappleapple.bundlednotsiloed.data.PlayerInventoryData").getMethod("inventoryWindow");
            Class<?> window = inventoryWindow.getReturnType();
            active = window.getMethod("active");
            identityView = window.getMethod("identityView");
            logicalSlots = window.getMethod("logicalSlots");
            inventory = inventoryWindow.getDeclaringClass().getMethod("inventory");
            clientSync = inventoryWindow.getDeclaringClass().getMethod("clientSync");
            serverRevision = clientSync.getReturnType().getMethod("serverRevision");
            Class<?> storage = inventory.getReturnType();
            revision = storage.getMethod("revision");
            extent = storage.getMethod("syntheticSlotCount");
            syntheticStack = storage.getMethod("syntheticStack", int.class);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) { unavailable(error); }
    }
    public static boolean active() { return present && ClientConfig.BNS.get(); }

    @SuppressWarnings("unchecked")
    public static Projection projection() {
        var player = Minecraft.getInstance().player;
        if (!active() || attachment == null || player == null || !player.hasData(attachment)) return Projection.NONE;
        try {
            Object window = inventoryWindow.invoke(player.getData(attachment));
            boolean identities = (boolean)identityView.invoke(window);
            List<Integer> slots = (List<Integer>)logicalSlots.invoke(window);
            // Acknowledging the same default page changes session state, not visible geometry.
            return new Projection(slots.hashCode() * 31L + (identities ? 1 : 0), true, identities, slots);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            unavailable(error);
            return Projection.NONE;
        }
    }
    public static long contentsRevision() {
        Object storage = storage();
        if (storage == null) return 0;
        try { return (long)revision.invoke(storage); }
        catch (ReflectiveOperationException | RuntimeException error) { unavailable(error); return 0; }
    }
    /** Independent of client-predicted backend revisions; negative until a server baseline is installed. */
    public static long serverRevision() {
        var player = Minecraft.getInstance().player;
        if (!active() || attachment == null || player == null || !player.hasData(attachment)) return -1;
        try { return (long)serverRevision.invoke(clientSync.invoke(player.getData(attachment))); }
        catch (ReflectiveOperationException | RuntimeException error) { unavailable(error); return -1; }
    }
    private static Object storage() {
        var player = Minecraft.getInstance().player;
        if (!active() || attachment == null || player == null || !player.hasData(attachment)) return null;
        try { return inventory.invoke(player.getData(attachment)); }
        catch (ReflectiveOperationException | RuntimeException error) { unavailable(error); return null; }
    }
    /** Only defensive reads; the real inventory and its normal rendering ownership remain untouched. */
    public static void appendStowedDestinations(AbstractContainerScreen<?> screen, Projection projection, List<VisualItem> items) {
        if (projection.customRendering() || projection.slots().size() != 27 || screen instanceof CreativeModeInventoryScreen) return;
        Object storage = storage();
        if (storage == null) return;
        var player = Minecraft.getInstance().player;
        Slot[] grid = new Slot[27];
        for (Slot slot : screen.getMenu().slots) {
            int index = slot.getContainerSlot() - 9;
            if (slot.container == player.getInventory() && index >= 0 && index < grid.length && slot.isActive()) grid[index] = slot;
        }
        int first = projection.slots().getFirst();
        for (int i = 0; i < grid.length; i++) {
            if (grid[i] == null || projection.slots().get(i) != first + i) return;
        }
        double dx = grid[1].x - grid[0].x, dy = grid[9].y - grid[0].y;
        if (dx <= 0 || dy <= 0) return;
        Bounds origin = Bounds.item(screen.getGuiLeft() + grid[0].x, screen.getGuiTop() + grid[0].y);
        try {
            // Bound snapshot size and work independently of BNS's storage capacity.
            int count = (int)extent.invoke(storage);
            if (count > 4096 - items.size()) return;
            for (int index = 9; index < count; index++) {
                if (index >= first && index < first + 27) continue;
                ItemStack stack = (ItemStack)syntheticStack.invoke(storage, index);
                if (stack.isEmpty()) continue;
                Bounds edge = StowedSlotGeometry.destination(index, first, origin, dx, dy);
                items.add(new VisualItem("bns:stowed:" + index, stack, edge, "bns:stowed",
                        false, true, false, null, edge, null, null,
                        StowedSlotGeometry.source(index, first, origin, dx, dy)));
            }
        } catch (ReflectiveOperationException | RuntimeException error) { unavailable(error); }
    }
    /** Search's aggregate labels use a separate renderer; ordinary paged cells remain fully eligible. */
    public static boolean customProjection(Slot slot, Projection projection) {
        return projection.customRendering() && slot.container instanceof Inventory
                && slot.getContainerSlot() >= 9 && slot.getContainerSlot() < 36;
    }
    private static void unavailable(Throwable error) {
        attachment = null;
        LogUtils.getLogger().warn("Animated Inventory could not read BNS page state; native Slot animations remain enabled", error);
    }
    private BundledCompatibility() { }
}
