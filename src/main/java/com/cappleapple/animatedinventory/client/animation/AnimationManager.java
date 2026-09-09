package com.cappleapple.animatedinventory.client.animation;

import com.cappleapple.animatedinventory.api.animation.*;
import com.cappleapple.animatedinventory.api.inventory.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import java.util.function.Consumer;

/** Owns visual copies only. A suppression claim exists exactly as long as its accepted animation. */
public final class AnimationManager {
    private final List<ItemAnimation> active = new ArrayList<>();
    private final List<ItemAnimation> readOnly = Collections.unmodifiableList(active);
    private long sequence;
    public long cancelled;
    public List<ItemAnimation> active() { return readOnly; }
    public long add(long owner, String transaction, ItemTransition transition, AnimationOptions options,
                    long now, long duration, int cap, boolean inline, Bounds start) {
        if (transition.stack().isEmpty() || active.size() >= cap) return -1;
        Bounds clip = transition.clipRegion();
        if (clip != null && (!clip.intersects(start) || !clip.intersects(transition.destination()))) return -1;
        long id = ++sequence;
        active.add(new ItemAnimation(id, owner, transaction, transition, options, start, now, Math.max(1, duration), inline));
        active.sort(Comparator.comparingDouble(a -> a.options.zOrder()));
        return id;
    }
    public void update(long now, Bounds cursor, Consumer<ItemAnimation> completion) {
        for (Iterator<ItemAnimation> it = active.iterator(); it.hasNext();) {
            ItemAnimation animation = it.next();
            if (animation.options.followCursor()) animation.destination = cursor;
            if (animation.finished(now)) { it.remove(); completion.accept(animation); }
        }
        // Ingredient visuals converge on the product's current visual position, including a moving cursor.
        for (ItemAnimation animation : active) if (animation.transition.type() == TransitionType.CRAFT) {
            Bounds logical = "cursor".equals(animation.transition.destinationId()) ? cursor : animation.transition.destination();
            animation.destination = current(animation.transition.destinationId(), logical, now);
        }
    }
    public ItemAnimation destination(String id, long now) {
        if (id == null) return null;
        for (int i = active.size() - 1; i >= 0; i--) {
            ItemAnimation a = active.get(i);
            if (a.transition.type() != TransitionType.CRAFT && id.equals(a.transition.destinationId()) && !a.finished(now)) return a;
        }
        return null;
    }
    public Bounds current(String id, Bounds fallback, long now) {
        ItemAnimation a = destination(id, now);
        return a == null ? fallback : a.bounds(now);
    }
    public ItemStack normalStack(String id, ItemStack stack, long now) {
        long withheld = 0;
        for (ItemAnimation a : active) if (a.transition.type() != TransitionType.CRAFT && !a.inline && !a.finished(now) && id.equals(a.transition.destinationId())
                && ItemStack.isSameItemSameComponents(stack, a.transition.stack())) withheld += a.transition.stack().getCount();
        return withheld == 0 ? stack : stack.copyWithCount((int)Math.max(0, stack.getCount() - withheld));
    }
    public void cancel(long handle) { if (active.removeIf(a -> a.handle == handle)) cancelled++; }
    public void cancelTouching(Set<String> ids) {
        int size = active.size();
        active.removeIf(a -> (a.transition.sourceId() != null && ids.contains(a.transition.sourceId())) || (a.transition.destinationId() != null && ids.contains(a.transition.destinationId())));
        cancelled += size - active.size();
    }
    public void clear() { cancelled += active.size(); active.clear(); }
}
