package com.cappleapple.animatedinventory.api.animation;

public enum MovementStyle implements net.neoforged.neoforge.common.TranslatableEnum {
    LINEAR, SMOOTH, ARC, SPRING, SNAP_SMOOTH;

    public double progress(double time, Easing easing) {
        return switch (this) {
            case LINEAR -> Easing.LINEAR.apply(time);
            case SMOOTH, ARC -> easing.apply(time);
            case SPRING -> Easing.BACK_OUT.apply(time);
            case SNAP_SMOOTH -> Easing.EASE_OUT_QUINT.apply(time);
        };
    }
    public Bounds sample(Bounds from, Bounds to, double time, Easing easing, double arc) {
        Bounds result = from.interpolate(to, progress(time, easing));
        double t = Math.clamp(time, 0, 1);
        return this == ARC ? result.offset(0, -4 * arc * t * (1 - t)) : result;
    }
    @Override public net.minecraft.network.chat.Component getTranslatedName() { return net.minecraft.network.chat.Component.translatable("animatedinventory.configuration.value." + name().toLowerCase(java.util.Locale.ROOT)); }
}
