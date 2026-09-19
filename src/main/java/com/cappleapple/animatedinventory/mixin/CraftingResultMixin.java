package com.cappleapple.animatedinventory.mixin;

import com.cappleapple.animatedinventory.client.ClientRuntime;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ResultSlot.class)
abstract class CraftingResultMixin {
    @Shadow @Final private Player player;

    /** Observe exactly what vanilla removed, including recipe remainders; do not replay or alter crafting. */
    @WrapOperation(method = "onTake", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/CraftingContainer;removeItem(II)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack animatedinventory$consumed(CraftingContainer grid, int index, int count, Operation<ItemStack> original) {
        ItemStack removed = original.call(grid, index, count);
        if (player.level().isClientSide()) {
            try { ClientRuntime.INSTANCE.craftedIngredient(player, (Slot)(Object)this, grid, index, removed); }
            catch (RuntimeException | LinkageError error) { ClientRuntime.INSTANCE.fail(error); }
        }
        return removed;
    }
}
