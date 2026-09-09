package com.cappleapple.animatedinventory.validation;

import com.cappleapple.animatedinventory.client.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.*;
import java.util.Map;
import static com.cappleapple.animatedinventory.validation.ClientValidation.require;

/** Actual optional TrashSlot widget exercised alongside the authoritative transfer fixture. */
final class TrashSlotValidation {
    private static final boolean ENABLED = Boolean.getBoolean("animatedinventory.trashValidation");

    static void prepare(AbstractContainerScreen<?> screen) throws Exception {
        if (!ENABLED) return;
        Class<?> handler = Class.forName("net.blay09.mods.trashslot.client.TrashSlotGuiHandler");
        var field = handler.getDeclaredField("currentContainerSettings"); field.setAccessible(true);
        Object settings = field.get(null);
        settings.getClass().getMethod("setEnabled", boolean.class).invoke(settings, true);
        Object component = handler.getMethod("getTrashSlotComponent").invoke(null);
        require(component != null && (boolean)component.getClass().getMethod("isVisible").invoke(component),
                "actual TrashSlot widget is showing: " + screen.getClass().getSimpleName());
        clear();
        Slot trash = slot();
        require(trash.index == 0 && trash != screen.getMenu().getSlot(0)
                && screen.getMenu().slots.stream().noneMatch(s -> s == trash),
                "independent TrashSlot aliases real menu slot zero");
    }

    static void pending(AbstractContainerScreen<?> screen, Slot source) throws Exception {
        if (!ENABLED) return;
        var runtime = ClientRuntime.INSTANCE;
        require(runtime.pendingStack(screen, source, source.getItem()).is(Items.DIAMOND),
                "real source retains diamonds while transfer acknowledgement is pending");
        Slot trash = slot();
        require(trash.getItem().isEmpty(), "actual TrashSlot is empty before pending render");
        require(runtime.pendingStack(screen, trash, trash.getItem()).isEmpty(),
                "empty TrashSlot never borrows pending source diamonds");
        verify(screen, "pending shift click");
        ClientValidation.captureSophisticated(screen, "trash-pending-" + screen.getClass().getSimpleName());
    }

    @SuppressWarnings("unchecked")
    static void verify(AbstractContainerScreen<?> screen, String phase) throws Exception {
        if (!ENABLED) return;
        var runtime = ClientRuntime.INSTANCE;
        Slot trash = slot();
        ItemStack real = trash.getItem();
        require(runtime.pendingStack(screen, trash, real) == real,
                "TrashSlot retains its own native stack: " + phase);
        require(!runtime.canAnimateSlot(screen, trash), "TrashSlot never inherits menu animation: " + phase);
        require(runtime.canAnimateSlot(screen, screen.getMenu().getSlot(0)),
                "real menu slot zero remains eligible: " + phase);
        var field = ClientRuntime.class.getDeclaredField("observedNativeSlots"); field.setAccessible(true);
        var observed = (Map<String, Slot>)field.get(runtime);
        require(observed.get("slot:0") == screen.getMenu().getSlot(0),
                "TrashSlot rendering does not replace real source ownership: " + phase);
    }

    static void retainedItem() throws Exception {
        if (!ENABLED) return;
        // Emulate the native client notification for the retained undo item, without deleting fixture items.
        contents(new ItemStack(Items.GOLD_INGOT, 3));
        require(slot().getItem().is(Items.GOLD_INGOT) && slot().getItem().getCount() == 3,
                "TrashSlot retained undo item is available to its native renderer");
    }
    static void clear() throws Exception { if (ENABLED) contents(ItemStack.EMPTY); }
    private static void contents(ItemStack stack) throws Exception {
        Class.forName("net.blay09.mods.trashslot.client.TrashSlotClient")
                .getMethod("receivedTrashSlotContent", ItemStack.class).invoke(null, stack);
    }
    private static Slot slot() throws Exception {
        return (Slot)Class.forName("net.blay09.mods.trashslot.client.TrashSlotGuiHandler").getMethod("getTrashSlot").invoke(null);
    }
    private TrashSlotValidation() { }
}
