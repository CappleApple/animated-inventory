package com.cappleapple.animatedinventory.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccess {
    @org.spongepowered.asm.mixin.gen.Accessor("leftPos") int animatedinventory$left();
    @org.spongepowered.asm.mixin.gen.Accessor("topPos") int animatedinventory$top();

    @Invoker("isHovering")
    boolean animatedinventory$isHovering(Slot slot, double mouseX, double mouseY);
}
