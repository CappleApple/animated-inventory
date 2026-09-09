package com.cappleapple.animatedinventory.api.inventory;

import com.cappleapple.animatedinventory.api.animation.*;
import net.minecraft.world.item.ItemStack;
import java.util.Objects;

/** ID is stable logical identity within one provider; it need not be a Slot index. */
public record VisualItem(String id, ItemStack stack, Bounds bounds, String region, boolean visible,
                         boolean mayAnimate, boolean equipment, Bounds clipRegion,
                         Bounds destinationBounds, String transactionId, AnimationOptions overrides, Bounds sourceBounds) {
    public VisualItem {
        Objects.requireNonNull(id); Objects.requireNonNull(stack); Objects.requireNonNull(bounds); Objects.requireNonNull(region);
        stack = stack.copy();
    }
    /** Invisible storage may expose a visible arrival point without becoming a normal render owner. */
    public boolean offscreenDestination() { return !visible && mayAnimate && destinationBounds != null; }
    /** Invisible storage can expose an arrival origin independently of its stowing destination. */
    public boolean offscreenSource() { return !visible && mayAnimate && sourceBounds != null; }
    public VisualItem(String id, ItemStack stack, Bounds bounds, String region, boolean visible,
                      boolean mayAnimate, boolean equipment, Bounds clipRegion, Bounds destinationBounds,
                      String transactionId, AnimationOptions overrides) {
        this(id, stack, bounds, region, visible, mayAnimate, equipment, clipRegion, destinationBounds, transactionId, overrides, null);
    }
    public VisualItem(String id, ItemStack stack, Bounds bounds, String region) {
        this(id, stack, bounds, region, true, true, false, null, null, null, null);
    }
}
