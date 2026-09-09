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
    @Shadow protected abstract void renderSlotHighlight(GuiGraphics graphics, Slot slot, int mouseX, int mouseY, float partialTick);

    /** Draw vanilla's own highlight before the native item/decorators, with unchanged logical hit testing. */
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlot(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;)V"))
    private void animatedinventory$highlightBehindItem(AbstractContainerScreen<?> screen, GuiGraphics graphics, Slot slot,
                                                        Operation<Void> original, @Local(argsOnly = true, ordinal = 0) int mouseX,
                                                        @Local(argsOnly = true, ordinal = 1) int mouseY, @Local(argsOnly = true) float partialTick) {
        if (ClientRuntime.INSTANCE.enabled() && ((ContainerScreenAccess)screen).animatedinventory$isHovering(slot, mouseX, mouseY)) {
            renderSlotHighlight(graphics, slot, mouseX, mouseY, partialTick);
            graphics.flush();
        }
        original.call(screen, graphics, slot);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;IIF)V"))
    private void animatedinventory$alreadyDrewHighlight(AbstractContainerScreen<?> screen, GuiGraphics graphics, Slot slot,
                                                       int mouseX, int mouseY, float partialTick, Operation<Void> original) {
        if (!ClientRuntime.INSTANCE.enabled()) original.call(screen, graphics, slot, mouseX, mouseY, partialTick);
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
    /** Keep native quick-craft previews, slot subclasses, item models, and decorators in their owning path. */
    @WrapOperation(method = "renderSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderSlotContents(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/inventory/Slot;Ljava/lang/String;)V"))
    private void animatedinventory$slot(AbstractContainerScreen<?> screen, GuiGraphics graphics, ItemStack stack,
                                         Slot slot, String count, Operation<Void> original) {
        var runtime = ClientRuntime.INSTANCE;
        if (!runtime.canAnimateSlot(screen, slot)) { original.call(screen, graphics, stack, slot, count); return; }
        String id = VanillaInventoryProvider.id(slot);
        // Vanilla drag-distribution preview quantities have separate ownership; never suppress those.
        boolean preview = count != null || !ItemStack.matches(stack, slot.getItem());
        ItemStack rendered = preview ? stack : runtime.normalStack(screen, id, stack);
        long now = System.nanoTime();
        Bounds logical = Bounds.item(screen.getGuiLeft() + slot.x, screen.getGuiTop() + slot.y);
        double scale = runtime.emphasis.scale(id, logical.contains(runtime.mouseX, runtime.mouseY), now);
        ItemAnimation inline = preview ? null : runtime.animations.destination(id, now);
        graphics.pose().pushPose();
        try {
            if (inline != null && inline.inline) {
                Bounds visual = inline.bounds(now);
                graphics.pose().translate(visual.x() - logical.x(), visual.y() - logical.y(), 0);
                scale *= inline.scale(now);
            }
            graphics.pose().translate(slot.x + 8, slot.y + 8, scale > 1 ? ClientConfig.HOVER_Z.get() : 0);
            graphics.pose().scale((float)scale, (float)scale, 1);
            graphics.pose().translate(-slot.x - 8, -slot.y - 8, 0);
            original.call(screen, graphics, rendered, slot, count);
        } finally { graphics.flush(); graphics.pose().popPose(); }
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
                graphics.pose().translate(b.x() - x - screen.getGuiLeft(), b.y() - y - screen.getGuiTop(), 0);
            }
            original.call(graphics, rendered, x, y, count);
        } finally { graphics.flush(); graphics.pose().popPose(); }
    }
}
