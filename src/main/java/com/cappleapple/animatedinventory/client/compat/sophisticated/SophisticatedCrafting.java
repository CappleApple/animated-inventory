package com.cappleapple.animatedinventory.client.compat.sophisticated;

import com.cappleapple.animatedinventory.api.inventory.*;
import com.cappleapple.animatedinventory.client.VanillaInventoryProvider;
import com.cappleapple.animatedinventory.client.transaction.CraftingFlow;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Visual ingredient usage after a predicted result take. Core remains responsible for server consumption/refill. */
public final class SophisticatedCrafting {
    public record Take(InventoryVisualSnapshot before, String resultId, List<String> inputs) {
        public Take { inputs = List.copyOf(inputs); }
        private long productCount(InventoryVisualSnapshot snapshot, ItemStack product) {
            return snapshot.items().values().stream().filter(item -> !item.id().equals(resultId) && !inputs.contains(item.id())
                    && item.mayAnimate() && (item.visible() || item.offscreenDestination())
                    && ItemStack.isSameItemSameComponents(product, item.stack())).mapToLong(item -> item.stack().getCount()).sum();
        }
        public List<CraftingFlow.Consumption> observe(InventoryVisualSnapshot after) {
            if (before.owner() != after.owner() || before.layoutRevision() != after.layoutRevision()
                    || !before.providerId().equals(after.providerId())) return List.of();
            VisualItem result = before.items().get(resultId);
            if (result == null || result.stack().isEmpty()) return List.of();
            long gained = productCount(after, result.stack()) - productCount(before, result.stack());
            int crafts = (int)Math.min(1024, gained / result.stack().getCount());
            if (crafts <= 0) return List.of();
            List<CraftingFlow.Consumption> uses = new ArrayList<>();
            for (String id : inputs) {
                VisualItem input = before.items().get(id);
                if (input != null && input.visible() && input.mayAnimate() && !input.stack().isEmpty())
                    uses.add(new CraftingFlow.Consumption(id, resultId, input.stack().copyWithCount(crafts), result.stack()));
            }
            return uses;
        }
    }
    public static Take prepare(AbstractContainerScreen<?> screen, Slot result, ContainerInput type, InventoryVisualSnapshot before) {
        if (!(screen instanceof SophisticatedView) || !(result instanceof ResultSlot)
                || type != ContainerInput.PICKUP && type != ContainerInput.QUICK_MOVE && type != ContainerInput.SWAP) return null;
        try {
            Object container = ((Optional<?>)screen.getMenu().getClass().getMethod("getSlotUpgradeContainer", Slot.class)
                    .invoke(screen.getMenu(), result)).orElse(null);
            if (container == null || !container.getClass().getName().equals("net.p3pp3rf1y.sophisticatedcore.upgrades.crafting.CraftingUpgradeContainer")) return null;
            List<?> slots = (List<?>)container.getClass().getMethod("getRecipeSlots").invoke(container);
            if (slots.size() != 9 || slots.stream().anyMatch(s -> !(s instanceof Slot))) return null;
            return new Take(before, VanillaInventoryProvider.id(result), slots.stream().map(s -> VanillaInventoryProvider.id((Slot)s)).toList());
        } catch (ReflectiveOperationException | LinkageError error) {
            // Optional API unavailable: ordinary inventory animation still works.
            return null;
        }
    }
    private SophisticatedCrafting() { }
}
