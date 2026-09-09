package com.cappleapple.animatedinventory.api;

import com.cappleapple.animatedinventory.api.animation.*;
import com.cappleapple.animatedinventory.api.inventory.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Client-thread API. IDs and bounds belong to a provider; no menu indices are required. */
public final class AnimatedInventoryApi {
    public interface Backend {
        boolean enabled();
        AutoCloseable register(InventoryViewProvider provider);
        long owner(Screen screen);
        List<Long> submit(Screen screen, InventoryVisualTransaction transaction);
        Optional<Bounds> bounds(Screen screen, String id, boolean animated);
        Optional<Bounds> normalBounds(Screen screen, String id);
        ItemStack normalStack(Screen screen, String id, ItemStack real);
        void invalidate(Screen screen, boolean reflow);
        void cancel(long handle);
        void render(Screen screen, GuiGraphics graphics);
    }
    private static Backend backend;
    /** Implementation bootstrap; integrations should not replace the backend. */
    public static void install(Backend value) {
        if (backend != null) throw new IllegalStateException("Animated Inventory is already initialized");
        backend = Objects.requireNonNull(value);
    }
    public static boolean isEnabled() { return backend != null && backend.enabled(); }
    public static AutoCloseable registerInventoryViewProvider(InventoryViewProvider provider) { return require().register(provider); }
    public static long owner(Screen screen) { return require().owner(screen); }
    public static List<Long> notifyTransaction(Screen screen, InventoryVisualTransaction transaction) {
        return isEnabled() ? backend.submit(screen, transaction) : List.of();
    }
    public static List<Long> animate(Screen screen, String transactionId, ItemTransition... transitions) {
        return notifyTransaction(screen, new InventoryVisualTransaction(owner(screen), transactionId, false, List.of(transitions)));
    }
    public static List<Long> animateItemMove(Screen screen, String transactionId, String sourceId, String destinationId,
                                             ItemStack stack, Bounds source, Bounds destination, Bounds clip, AnimationOptions options) {
        return animate(screen, transactionId, new ItemTransition(TransitionType.MOVE, sourceId, destinationId, stack, source, destination, clip, options));
    }
    public static List<Long> animateItemAppear(Screen screen, String id, ItemStack stack, Bounds destination, AnimationOptions options) {
        return animate(screen, UUID.randomUUID().toString(), new ItemTransition(TransitionType.APPEAR, null, id, stack, destination, destination, null, options));
    }
    public static List<Long> animateItemDisappear(Screen screen, String id, ItemStack stack, Bounds source, AnimationOptions options) {
        return animate(screen, UUID.randomUUID().toString(), new ItemTransition(TransitionType.DISAPPEAR, id, null, stack, source, source, null, options));
    }
    public static List<Long> animateItemMerge(Screen screen, String transactionId, ItemTransition... transfers) {
        return animate(screen, transactionId, Arrays.stream(transfers).map(t -> new ItemTransition(TransitionType.MERGE,
                t.sourceId(), t.destinationId(), t.stack(), t.source(), t.destination(), t.clipRegion(), t.options())).toArray(ItemTransition[]::new));
    }
    public static List<Long> animateItemSplit(Screen screen, String transactionId, ItemTransition... transfers) {
        return animate(screen, transactionId, Arrays.stream(transfers).map(t -> new ItemTransition(TransitionType.SPLIT,
                t.sourceId(), t.destinationId(), t.stack(), t.source(), t.destination(), t.clipRegion(), t.options())).toArray(ItemTransition[]::new));
    }
    public static Optional<Bounds> getAnimatedBounds(Screen screen, String id) { return backend == null ? Optional.empty() : backend.bounds(screen, id, true); }
    /** Bounds for a provider\u0027s single native render, including the particle-safe inline movement path. */
    public static Optional<Bounds> getNormalRenderBounds(Screen screen, String id) { return backend == null ? Optional.empty() : backend.normalBounds(screen, id); }
    public static Optional<Bounds> getLogicalBounds(Screen screen, String id) { return backend == null ? Optional.empty() : backend.bounds(screen, id, false); }
    /** Render this defensive adjusted copy instead of changing the real item. Always returns real when disabled. */
    public static ItemStack normalRenderStack(Screen screen, String id, ItemStack real) {
        return isEnabled() ? backend.normalStack(screen, id, real) : real;
    }
    public static void notifyLayoutChanged(Screen screen, boolean animateReflow) { if (backend != null) backend.invalidate(screen, animateReflow); }
    /** Custom non-container screens call this once after items and before cursor/tooltips. */
    public static void renderAnimations(Screen screen, GuiGraphics graphics) { if (isEnabled()) backend.render(screen, graphics); }
    public static void cancel(long handle) { if (backend != null) backend.cancel(handle); }
    private static Backend require() { return Objects.requireNonNull(backend, "Animated Inventory client has not initialized"); }
    private AnimatedInventoryApi() { }
}
