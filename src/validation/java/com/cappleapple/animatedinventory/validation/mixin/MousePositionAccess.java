package com.cappleapple.animatedinventory.validation.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MousePositionAccess {
    @Accessor("xpos") void validation$x(double value);
    @Accessor("ypos") void validation$y(double value);
}
