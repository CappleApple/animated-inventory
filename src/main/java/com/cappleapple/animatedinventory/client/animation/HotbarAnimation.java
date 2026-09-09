package com.cappleapple.animatedinventory.client.animation;

import com.cappleapple.animatedinventory.api.animation.Easing;
import com.cappleapple.animatedinventory.client.ClientConfig;

public final class HotbarAnimation {
    private static final Tween POSITION = new Tween(0);
    private static final Tween SPRING = new Tween(0);
    private static int width = -1;
    public static double position(int logical, int guiWidth, long now) {
        if (!ClientConfig.ENABLED.get() || !ClientConfig.HOTBAR.get() || ClientConfig.REDUCE_MOTION.get() || width != guiWidth) {
            POSITION.reset(logical); SPRING.reset(logical); width = guiWidth; return logical;
        }
        Easing easing = ClientConfig.HOTBAR_EASING.get();
        POSITION.target(logical, now, ClientConfig.nanos(ClientConfig.HOTBAR_MS.get()), easing);
        SPRING.target(logical, now, ClientConfig.nanos(ClientConfig.HOTBAR_MS.get()), Easing.BACK_OUT);
        double strength = ClientConfig.HOTBAR_SPRING.get();
        return POSITION.value(now) * (1 - strength) + SPRING.value(now) * strength;
    }
    public static void reset() { width = -1; }
    private HotbarAnimation() { }
}
