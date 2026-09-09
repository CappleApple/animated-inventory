package com.cappleapple.animatedinventory;

import com.cappleapple.animatedinventory.api.animation.*;
import com.cappleapple.animatedinventory.client.animation.Tween;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class AnimationMathTest {
    @ParameterizedTest @EnumSource(Easing.class)
    void easingClampsAndReachesBothEndpoints(Easing easing) {
        assertEquals(0, easing.apply(-2), 1e-9);
        assertEquals(1, easing.apply(3), 1e-9);
        for (int i = 0; i <= 100; i++) assertTrue(Double.isFinite(easing.apply(i / 100.0)));
    }
    @ParameterizedTest @EnumSource(MovementStyle.class)
    void trajectoriesHaveExactEndpoints(MovementStyle style) {
        Bounds from = Bounds.item(3, 9), to = Bounds.item(170, 65);
        assertEquals(from.x(), style.sample(from, to, 0, Easing.EASE_IN_OUT_CUBIC, 8).x(), 1e-9);
        Bounds finalBounds = style.sample(from, to, 1, Easing.EASE_IN_OUT_CUBIC, 8);
        assertEquals(to.x(), finalBounds.x(), 1e-9); assertEquals(to.y(), finalBounds.y(), 1e-9);
    }
    @Test void arcIsAboveDirectPath() {
        Bounds a = Bounds.item(0, 20), b = Bounds.item(100, 20);
        assertEquals(12, MovementStyle.ARC.sample(a, b, .5, Easing.LINEAR, 8).y());
    }
    @Test void springOvershootsThenSettles() { assertTrue(Easing.BACK_OUT.apply(.7) > 1); assertEquals(1, Easing.BACK_OUT.apply(1)); }
    @Test void retargetingStartsAtCurrentPosition() {
        Tween tween = new Tween(0);
        tween.target(100, 0, 1000, Easing.LINEAR);
        double current = tween.value(300);
        tween.target(200, 300, 1000, Easing.EASE_OUT_CUBIC);
        assertEquals(current, tween.value(300), 1e-9); assertEquals(200, tween.value(1300));
    }
    @ParameterizedTest @ValueSource(ints = {30, 60, 144, 240})
    void durationIsIndependentOfFrameRate(int fps) {
        Tween tween = new Tween(0); tween.target(100, 0, 140_000_000, Easing.EASE_OUT_CUBIC);
        for (long n = 0; n < 140_000_000; n += 1_000_000_000 / fps) tween.value(n);
        assertEquals(100, tween.value(140_000_000), 1e-9);
    }
    @Test void clippingIntersectionIsSafe() {
        assertEquals(new Bounds(10, 5, 5, 5), new Bounds(0, 0, 15, 10).intersect(new Bounds(10, 5, 30, 30)));
        assertFalse(Bounds.item(0, 0).intersects(Bounds.item(100, 100)));
        assertFalse(Bounds.item(0, 0).contains(16, 16));
        assertThrows(IllegalArgumentException.class, () -> Bounds.item(Double.NaN, 1));
    }
}
