package com.cappleapple.animatedinventory.client.animation;

import com.cappleapple.animatedinventory.api.animation.Easing;

/** Retargets from the current interpolated value, never the previous endpoint. Uses monotonic time. */
public final class Tween {
    private double from, target;
    private long start, duration = 1;
    private Easing easing = Easing.LINEAR;
    public Tween(double value) { from = target = value; }
    public double value(long now) { return from + (target - from) * easing.apply((double)(now - start) / duration); }
    public double target() { return target; }
    public void target(double next, long now, long duration, Easing easing) {
        if (next == target) return;
        from = value(now); target = next; start = now; this.duration = Math.max(1, duration); this.easing = easing;
    }
    public void reset(double value) { from = target = value; }
}
