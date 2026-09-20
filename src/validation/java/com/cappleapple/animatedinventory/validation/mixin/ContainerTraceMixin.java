package com.cappleapple.animatedinventory.validation.mixin;

import com.cappleapple.animatedinventory.validation.RenderTrace;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
abstract class ContainerTraceMixin {
    @Inject(method="render",at=@At("HEAD"))
    private void validation$begin(GuiGraphics graphics,int mouseX,int mouseY,float partial,CallbackInfo ci) { RenderTrace.calls.clear(); }
    @Inject(method="renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;IIII)V",at=@At("HEAD"),remap=false)
    private static void validation$highlight(GuiGraphics graphics,int x,int y,int z,int color,CallbackInfo ci) { RenderTrace.calls.add("H:"+x+":"+y); }
}
