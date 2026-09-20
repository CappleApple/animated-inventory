package com.cappleapple.animatedinventory.validation.mixin;
import com.cappleapple.animatedinventory.validation.ClientSmoke; import net.minecraft.client.gui.Gui; import net.minecraft.client.gui.GuiGraphics; import org.spongepowered.asm.mixin.Mixin; import org.spongepowered.asm.mixin.injection.*; import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Gui.class) abstract class HudRenderProbe { @Inject(method="render", at=@At("TAIL")) private void animatedinventory$hud(GuiGraphics graphics, float delta, CallbackInfo ci) { ClientSmoke.renderHud(graphics); } }
