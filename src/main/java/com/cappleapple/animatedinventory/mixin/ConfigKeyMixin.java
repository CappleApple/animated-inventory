package com.cappleapple.animatedinventory.mixin;
import com.cappleapple.animatedinventory.client.config.AnimationConfigScreen;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardHandler.class)
abstract class ConfigKeyMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void animatedinventory$config(long window, int key, int scanCode, int action, int modifiers, CallbackInfo ci) {
        var client = Minecraft.getInstance();
        if (window == client.getWindow().getWindow() && key == GLFW.GLFW_KEY_F8 && action == GLFW.GLFW_PRESS
                && !(client.screen instanceof AnimationConfigScreen)) {
            client.setScreen(new AnimationConfigScreen(client.screen)); ci.cancel();
        }
    }
}
