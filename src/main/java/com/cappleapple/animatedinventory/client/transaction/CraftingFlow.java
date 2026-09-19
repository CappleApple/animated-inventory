package com.cappleapple.animatedinventory.client.transaction;

import com.cappleapple.animatedinventory.api.animation.TransitionType;
import com.cappleapple.animatedinventory.api.inventory.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Describes consumed ingredient visuals after an actual ResultSlot take; never guesses from recipe previews. */
public final class CraftingFlow {
    public record Consumption(String sourceId, String resultId, ItemStack ingredient, ItemStack product) {
        public Consumption { ingredient = ingredient.copy(); product = product.copy(); }
    }
    public static InventoryVisualTransaction compare(TransactionInference inference, InventoryVisualSnapshot before,
            InventoryVisualSnapshot after, List<Consumption> consumed, String id, boolean quick, int budget) {
        if (consumed.isEmpty() || before.owner() != after.owner() || before.layoutRevision() != after.layoutRevision())
            return inference.compare(before, after, id, quick, budget);
        Map<String, VisualItem> baseline = new LinkedHashMap<>(before.items());
        Set<String> sources = new HashSet<>();
        Map<String, Consumption> products = new LinkedHashMap<>();
        for (Consumption c : consumed) { sources.add(c.sourceId()); products.putIfAbsent(c.resultId(), c); }
        for (String source : sources) {
            VisualItem old = baseline.get(source), next = after.items().get(source);
            if (old == null) continue;
            // Remainders (including buckets and reusable tools) stay in the normal grid renderer.
            // Consumed units get their own decorative flow rather than a second generic disappearance.
            baseline.put(source, next == null ? withCount(old, 0) : next);
        }
        // A regenerated result preview is not another acquired item. Attribute actual product gains to its result slot.
        for (Consumption c : products.values()) {
            VisualItem result = before.items().get(c.resultId());
            if (result == null) continue;
            long gained = 0;
            for (VisualItem next : after.items().values()) {
                if (sources.contains(next.id()) || products.containsKey(next.id()) || !matching(c.product(), next)
                        || !next.mayAnimate() || !next.visible() && !next.offscreenDestination()) continue;
                VisualItem old = before.items().get(next.id());
                int kept = matching(next.stack(), old) ? Math.min(old.stack().getCount(), next.stack().getCount()) : 0;
                gained += next.stack().getCount() - kept;
            }
            VisualItem preview = after.items().get(c.resultId());
            int stays = matching(c.product(), preview) ? preview.stack().getCount() : 0;
            if (gained > 0) baseline.put(c.resultId(), withCount(result, (int)Math.min(Integer.MAX_VALUE, gained + stays)));
        }
        var adjusted = new InventoryVisualSnapshot(before.owner(), before.providerId(), before.layoutRevision(), before.virtualized(), baseline);
        var ordinary = inference.compare(adjusted, after, id, quick, budget);
        List<ItemTransition> transitions = new ArrayList<>(ordinary.transitions());
        List<Consumption> grouped = new ArrayList<>();
        for (Consumption c : consumed) {
            int found = -1;
            for (int i = 0; i < grouped.size(); i++) {
                Consumption existing = grouped.get(i);
                if (existing.sourceId().equals(c.sourceId()) && existing.resultId().equals(c.resultId())
                        && ItemStack.isSameItemSameTags(existing.ingredient(), c.ingredient())) { found = i; break; }
            }
            if (found < 0) grouped.add(c);
            else {
                Consumption old = grouped.get(found);
                grouped.set(found, new Consumption(old.sourceId(), old.resultId(),
                        old.ingredient().copyWithCount((int)Math.min(Integer.MAX_VALUE,
                                (long)old.ingredient().getCount() + c.ingredient().getCount())), old.product()));
            }
        }
        for (Consumption c : grouped) {
            if (transitions.size() >= budget) break;
            VisualItem source = before.items().get(c.sourceId()), result = before.items().get(c.resultId());
            if (source == null || result == null || !source.visible() || !source.mayAnimate() || c.ingredient().isEmpty()) continue;
            ItemTransition productMove = ordinary.transitions().stream()
                    .filter(t -> c.resultId().equals(t.sourceId()) && t.destinationId() != null
                            && ItemStack.isSameItemSameTags(t.stack(), c.product()))
                    .max(Comparator.comparingInt(t -> "cursor".equals(t.destinationId()) ? Integer.MAX_VALUE : t.stack().getCount()))
                    .orElse(null);
            String destinationId = productMove == null ? c.resultId() : productMove.destinationId();
            var destination = productMove == null ? result.bounds() : productMove.destination();
            transitions.add(new ItemTransition(TransitionType.CRAFT, source.id(), destinationId,
                    c.ingredient(), source.bounds(), destination, null, null));
        }
        return new InventoryVisualTransaction(after.owner(), id, quick, transitions);
    }
    private static boolean matching(ItemStack stack, VisualItem item) {
        return item != null && ItemStack.isSameItemSameTags(stack, item.stack());
    }
    private static VisualItem withCount(VisualItem item, int count) {
        return new VisualItem(item.id(), item.stack().copyWithCount(count), item.bounds(), item.region(), item.visible(),
                item.mayAnimate(), item.equipment(), item.clipRegion(), item.destinationBounds(), item.transactionId(), item.overrides(), item.sourceBounds());
    }
    private CraftingFlow() { }
}
