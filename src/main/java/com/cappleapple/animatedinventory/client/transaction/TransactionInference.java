package com.cappleapple.animatedinventory.client.transaction;

import com.cappleapple.animatedinventory.api.animation.*;
import com.cappleapple.animatedinventory.api.inventory.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Quantity-conserving comparison. Same identity is reserved first, then compatible deltas are matched. */
public final class TransactionInference {
    private static final class Amount {
        final VisualItem item;
        int count;
        Amount(VisualItem item, int count) { this.item = item; this.count = count; }
    }
    public InventoryVisualTransaction compare(InventoryVisualSnapshot before, InventoryVisualSnapshot after,
                                               String id, boolean quickMove, int budget) {
        List<ItemTransition> result = new ArrayList<>();
        if (before.owner() != after.owner() || !before.providerId().equals(after.providerId()))
            return new InventoryVisualTransaction(after.owner(), id, quickMove, result);
        // Virtualized layouts cannot imply item acquisition, consumption, or movement.
        if ((before.virtualized() || after.virtualized()) && before.layoutRevision() != after.layoutRevision())
            return new InventoryVisualTransaction(after.owner(), id, quickMove, result);
        List<Amount> losses = new ArrayList<>(), gains = new ArrayList<>();
        for (VisualItem old : before.items().values()) {
            VisualItem next = after.items().get(old.id());
            int kept = compatible(old, next) ? Math.min(old.stack().getCount(), next.stack().getCount()) : 0;
            if ((eligible(old) || old.offscreenSource()) && old.stack().getCount() > kept) losses.add(new Amount(old, old.stack().getCount() - kept));
            if (kept > 0 && eligible(old) && eligible(next) && !old.id().equals("cursor")
                    && old.stack().getCount() == next.stack().getCount() && !old.bounds().equals(next.bounds()) && result.size() < budget) {
                result.add(transition(TransitionType.LAYOUT_REFLOW, old, next, kept));
            }
        }
        for (VisualItem next : after.items().values()) {
            VisualItem old = before.items().get(next.id());
            int kept = compatible(old, next) ? Math.min(old.stack().getCount(), next.stack().getCount()) : 0;
            if ((eligible(next) || next.offscreenDestination()) && next.stack().getCount() > kept) gains.add(new Amount(next, next.stack().getCount() - kept));
        }
        // Bound work for adversarial/custom inventories independently of the visual animation cap.
        int comparisons = 0;
        for (Amount loss : losses) {
            while (loss.count > 0 && result.size() < budget) {
                Amount best = null; double bestCost = Double.MAX_VALUE;
                for (Amount gain : gains) {
                    if (++comparisons > 65_536) return new InventoryVisualTransaction(after.owner(), id, quickMove, List.of());
                    if (gain.count == 0 || !compatible(loss.item, gain.item) || !loss.item.visible() && !gain.item.visible()) continue;
                    double dx = loss.item.bounds().centerX() - gain.item.bounds().centerX();
                    double dy = loss.item.bounds().centerY() - gain.item.bounds().centerY();
                    double cost = (gain.count == loss.count ? 0 : 1_000_000) + dx * dx + dy * dy;
                    if (cost < bestCost) { best = gain; bestCost = cost; }
                }
                if (best == null) break;
                int moved = Math.min(loss.count, best.count);
                VisualItem previousDestination = before.items().get(best.item.id());
                VisualItem newSource = after.items().get(loss.item.id());
                TransitionType type = loss.item.offscreenSource() ? TransitionType.RETRIEVE : best.item.offscreenDestination() ? TransitionType.STOW : compatible(previousDestination, best.item) && !previousDestination.stack().isEmpty() ? TransitionType.MERGE
                        : best.item.equipment() ? TransitionType.EQUIP : loss.item.equipment() ? TransitionType.UNEQUIP
                        : previousDestination != null && !previousDestination.stack().isEmpty() && newSource != null
                            && !newSource.stack().isEmpty() && !compatible(loss.item, newSource) ? TransitionType.SWAP
                        : moved < loss.item.stack().getCount() ? TransitionType.SPLIT : TransitionType.MOVE;
                result.add(transition(type, loss.item, best.item, moved));
                loss.count -= moved; best.count -= moved;
            }
        }
        for (Amount gain : gains) if (gain.item.visible() && gain.count > 0 && result.size() < budget) {
            VisualItem old = before.items().get(gain.item.id());
            TransitionType type = compatible(old, gain.item) && !old.stack().isEmpty() ? TransitionType.COUNT_CHANGE : TransitionType.APPEAR;
            result.add(transition(type, null, gain.item, gain.count));
        }
        for (Amount loss : losses) if (loss.item.visible() && loss.count > 0 && result.size() < budget) {
            // A residual count change gets a pulse, not a second fading copy on top of a live stack.
            VisualItem remaining = after.items().get(loss.item.id());
            if (compatible(loss.item, remaining) && !remaining.stack().isEmpty())
                result.add(transition(TransitionType.COUNT_CHANGE, null, remaining, remaining.stack().getCount()));
            else result.add(transition(TransitionType.DISAPPEAR, loss.item, null, loss.count));
        }
        return new InventoryVisualTransaction(after.owner(), id, quickMove, result);
    }
    private static boolean eligible(VisualItem item) { return item != null && item.visible() && item.mayAnimate() && !item.stack().isEmpty(); }
    private static boolean compatible(VisualItem a, VisualItem b) {
        return a != null && b != null && ItemStack.isSameItemSameTags(a.stack(), b.stack());
    }
    private static ItemTransition transition(TransitionType type, VisualItem from, VisualItem to, int count) {
        VisualItem item = to == null ? from : to;
        Bounds source = from == null ? item.bounds() : from.offscreenSource() ? from.sourceBounds() : from.bounds();
        Bounds destination = to == null ? (from.destinationBounds() == null ? source : from.destinationBounds()) : to.offscreenDestination() ? to.destinationBounds() : to.bounds();
        Bounds clip = from == null ? item.clipRegion() : from.clipRegion();
        if (to != null && to.clipRegion() != null) clip = clip == null ? to.clipRegion() : clip.intersect(to.clipRegion());
        return new ItemTransition(type, from == null ? null : from.id(), to == null ? null : to.id(),
                item.stack().copyWithCount(count), source, destination, clip, item.overrides());
    }
}
