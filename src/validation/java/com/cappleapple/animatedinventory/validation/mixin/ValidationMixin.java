package com.cappleapple.animatedinventory.validation.mixin;
import com.cappleapple.animatedinventory.validation.ClientSmoke;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
abstract class ValidationMixin {
    @Inject(method = "tick", at = @At("TAIL")) private void animatedinventory$validate(CallbackInfo ci) { ClientSmoke.tick(); }
}
