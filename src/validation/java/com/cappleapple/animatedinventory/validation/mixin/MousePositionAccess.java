package com.cappleapple.animatedinventory.validation.mixin;
import org.spongepowered.asm.mixin.Mixin; import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(net.minecraft.client.MouseHandler.class) public interface MousePositionAccess { @org.spongepowered.asm.mixin.gen.Accessor("xpos") void animatedinventory$x(double value); @org.spongepowered.asm.mixin.gen.Accessor("ypos") void animatedinventory$y(double value); }
