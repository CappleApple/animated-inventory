package com.cappleapple.animatedinventory.api.animation;
/** Java 17 equivalents for bounded animation channels. */
public final class Clamp {
    public static double value(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
    public static float value(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
    public static int value(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private Clamp() { }
}
