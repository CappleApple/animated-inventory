package com.cappleapple.animatedinventory.mixin;

import com.cappleapple.animatedinventory.client.render.TextureCompositor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PictureInPictureRenderer.class)
abstract class PictureInPictureMixin {
    @WrapOperation(method = "blitTexture", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/state/gui/GuiRenderState;addBlitToCurrentLayer(Lnet/minecraft/client/renderer/state/gui/BlitRenderState;)V"))
    private void animatedinventory$pictureEffect(GuiRenderState state, BlitRenderState blit, Operation<Void> original,
                                                @Local(argsOnly = true) PictureInPictureRenderState picture) {
        original.call(state, TextureCompositor.pictureBlit(blit, picture));
    }
}
