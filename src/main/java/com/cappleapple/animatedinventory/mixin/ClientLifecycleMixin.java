package com.cappleapple.animatedinventory.mixin;
import com.cappleapple.animatedinventory.client.ClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Minecraft.class)
abstract class ClientLifecycleMixin {
    @Inject(method = "tick", at = @At("TAIL")) private void animatedinventory$tick(CallbackInfo ci) { ClientRuntime.INSTANCE.tick(); }
    @Inject(method = "setScreen", at = @At("HEAD")) private void animatedinventory$screen(Screen next, CallbackInfo ci) {
        Screen current = Minecraft.getInstance().screen;
        ClientRuntime.INSTANCE.opening(current, next);
        if (current != null) ClientRuntime.INSTANCE.closing(current);
    }
}
