package com.cappleapple.animatedinventory.api.animation;

/** All easing inputs are clamped; only BACK_OUT deliberately overshoots. */
public enum Easing {
    LINEAR, EASE_IN_QUAD, EASE_OUT_QUAD, EASE_IN_OUT_QUAD, EASE_OUT_CUBIC,
    EASE_IN_OUT_CUBIC, EASE_OUT_QUART, EASE_OUT_QUINT, BACK_OUT, SMOOTHSTEP, SMOOTHERSTEP;

    public double apply(double progress) {
        double t = net.minecraft.util.Mth.clamp(progress, 0, 1);
        double u = 1 - t;
        return switch (this) {
            case LINEAR -> t;
            case EASE_IN_QUAD -> t * t;
            case EASE_OUT_QUAD -> 1 - u * u;
            case EASE_IN_OUT_QUAD -> t < .5 ? 2 * t * t : 1 - 2 * u * u;
            case EASE_OUT_CUBIC -> 1 - u * u * u;
            case EASE_IN_OUT_CUBIC -> t < .5 ? 4 * t * t * t : 1 - 4 * u * u * u;
            case EASE_OUT_QUART -> 1 - u * u * u * u;
            case EASE_OUT_QUINT -> 1 - u * u * u * u * u;
            case BACK_OUT -> 1 - 2.70158 * u * u * u + 1.70158 * u * u;
            case SMOOTHSTEP -> t * t * (3 - 2 * t);
            case SMOOTHERSTEP -> t * t * t * (t * (6 * t - 15) + 10);
        };
    }
}
