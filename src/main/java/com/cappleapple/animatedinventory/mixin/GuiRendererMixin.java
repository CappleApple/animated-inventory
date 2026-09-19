package com.cappleapple.animatedinventory.mixin;

import com.cappleapple.animatedinventory.client.render.ItemOpacityLayer;
import com.cappleapple.animatedinventory.client.render.TextureCompositor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiRenderer.class)
abstract class GuiRendererMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void animatedinventory$releaseFrameEffects(CallbackInfo ci) { TextureCompositor.clear(); }

    @WrapOperation(method = "submitBlitFromItemAtlas", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;addBlitToCurrentLayer(Lnet/minecraft/client/renderer/state/gui/BlitRenderState;)V"))
    private void animatedinventory$itemOpacity(GuiRenderState state, BlitRenderState blit, Operation<Void> original,
                                              @Local(argsOnly = true) GuiItemRenderState item) {
        original.call(state, ItemOpacityLayer.itemBlit(blit, item));
    }
}
