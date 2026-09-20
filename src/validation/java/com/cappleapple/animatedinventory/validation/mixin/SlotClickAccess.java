package com.cappleapple.animatedinventory.validation.mixin;
import org.spongepowered.asm.mixin.Mixin; import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class) public interface SlotClickAccess { @Invoker("slotClicked") void animatedinventory$click(net.minecraft.world.inventory.Slot slot, int index, int button, net.minecraft.world.inventory.ClickType type); }
