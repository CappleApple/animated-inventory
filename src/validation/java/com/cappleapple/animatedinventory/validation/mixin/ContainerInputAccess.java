package com.cappleapple.animatedinventory.validation.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerScreen.class)
public interface ContainerInputAccess {
    @Invoker("slotClicked") void validation$click(Slot slot, int index, int button, ClickType type);
}
