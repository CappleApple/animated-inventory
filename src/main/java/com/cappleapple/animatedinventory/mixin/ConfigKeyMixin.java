package com.cappleapple.animatedinventory.mixin;
import com.cappleapple.animatedinventory.client.config.AnimationConfigScreen;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import com.mojang.blaze3d.platform.InputConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(KeyboardHandler.class)
abstract class ConfigKeyMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void animatedinventory$config(long window, int action, KeyEvent key, CallbackInfo ci) {
        var client = Minecraft.getInstance();
        if (window == client.getWindow().handle() && key.key() == InputConstants.KEY_F8 && action == InputConstants.PRESS
                && !(client.gui.screen() instanceof AnimationConfigScreen)) {
            client.gui.setScreen(new AnimationConfigScreen(client.gui.screen())); ci.cancel();
        }
    }
}
