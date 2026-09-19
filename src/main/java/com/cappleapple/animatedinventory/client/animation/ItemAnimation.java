package com.cappleapple.animatedinventory.client.animation;

import com.cappleapple.animatedinventory.api.animation.*;
import com.cappleapple.animatedinventory.api.inventory.ItemTransition;

/** Independent position, scale, alpha, and rotation channels. Emphasis composes at render time. */
public final class ItemAnimation {
    public final long handle, owner;
    public final String transactionId;
    public final ItemTransition transition;
    public final AnimationOptions options;
    public final Bounds source;
    public Bounds destination;
    public final long start;
    public long duration;
    public final boolean inline;
    public ItemAnimation(long handle, long owner, String transactionId, ItemTransition transition, AnimationOptions options,
                         Bounds source, long start, long duration, boolean inline) {
        this.handle = handle; this.owner = owner; this.transactionId = transactionId; this.transition = transition;
        this.options = options; this.source = source; this.destination = transition.destination();
        this.start = start; this.duration = duration; this.inline = inline;
    }
    public double progress(long now) { return net.minecraft.util.Mth.clamp((double)(now - start) / duration, 0, 1); }
    public boolean finished(long now) { return now - start >= duration; }
    public Bounds bounds(long now) { return options.style().sample(source, destination, progress(now), options.easing(), options.arcHeight()); }
    public double scale(long now) { return channel(options.startScale(), options.endScale(), now); }
    public double alpha(long now) {
        double edgeFade = (transition.type() == TransitionType.STOW || transition.type() == TransitionType.CRAFT) ? 1 - net.minecraft.util.Mth.clamp((progress(now) - .65) / .35, 0, 1) : 1;
        double arrivalFade = transition.type() == TransitionType.RETRIEVE ? net.minecraft.util.Mth.clamp(progress(now) / .25, 0, 1) : 1;
        return net.minecraft.util.Mth.clamp(channel(options.startAlpha(), options.endAlpha(), now), 0, 1) * edgeFade * arrivalFade;
    }
    public double rotation(long now) { return channel(options.startRotation(), options.endRotation(), now); }
    private double channel(double from, double to, long now) { return from + (to - from) * options.easing().apply(progress(now)); }
}
