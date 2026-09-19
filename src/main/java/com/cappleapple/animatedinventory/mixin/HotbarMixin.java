package com.cappleapple.animatedinventory.mixin;

import com.cappleapple.animatedinventory.client.animation.HotbarAnimation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Gui.class)
abstract class HotbarMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = "render", at = @At("TAIL"))
    private void animatedinventory$hud(GuiGraphics graphics, float partialTick, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        com.cappleapple.animatedinventory.client.ClientRuntime.INSTANCE.hud(graphics);
    }

    /** Translate only the existing selection sprite; no duplicated hotbar or input interception. */
    @WrapOperation(method = "renderHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"))
    private void animatedinventory$selection(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int u, int v, int width, int height, Operation<Void> original) {
        if (!sprite.getNamespace().equals("minecraft") || !sprite.getPath().equals("textures/gui/widgets.png") || u != 0 || v != 22 || width != 24 || height != 22) {
            original.call(graphics, sprite, x, y, u, v, width, height); return;
        }
        double current = HotbarAnimation.position(x, graphics.guiWidth(), System.nanoTime());
        graphics.pose().pushPose();
        try { graphics.pose().translate(current - x, 0, 0); original.call(graphics, sprite, x, y, u, v, width, height); }
        finally { graphics.pose().popPose(); }
    }
}
