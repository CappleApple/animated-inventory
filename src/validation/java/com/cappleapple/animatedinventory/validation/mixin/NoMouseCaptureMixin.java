package com.cappleapple.animatedinventory.validation.mixin;
import com.cappleapple.animatedinventory.validation.ClientSmoke;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MouseHandler.class)
abstract class NoMouseCaptureMixin {
    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void animatedinventory$noCapture(CallbackInfo ci) { ci.cancel(); }
}
