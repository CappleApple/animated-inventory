package com.cappleapple.animatedinventory.validation.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Runtime fixtures must never capture the user's mouse, including while the world opens. */
@Mixin(MouseHandler.class)
abstract class UncapturedMouseMixin {
    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void doNotCapture(CallbackInfo ci) { ci.cancel(); }
}
