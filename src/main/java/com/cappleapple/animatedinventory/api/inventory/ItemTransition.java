package com.cappleapple.animatedinventory.api.inventory;

import com.cappleapple.animatedinventory.api.animation.*;
import net.minecraft.world.item.ItemStack;
import java.util.Objects;

public record ItemTransition(TransitionType type, String sourceId, String destinationId, ItemStack stack,
                             Bounds source, Bounds destination, Bounds clipRegion, AnimationOptions options) {
    public ItemTransition {
        Objects.requireNonNull(type); Objects.requireNonNull(stack); Objects.requireNonNull(source); Objects.requireNonNull(destination);
        stack = stack.copy();
    }
}
