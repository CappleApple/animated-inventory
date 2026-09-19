package com.cappleapple.animatedinventory.mixin;
import com.cappleapple.animatedinventory.client.ClientRuntime;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Screen.class)
abstract class ScreenLifecycleMixin {
    @Inject(method = "init(Lnet/minecraft/client/Minecraft;II)V", at = @At("TAIL")) private void animatedinventory$init(CallbackInfo ci) { ClientRuntime.INSTANCE.init((Screen)(Object)this); }
    @Inject(method = "renderWithTooltip", at = @At("HEAD")) private void animatedinventory$render(GuiGraphics graphics, int x, int y, float partialTick, CallbackInfo ci) { ClientRuntime.INSTANCE.renderPre((Screen)(Object)this, x, y); }
}
