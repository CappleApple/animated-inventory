package com.cappleapple.animatedinventory.mixin;
import com.cappleapple.animatedinventory.client.ClientRuntime;
import com.cappleapple.animatedinventory.client.animation.HotbarAnimation;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Hud.class)
abstract class HotbarMixin {
    @WrapOperation(method = "extractItemHotbar", at = {
        @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"),
        @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V")
    }, require = 1)
    private void animatedinventory$selection(GuiGraphicsExtractor graphics, @Coerce Object pipeline, Identifier sprite,
        int x, int y, int width, int height, Operation<Void> original) {
        if (!sprite.getNamespace().equals("minecraft") || !sprite.getPath().equals("hud/hotbar_selection")) {
            original.call(graphics, pipeline, sprite, x, y, width, height); return;
        }
        double current = HotbarAnimation.position(x, graphics.guiWidth(), System.nanoTime());
        graphics.pose().pushMatrix();
        try { graphics.pose().translate((float)(current - x), 0); original.call(graphics, pipeline, sprite, x, y, width, height); }
        finally { graphics.pose().popMatrix(); }
    }
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void animatedinventory$exit(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        ClientRuntime.INSTANCE.hud(graphics);
    }
}
