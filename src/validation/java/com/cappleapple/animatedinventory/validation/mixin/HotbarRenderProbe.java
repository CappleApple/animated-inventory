package com.cappleapple.animatedinventory.validation.mixin;
import com.cappleapple.animatedinventory.validation.ClientSmoke;
import net.minecraft.client.gui.GuiGraphics; import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin; import org.spongepowered.asm.mixin.injection.*; import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GuiGraphics.class) abstract class HotbarRenderProbe {
 @Inject(method="blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V", at=@At("HEAD")) private void animatedinventory$selector(ResourceLocation sprite, int x, int y, int u, int v, int width, int height, CallbackInfo ci) {
  if (sprite.getPath().equals("textures/gui/widgets.png") && u == 0 && v == 22 && width == 24 && height == 22) ClientSmoke.hotbar(((GuiGraphics)(Object)this).pose().last().pose().m30());
 }
}
