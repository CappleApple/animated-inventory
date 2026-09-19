package com.cappleapple.animatedinventory.mixin;

import com.cappleapple.animatedinventory.api.animation.Bounds;
import com.cappleapple.animatedinventory.client.*;
import com.cappleapple.animatedinventory.client.animation.ItemAnimation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(AbstractContainerScreen.class)
abstract class ContainerScreenMixin {
    @Shadow private void extractSlotHighlightFront(GuiGraphicsExtractor graphics) { }

    @WrapOperation(method = "extractContents", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractSlotHighlightBack(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
    private void animatedinventory$highlightBehindItems(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics, Operation<Void> original) {
        original.call(screen, graphics);
        if (ClientRuntime.INSTANCE.enabled()) extractSlotHighlightFront(graphics);
    }
    @WrapOperation(method = "extractContents", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractSlotHighlightFront(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
    private void animatedinventory$frontAlreadyExtracted(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics, Operation<Void> original) {
        if (!ClientRuntime.INSTANCE.enabled()) original.call(screen, graphics);
    }
    @WrapMethod(method = "extractSlots")
    private void animatedinventory$elevatedItems(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Operation<Void> original) {
        if (!ClientRuntime.INSTANCE.enabled()) { original.call(graphics, mouseX, mouseY); return; }
        com.cappleapple.animatedinventory.client.render.NativeSlotRenderer.withElevatedLayer(graphics,
            () -> original.call(graphics, mouseX, mouseY));
    }
    @WrapMethod(method = "extractSlot")
    private void animatedinventory$observeNativeSlot(GuiGraphicsExtractor graphics, Slot slot, int x, int y, Operation<Void> original) {
        ClientRuntime.INSTANCE.observeNativeSlot((AbstractContainerScreen<?>)(Object)this, slot);
        original.call(graphics, slot, x, y);
    }
    @WrapMethod(method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V")
    private void animatedinventory$transaction(Slot slot, int index, int button, ContainerInput type, Operation<Void> original) {
        var runtime = ClientRuntime.INSTANCE;
        runtime.beforeInteraction((AbstractContainerScreen<?>)(Object)this, slot, type);
        original.call(slot, index, button, type);
        runtime.afterInteraction();
    }
    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void animatedinventory$click(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        ClientRuntime.INSTANCE.clickPre((AbstractContainerScreen<?>)(Object)this, event.x(), event.y());
    }
    @Inject(method = "extractRenderState", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractCarriedItem(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"))
    private void animatedinventory$foreground(GuiGraphicsExtractor graphics, int x, int y, float delta, CallbackInfo ci) {
        var runtime = ClientRuntime.INSTANCE;
        graphics.nextStratum();
        runtime.foreground(graphics, (AbstractContainerScreen<?>)(Object)this);
        runtime.screens.finishIfNeeded(graphics);
    }
    @WrapOperation(method = "extractSlot", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack animatedinventory$pendingStack(Slot slot, Operation<ItemStack> original) {
        return ClientRuntime.INSTANCE.pendingStack((AbstractContainerScreen<?>)(Object)this, slot, original.call(slot));
    }
    @WrapOperation(method = {"extractSlot", "renderSlotContents"}, require = 1, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;item(Lnet/minecraft/world/item/ItemStack;III)V"))
    private void animatedinventory$normalItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int seed, Operation<Void> original,
        @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) Slot slot,
        @com.llamalad7.mixinextras.sugar.Local String count) {
        com.cappleapple.animatedinventory.client.render.NativeSlotRenderer.draw(graphics, (AbstractContainerScreen<?>)(Object)this,
            slot, stack, count, (output, rendered) -> original.call(output, rendered, x, y, seed));
    }
    @WrapOperation(method = {"extractSlot", "renderSlotContents"}, require = 1, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fakeItem(Lnet/minecraft/world/item/ItemStack;III)V"))
    private void animatedinventory$normalFakeItem(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int seed, Operation<Void> original,
        @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) Slot slot,
        @com.llamalad7.mixinextras.sugar.Local String count) {
        com.cappleapple.animatedinventory.client.render.NativeSlotRenderer.draw(graphics, (AbstractContainerScreen<?>)(Object)this,
            slot, stack, count, (output, rendered) -> original.call(output, rendered, x, y, seed));
    }
    @WrapOperation(method = {"extractSlot", "renderSlotContents"}, require = 1, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"))
    private void animatedinventory$normalDecorations(GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font, ItemStack stack,
        int x, int y, String count, Operation<Void> original, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) Slot slot) {
        com.cappleapple.animatedinventory.client.render.NativeSlotRenderer.draw(graphics, (AbstractContainerScreen<?>)(Object)this,
            slot, stack, count, (output, rendered) -> original.call(output, font, rendered, x, y, count));
    }
    @WrapMethod(method = "extractFloatingItem")
    private void animatedinventory$cursor(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, String count, Operation<Void> original) {
        var runtime = ClientRuntime.INSTANCE;
        var screen = (AbstractContainerScreen<?>)(Object)this;
        if (!runtime.enabled() || count != null || !ItemStack.matches(stack, screen.getMenu().getCarried())) {
            original.call(graphics, stack, x, y, count); return;
        }
        ItemStack rendered = runtime.normalStack(screen, "cursor", stack);
        ItemAnimation animation = runtime.animations.destination("cursor", System.nanoTime());
        graphics.pose().pushMatrix();
        try {
            if (animation != null && animation.inline) {
                Bounds b = animation.bounds(System.nanoTime());
                graphics.pose().translate((float)(b.x() - x), (float)(b.y() - y));
            }
            original.call(graphics, rendered, x, y, count);
        } finally { graphics.pose().popMatrix(); }
    }
}
