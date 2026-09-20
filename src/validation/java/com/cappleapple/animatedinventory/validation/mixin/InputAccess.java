package com.cappleapple.animatedinventory.validation.mixin;
import org.spongepowered.asm.mixin.Mixin; import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(net.minecraft.client.KeyboardHandler.class) public interface InputAccess { @Invoker("keyPress") void animatedinventory$key(long window, int key, int scan, int action, int modifiers); }
