package com.cappleapple.animatedinventory.mixin;

import com.cappleapple.animatedinventory.api.animation.Bounds;
import com.cappleapple.animatedinventory.client.*;
import com.cappleapple.animatedinventory.client.animation.ItemAnimation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractContainerScreen.class)
abstract class ContainerScreenMixin {

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlot(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;)V"))
    private void animatedinventory$highlightBehind(AbstractContainerScreen<?> screen, GuiGraphics graphics, Slot slot, Operation<Void> original,
            @Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 1) int y) {
        if (ClientRuntime.INSTANCE.enabled() && slot.isHighlightable() && ((ContainerScreenAccess)screen).animatedinventory$isHovering(slot, x, y)) {
            AbstractContainerScreen.renderSlotHighlight(graphics, slot.x, slot.y, 0); graphics.flush();
        }
        original.call(screen, graphics, slot);
    }
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;III)V"))
    private void animatedinventory$highlightOnce(GuiGraphics graphics, int x, int y, int z, Operation<Void> original) {
        if (!ClientRuntime.INSTANCE.enabled()) original.call(graphics, x, y, z);
    }
    /** Custom overrides that bypass the base slot renderer never acquire suppression ownership. */
    @WrapMethod(method = "renderSlot")
    private void animatedinventory$observeNativeSlot(GuiGraphics graphics, Slot slot, Operation<Void> original) {
        ClientRuntime.INSTANCE.observeNativeSlot((AbstractContainerScreen<?>)(Object)this, slot);
        original.call(graphics, slot);
    }
    /** Observe actual vanilla prediction immediately; never intercept or replay the transaction. */
    @WrapMethod(method = "slotClicked")
    private void animatedinventory$transaction(Slot slot, int index, int button, ClickType type, Operation<Void> original) {
        var runtime = ClientRuntime.INSTANCE;
        runtime.beforeInteraction((AbstractContainerScreen<?>)(Object)this, slot, type);
        original.call(slot, index, button, type);
        runtime.afterInteraction();
    }
    /** A narrow render boundary enables capture without storing live Screen objects on removal. */
    @WrapMethod(method = "render")
    private void animatedinventory$screen(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, Operation<Void> original) {
        var runtime = ClientRuntime.INSTANCE;
        runtime.ensure((AbstractContainerScreen<?>)(Object)this);
        if (runtime.enabled()) runtime.screens.begin((AbstractContainerScreen<?>)(Object)this, graphics);
        try { original.call(graphics, mouseX, mouseY, partialTick); }
        finally { runtime.screens.finishIfNeeded(graphics); }
    }
    @WrapOperation(method = "renderSlot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack animatedinventory$pendingStack(Slot slot, Operation<ItemStack> original) {
        return ClientRuntime.INSTANCE.pendingStack((AbstractContainerScreen<?>)(Object)this, slot, original.call(slot));
    }
    @org.spongepowered.asm.mixin.Unique private ItemStack animatedinventory$rendered;
    @org.spongepowered.asm.mixin.Unique private boolean animatedinventory$preview;
    @WrapOperation(method = "renderSlot", at = { @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;III)V"), @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderFakeItem(Lnet/minecraft/world/item/ItemStack;III)V") })
    private void animatedinventory$item(GuiGraphics graphics, ItemStack stack, int x, int y, int seed, Operation<Void> original, @Local(argsOnly = true) Slot slot, @Local String count) {
        var screen = (AbstractContainerScreen<?>)(Object)this;
        var runtime = ClientRuntime.INSTANCE;
        animatedinventory$preview = count != null || !ItemStack.matches(stack, slot.getItem());
        animatedinventory$rendered = runtime.canAnimateSlot(screen, slot) && !animatedinventory$preview
                ? runtime.normalStack(screen, VanillaInventoryProvider.id(slot), stack) : stack;
        animatedinventory$pose(graphics, slot);
        try { original.call(graphics, animatedinventory$rendered, x, y, seed); }
        finally { graphics.flush(); graphics.pose().popPose(); }
    }
    @WrapOperation(method = "renderSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"))
    private void animatedinventory$decorations(GuiGraphics graphics, net.minecraft.client.gui.Font font, ItemStack stack, int x, int y, String count, Operation<Void> original, @Local(argsOnly = true) Slot slot) {
        animatedinventory$pose(graphics, slot);
        try { original.call(graphics, font, animatedinventory$rendered == null ? stack : animatedinventory$rendered, x, y, count); }
        finally { graphics.flush(); graphics.pose().popPose(); animatedinventory$rendered = null; }
    }
    @org.spongepowered.asm.mixin.Unique private void animatedinventory$pose(GuiGraphics graphics, Slot slot) {
        graphics.pose().pushPose();
        var screen = (AbstractContainerScreen<?>)(Object)this;
        var runtime = ClientRuntime.INSTANCE;
        if (!runtime.canAnimateSlot(screen, slot)) return;
        var access = (ContainerScreenAccess)screen;
        String id = VanillaInventoryProvider.id(slot);
        Bounds logical = Bounds.item(access.animatedinventory$left() + slot.x, access.animatedinventory$top() + slot.y);
        long now = System.nanoTime();
        double scale = runtime.emphasis.scale(id, logical.contains(runtime.mouseX, runtime.mouseY), now);
        ItemAnimation inline = animatedinventory$preview ? null : runtime.animations.destination(id, now);
        if (inline != null && inline.inline) {
            Bounds visual = inline.bounds(now);
            graphics.pose().translate(visual.x() - logical.x(), visual.y() - logical.y(), 0);
            scale *= inline.scale(now);
        }
        graphics.pose().translate(slot.x + 8, slot.y + 8, scale > 1 ? ClientConfig.HOVER_Z.get() : 0);
        graphics.pose().scale((float)scale, (float)scale, 1);
        graphics.pose().translate(-slot.x - 8, -slot.y - 8, 0);
    }
    @org.spongepowered.asm.mixin.injection.Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V", shift = At.Shift.AFTER))
    private void animatedinventory$foreground(GuiGraphics graphics, int x, int y, float partialTick, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        ClientRuntime.INSTANCE.foreground((AbstractContainerScreen<?>)(Object)this, graphics);
    }
    @WrapMethod(method = "renderFloatingItem")
    private void animatedinventory$cursor(GuiGraphics graphics, ItemStack stack, int x, int y, String count, Operation<Void> original) {
        var runtime = ClientRuntime.INSTANCE;
        var screen = (AbstractContainerScreen<?>)(Object)this;
        if (!runtime.enabled() || count != null || !ItemStack.matches(stack, screen.getMenu().getCarried())) {
            original.call(graphics, stack, x, y, count); return;
        }
        ItemStack rendered = runtime.normalStack(screen, "cursor", stack);
        ItemAnimation animation = runtime.animations.destination("cursor", System.nanoTime());
        graphics.pose().pushPose();
        try {
            if (animation != null && animation.inline) {
                Bounds b = animation.bounds(System.nanoTime());
                graphics.pose().translate(b.x() - x - ((com.cappleapple.animatedinventory.mixin.ContainerScreenAccess)screen).animatedinventory$left(), b.y() - y - ((com.cappleapple.animatedinventory.mixin.ContainerScreenAccess)screen).animatedinventory$top(), 0);
            }
            original.call(graphics, rendered, x, y, count);
        } finally { graphics.flush(); graphics.pose().popPose(); }
    }
}
