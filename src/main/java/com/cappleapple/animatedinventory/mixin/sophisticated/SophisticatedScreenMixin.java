package com.cappleapple.animatedinventory.mixin.sophisticated;

import com.cappleapple.animatedinventory.api.animation.Bounds;
import com.cappleapple.animatedinventory.client.*;
import com.cappleapple.animatedinventory.client.compat.sophisticated.*;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import java.util.Arrays;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase", remap = false)
abstract class SophisticatedScreenMixin extends AbstractContainerScreen<AbstractContainerMenu> implements SophisticatedView {
    protected SophisticatedScreenMixin(AbstractContainerMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    @Shadow private int getNumberOfVisibleRows() { throw new AssertionError(); }
    @Shadow public abstract int getSlotsOnLine();
    @Shadow protected abstract boolean isHovering(Slot slot, double mouseX, double mouseY);
    @Shadow private boolean isStorageSlotRenderReplaced(int index) { throw new AssertionError(); }

    @Unique private static final ClassValue<Boolean> animatedinventory$filterTypes = new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type) {
            if (type.getName().equals("net.p3pp3rf1y.sophisticatedcore.common.gui.IFilterSlot")) return true;
            return Arrays.stream(type.getInterfaces()).anyMatch(animatedinventory$filterTypes::get)
                    || type.getSuperclass() != null && animatedinventory$filterTypes.get(type.getSuperclass());
        }
    };

    @Override public boolean animatedinventory$visible(Slot slot) {
        if (!slot.isActive() || slot.y < 0 || slot.x <= -1000) return false;
        if (slot.index < ((SophisticatedMenu)menu).getNumberOfStorageInventorySlots()) {
            // The scroll widget rewrites coordinates, including sentinel coordinates outside its viewport.
            return slot.x >= 8 && slot.x + 16 <= 7 + getSlotsOnLine() * 18
                    && slot.y >= 18 && slot.y + 16 <= 17 + getNumberOfVisibleRows() * 18;
        }
        return new Bounds(0, 0, width, height).intersects(Bounds.item(leftPos + slot.x, topPos + slot.y));
    }
    @Override public boolean animatedinventory$allows(Slot slot) {
        return !animatedinventory$filterTypes.get(slot.getClass()) && !((SophisticatedMenu)menu).isInfiniteSlot(slot.index)
                && (slot.index >= ((SophisticatedMenu)menu).getNumberOfStorageInventorySlots()
                    || !((SophisticatedMenu)menu).isInaccessibleSlot(slot.index) && !isStorageSlotRenderReplaced(slot.index));
    }
    @Invoker("renderStack")
    public abstract void animatedinventory$drawStack(GuiGraphics graphics, int x, int y, ItemStack stack, boolean preview, String count);

    @WrapMethod(method = "slotClicked")
    private void animatedinventory$transaction(Slot slot, int index, int button, ClickType type, Operation<Void> original) {
        var runtime = ClientRuntime.INSTANCE;
        runtime.beforeInteraction(this, slot, type);
        original.call(slot, index, button, type);
        runtime.afterInteraction();
    }

    @WrapMethod(method = "renderSlot")
    private void animatedinventory$observe(GuiGraphics graphics, Slot slot, Operation<Void> original) {
        ClientRuntime.INSTANCE.observeNativeSlot(this, slot);
        original.call(graphics, slot);
    }

    @WrapOperation(method = "renderSlot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/Slot;getItem()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack animatedinventory$pendingStack(Slot slot, Operation<ItemStack> original) {
        return ClientRuntime.INSTANCE.pendingStack(this, slot, original.call(slot));
    }

    @WrapOperation(method = "renderSlot", at = @At(value = "INVOKE",
            target = "Lnet/p3pp3rf1y/sophisticatedcore/client/gui/StorageScreenBase;renderStack(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/world/item/ItemStack;ZLjava/lang/String;)V"))
    private void animatedinventory$item(@Coerce Object screen, GuiGraphics graphics, int x, int y, ItemStack stack,
            boolean preview, String count, Operation<Void> original, @Local(argsOnly = true) Slot slot) {
        var runtime = ClientRuntime.INSTANCE;
        if (!runtime.canAnimateSlot(this, slot) || preview || count != null || !ItemStack.matches(stack, slot.getItem())) {
            original.call(screen, graphics, x, y, stack, preview, count); return;
        }
        String id = VanillaInventoryProvider.id(slot);
        ItemStack rendered = runtime.normalStack(this, id, stack);
        long now = System.nanoTime();
        Bounds logical = Bounds.item(leftPos + x, topPos + y);
        double scale = runtime.emphasis.scale(id, logical.contains(runtime.mouseX, runtime.mouseY), now);
        var animation = runtime.animations.destination(id, now);
        graphics.pose().pushPose();
        try {
            if (animation != null && animation.inline) {
                Bounds visual = animation.bounds(now);
                graphics.pose().translate(visual.x() - logical.x(), visual.y() - logical.y(), 0);
                scale *= animation.scale(now);
            }
            graphics.pose().translate(x + 8, y + 8, scale > 1 ? ClientConfig.HOVER_Z.get() : 0);
            graphics.pose().scale((float)scale, (float)scale, 1);
            graphics.pose().translate(-x - 8, -y - 8, 0);
            original.call(screen, graphics, x, y, rendered, false, null);
        } finally { graphics.flush(); graphics.pose().popPose(); }
    }

    @WrapOperation(method = {"renderSuper", "renderUpgradeSlots",
            "renderStorageInventorySlots(Lnet/minecraft/client/gui/GuiGraphics;IIZ)V"},
            at = @At(value = "INVOKE", target = "Lnet/p3pp3rf1y/sophisticatedcore/client/gui/StorageScreenBase;renderSlot(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/inventory/Slot;)V"))
    private void animatedinventory$highlightBehind(@Coerce Object screen, GuiGraphics graphics, Slot slot, Operation<Void> original,
            @Local(argsOnly = true, ordinal = 0) int mouseX, @Local(argsOnly = true, ordinal = 1) int mouseY) {
        if (ClientRuntime.INSTANCE.enabled() && animatedinventory$visible(slot) && !((SophisticatedMenu)menu).isInaccessibleSlot(slot.index) && isHovering(slot, mouseX, mouseY)) {
            renderSlotHighlight(graphics, slot.x, slot.y, 0, getSlotColor(slot.index));
            graphics.flush();
        }
        original.call(screen, graphics, slot);
    }

    @WrapOperation(method = {"renderSuper", "renderUpgradeSlots",
            "renderStorageInventorySlots(Lnet/minecraft/client/gui/GuiGraphics;IIZ)V"},
            at = @At(value = "INVOKE", target = "Lnet/p3pp3rf1y/sophisticatedcore/client/gui/StorageScreenBase;renderSlotHighlight(Lnet/minecraft/client/gui/GuiGraphics;IIII)V"))
    private void animatedinventory$highlightAlreadyDrawn(GuiGraphics graphics, int x, int y, int z, int color, Operation<Void> original) {
        if (!ClientRuntime.INSTANCE.enabled()) original.call(graphics, x, y, z, color);
    }
}
