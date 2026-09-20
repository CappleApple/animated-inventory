package com.cappleapple.animatedinventory.validation.mixin;
import com.cappleapple.animatedinventory.validation.ClientSmoke;
import net.minecraft.client.gui.GuiGraphics; import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin; import org.spongepowered.asm.mixin.injection.*; import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GuiGraphics.class) abstract class HotbarRenderProbe {
 @Inject(method="blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V", at=@At("HEAD")) private void animatedinventory$selector(ResourceLocation sprite, int x, int y, int width, int height, CallbackInfo ci) {
  if (sprite.getPath().equals("hud/hotbar_selection")) ClientSmoke.hotbar(((GuiGraphics)(Object)this).pose().last().pose().m30());
 }
}
