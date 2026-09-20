package com.cappleapple.animatedinventory.validation.mixin;

import com.cappleapple.animatedinventory.validation.RenderTrace;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
abstract class GuiTraceMixin {
    @Inject(method="renderItem(Lnet/minecraft/world/item/ItemStack;III)V",at=@At("HEAD"))
    private void validation$item(ItemStack stack,int x,int y,int seed,CallbackInfo ci) { RenderTrace.calls.add("I:"+x+":"+y); }
    @Inject(method="blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V",at=@At("HEAD"))
    private void validation$selector(ResourceLocation texture,int x,int y,int u,int v,int width,int height,CallbackInfo ci) {
        if(texture.toString().equals("minecraft:textures/gui/widgets.png") && u==0 && v==22 && width==24 && height==22) {
            RenderTrace.selectorLogical=x;
            RenderTrace.selectorVisual=x+((GuiGraphics)(Object)this).pose().last().pose().m30();
            RenderTrace.selectorFrames++;
        }
    }
}
