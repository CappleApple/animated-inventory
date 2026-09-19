package com.cappleapple.animatedinventory.client.render;

import com.cappleapple.animatedinventory.api.animation.Bounds;
import com.cappleapple.animatedinventory.client.ClientRuntime;
import com.cappleapple.animatedinventory.client.ClientConfig;
import org.joml.Matrix3x2f;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.cappleapple.animatedinventory.client.VanillaInventoryProvider;
import com.cappleapple.animatedinventory.mixin.ContainerScreenAccess;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/** Applies presentation only to native item output; preview quantities keep their own ownership. */
public final class NativeSlotRenderer {
    private static final ThreadLocal<List<Elevated>> ELEVATED = new ThreadLocal<>();

    public static void draw(GuiGraphicsExtractor graphics, AbstractContainerScreen<?> screen, Slot slot,
                            ItemStack stack, String count, BiConsumer<GuiGraphicsExtractor, ItemStack> original) {
        var runtime = ClientRuntime.INSTANCE;
        if (!runtime.canAnimateSlot(screen, slot)) { original.accept(graphics, stack); return; }
        String id = VanillaInventoryProvider.id(slot);
        boolean preview = preview(stack, slot.getItem(), count);
        ItemStack rendered = selectStack(stack, slot.getItem(), count, () -> runtime.normalStack(screen, id, stack));
        long now = System.nanoTime();
        var access = (ContainerScreenAccess)screen;
        Bounds logical = Bounds.item(access.animatedinventory$left() + slot.x, access.animatedinventory$top() + slot.y);
        double scale = runtime.emphasis.scale(id, logical.contains(runtime.mouseX, runtime.mouseY), now);
        var inline = preview ? null : runtime.animations.destination(id, now);
        if (inline != null && inline.inline) scale *= inline.scale(now);
        var batch = ELEVATED.get();
        var capture = scale > 1 && ClientConfig.HOVER_Z.get() > 0 && batch != null ? new TextureCompositor.CaptureState() : null;
        var output = capture == null ? graphics : TextureCompositor.isolate(graphics, capture);
        output.pose().pushMatrix();
        try {
            if (inline != null && inline.inline) {
                Bounds visual = inline.bounds(now);
                output.pose().translate((float)(visual.x() - logical.x()), (float)(visual.y() - logical.y()));
            }
            output.pose().translate(slot.x + 8, slot.y + 8);
            output.pose().scale((float)scale);
            output.pose().translate(-slot.x - 8, -slot.y - 8);
            original.accept(output, rendered);
        } finally { output.pose().popMatrix(); }
        if (capture != null) batch.add(new Elevated(ClientConfig.HOVER_Z.get(), capture.snapshot()));
    }

    /** Native callbacks run once immediately; only their extracted output is replayed above ordinary slots. */
    public static void withElevatedLayer(GuiGraphicsExtractor graphics, Runnable extraction) {
        var previous = ELEVATED.get();
        List<Elevated> current = new ArrayList<>();
        ELEVATED.set(current);
        try {
            extraction.run();
            if (!current.isEmpty()) {
                graphics.nextStratum();
                current.sort(Comparator.comparingDouble(Elevated::z));
                for (Elevated item : current) TextureCompositor.draw(graphics, item.snapshot(), new Matrix3x2f(), 1);
            }
        } finally {
            current.clear();
            if (previous == null) ELEVATED.remove(); else ELEVATED.set(previous);
        }
    }
    private record Elevated(double z, TextureCompositor.Snapshot snapshot) { }
    public static ItemStack selectStack(ItemStack extracted, ItemStack real, String count, Supplier<ItemStack> normal) {
        return preview(extracted, real, count) ? extracted : normal.get();
    }
    private static boolean preview(ItemStack extracted, ItemStack real, String count) {
        return count != null || !ItemStack.matches(extracted, real);
    }
    private NativeSlotRenderer() { }
}
