package com.cappleapple.animatedinventory.validation.mixin;
import com.cappleapple.animatedinventory.validation.ClientValidation;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
abstract class ValidationMixin {
    @Redirect(method = "<init>", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwShowWindow(J)V", remap = false))
    private void animatedinventory$hidden(long window) { }
    @Inject(method = "tick", at = @At("TAIL"))
    private void animatedinventory$validate(CallbackInfo ci) { ClientValidation.tick(); }
}
