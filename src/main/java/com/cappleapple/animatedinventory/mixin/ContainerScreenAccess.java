package com.cappleapple.animatedinventory.mixin;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccess {
    @Accessor("leftPos") int animatedinventory$left();
    @Accessor("topPos") int animatedinventory$top();
    @Invoker("isHovering") boolean animatedinventory$isHovering(Slot slot, double mouseX, double mouseY);
}
