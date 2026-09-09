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
    /** Translate only the existing selection sprite; no duplicated hotbar or input interception. */
    @WrapOperation(method = "renderItemHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void animatedinventory$selection(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height, Operation<Void> original) {
        if (!sprite.getNamespace().equals("minecraft") || !sprite.getPath().equals("hud/hotbar_selection")) {
            original.call(graphics, sprite, x, y, width, height); return;
        }
        double current = HotbarAnimation.position(x, graphics.guiWidth(), System.nanoTime());
        graphics.pose().pushPose();
        try { graphics.pose().translate(current - x, 0, 0); original.call(graphics, sprite, x, y, width, height); }
        finally { graphics.pose().popPose(); }
    }
}
