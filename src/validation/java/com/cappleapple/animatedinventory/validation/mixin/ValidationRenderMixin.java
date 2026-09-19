package com.cappleapple.animatedinventory.validation.mixin;
import com.cappleapple.animatedinventory.validation.ClientSmoke;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Screen.class)
abstract class ValidationRenderMixin {
    @Inject(method = "renderWithTooltip", at = @At("TAIL"))
    private void animatedinventory$render(GuiGraphics graphics, int x, int y, float delta, CallbackInfo ci) { ClientSmoke.render(graphics); }
}
