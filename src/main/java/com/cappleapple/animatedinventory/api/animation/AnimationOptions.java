package com.cappleapple.animatedinventory.api.animation;

/** Null options on a transition select the current client config. Durations are before global speed. */
public record AnimationOptions(long durationMs, Easing easing, MovementStyle style, double arcHeight,
                               double startScale, double endScale, double startAlpha, double endAlpha,
                               double startRotation, double endRotation, double zOrder, boolean followCursor) {
    public AnimationOptions {
        if (durationMs < 1 || durationMs > 60_000 || easing == null || style == null
                || !Double.isFinite(arcHeight) || !Double.isFinite(startScale) || !Double.isFinite(endScale)
                || startScale < 0 || endScale < 0 || !Double.isFinite(startAlpha) || !Double.isFinite(endAlpha)
                || startAlpha < 0 || startAlpha > 1 || endAlpha < 0 || endAlpha > 1
                || !Double.isFinite(startRotation) || !Double.isFinite(endRotation) || !Double.isFinite(zOrder)) {
            throw new IllegalArgumentException("Invalid animation options");
        }
    }
    public static AnimationOptions move(long duration, Easing easing, MovementStyle style) {
        return new AnimationOptions(duration, easing, style, 8, 1, 1, 1, 1, 0, 0, 150, false);
    }
}
