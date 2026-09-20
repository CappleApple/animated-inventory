package com.cappleapple.animatedinventory.validation.mixin;
import com.cappleapple.animatedinventory.validation.ClientSmoke;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Supplies the modifier state for an automated real screen mouse click. */
@Mixin(InputConstants.class)
abstract class ShiftInputMixin {
    @Inject(method = "isKeyDown", at = @At("HEAD"), cancellable = true)
    private static void animatedinventory$shift(long window, int key, CallbackInfoReturnable<Boolean> ci) { if (ClientSmoke.shiftInput && (key == 340 || key == 344)) ci.setReturnValue(true); }
}
