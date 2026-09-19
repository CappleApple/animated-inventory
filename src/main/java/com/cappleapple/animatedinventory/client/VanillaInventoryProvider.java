package com.cappleapple.animatedinventory.client;

import com.cappleapple.animatedinventory.api.animation.Bounds;
import com.cappleapple.animatedinventory.api.inventory.*;
import com.cappleapple.animatedinventory.client.compat.bundlednotsiloed.BundledCompatibility;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import com.cappleapple.animatedinventory.client.compat.sophisticated.*;

public final class VanillaInventoryProvider implements InventoryViewProvider {
    public static String id(Slot slot) { return "slot:" + slot.index; }
    public static List<Slot> slots(AbstractContainerScreen<?> screen) {
        return screen instanceof SophisticatedView && screen.getMenu() instanceof SophisticatedMenu menu
                ? menu.animatedinventory$allSlots() : screen.getMenu().slots;
    }
    /** Auxiliary widgets can call a native renderer with an unrelated Slot sharing a menu index. */
    public static boolean ownsSlot(AbstractContainerScreen<?> screen, Slot slot) {
        if (slot == null) return false;
        List<Slot> slots = slots(screen);
        return slot.index >= 0 && slot.index < slots.size() && slots.get(slot.index) == slot;
    }
    @Override public String id() { return "animatedinventory:vanilla"; }
    @Override public boolean supports(Screen screen) { return screen instanceof AbstractContainerScreen<?>; }
    @Override public boolean controlsNormalRendering() { return true; }
    public boolean eligible(Screen screen, Slot slot) {
        return !(screen instanceof CreativeModeInventoryScreen) || slot.container instanceof Inventory;
    }
    @Override public long revision(Screen screen) {
        AbstractContainerScreen<?> container = (AbstractContainerScreen<?>)screen;
        long hash = layout(container);
        for (Slot slot : slots(container)) if (eligible(screen, slot)) hash = hash * 31 + stackHash(slot.getItem());
        return (hash * 31 + stackHash(container.getMenu().getCarried())) * 31 + BundledCompatibility.contentsRevision();
    }
    private static long stackHash(ItemStack stack) { return (long)java.util.Objects.hash(stack.getItem(), stack.getTag()) * 31 + stack.getCount(); }
    public static long layout(AbstractContainerScreen<?> screen) {
        long hash = System.identityHashCode(screen.getMenu());
        hash = hash * 31 + screen.width; hash = hash * 31 + screen.height;
        hash = hash * 31 + screen.getGuiLeft(); hash = hash * 31 + screen.getGuiTop();
        for (Slot slot : slots(screen)) {
            hash = hash * 31 + System.identityHashCode(slot); hash = hash * 31 + slot.x;
            hash = hash * 31 + slot.y; hash = hash * 31 + (slot.isActive() ? 1 : 0);
        }
        if (screen.getMenu() instanceof SophisticatedMenu menu) hash = hash * 31 + menu.animatedinventory$permissionsRevision();
        return hash * 31 + BundledCompatibility.projection().revision();
    }
    @Override public InventoryVisualSnapshot capture(Screen screen, long owner, double mouseX, double mouseY) {
        AbstractContainerScreen<?> container = (AbstractContainerScreen<?>)screen;
        List<VisualItem> items = new ArrayList<>();
        var projection = BundledCompatibility.projection();
        Bounds viewport = new Bounds(0, 0, screen.width, screen.height);
        for (Slot slot : slots(container)) {
            if (!eligible(screen, slot)) continue;
            boolean player = slot.container instanceof Inventory;
            Bounds bounds = Bounds.item(container.getGuiLeft() + slot.x, container.getGuiTop() + slot.y);
            items.add(new VisualItem(id(slot), slot.getItem(), bounds, player ? "player" : "container",
                    slot.isActive() && viewport.intersects(bounds) && (!(screen instanceof SophisticatedView view) || view.animatedinventory$visible(slot)),
                    !BundledCompatibility.customProjection(slot, projection) && (!(screen instanceof SophisticatedView view) || view.animatedinventory$allows(slot)),
                    player && slot.getContainerSlot() >= 36 && slot.getContainerSlot() <= 40,
                    null, null, null, null));
        }
        items.add(new VisualItem("cursor", container.getMenu().getCarried(), Bounds.item(mouseX - 8, mouseY - 8), "cursor"));
        BundledCompatibility.appendStowedDestinations(container, projection, items);
        return InventoryVisualSnapshot.of(owner, id(), layout(container), screen instanceof CreativeModeInventoryScreen || screen instanceof SophisticatedView || projection.virtualized(), items);
    }
}
