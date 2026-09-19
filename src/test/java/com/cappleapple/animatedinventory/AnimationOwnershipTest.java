package com.cappleapple.animatedinventory;

import com.cappleapple.animatedinventory.api.animation.*;
import com.cappleapple.animatedinventory.api.inventory.*;
import com.cappleapple.animatedinventory.client.animation.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AnimationOwnershipTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    static ItemTransition transition(int count, Bounds clip) {
        return new ItemTransition(TransitionType.MERGE, "a", "b", new ItemStack(Items.IRON_INGOT, count), Bounds.item(0, 0), Bounds.item(50, 0), clip, null);
    }
    static long add(AnimationManager manager, int count, int cap) {
        return manager.add(1, "tx", transition(count, null), AnimationOptions.move(100, Easing.LINEAR, MovementStyle.LINEAR), 0, 100, cap, false, Bounds.item(0, 0));
    }
    @Test void partialMergeWithholdsOnlyInFlightQuantity() {
        var manager = new AnimationManager(); add(manager, 16, 128);
        ItemStack real = new ItemStack(Items.IRON_INGOT, 64);
        assertEquals(48, manager.normalStack("b", real, 50).getCount()); assertEquals(64, real.getCount());
        assertEquals(64, manager.normalStack("b", real, 100).getCount());
    }
    @Test void multipleIncomingStacksConserveDestinationRepresentation() {
        var manager = new AnimationManager(); add(manager, 12, 128); add(manager, 20, 128);
        assertEquals(32, manager.normalStack("b", new ItemStack(Items.IRON_INGOT, 64), 50).getCount());
    }
    @Test void cancellationImmediatelyReleasesOwnership() {
        var manager = new AnimationManager(); long handle = add(manager, 16, 128); manager.cancel(handle);
        assertEquals(16, manager.normalStack("b", new ItemStack(Items.IRON_INGOT, 16), 50).getCount());
    }
    @Test void aChangedComponentOrItemCannotBeHiddenByOldAnimation() {
        var manager = new AnimationManager(); add(manager, 16, 128);
        assertEquals(16, manager.normalStack("b", new ItemStack(Items.GOLD_INGOT, 16), 50).getCount());
    }
    @Test void rejectedAnimationsCannotHideItems() {
        var manager = new AnimationManager(); assertTrue(add(manager, 4, 1) > 0); assertEquals(-1, add(manager, 16, 1));
        assertEquals(16, manager.normalStack("b", new ItemStack(Items.IRON_INGOT, 20), 50).getCount());
    }
    @Test void staleStateIsBoundedAndCleared() {
        var manager = new AnimationManager();
        for (int i = 0; i < 1000; i++) add(manager, 1, 128);
        assertEquals(128, manager.active().size()); manager.clear(); assertTrue(manager.active().isEmpty());
    }
    @Test void offscreenEndpointRejectsAnimation() {
        var manager = new AnimationManager(); var t = transition(1, Bounds.item(0, 0));
        assertEquals(-1, manager.add(1, "tx", t, AnimationOptions.move(100, Easing.LINEAR, MovementStyle.LINEAR), 0, 100, 128, false, t.source()));
    }
    @Test void completionAndCursorFollowingAreElapsedTimeDriven() {
        var manager = new AnimationManager(); var t = transition(1, null);
        var options = new AnimationOptions(100, Easing.LINEAR, MovementStyle.LINEAR, 0, 1, 1, 1, 1, 0, 0, 150, true);
        manager.add(1, "tx", t, options, 0, 100, 128, false, t.source());
        manager.update(50, Bounds.item(100, 80), a -> fail());
        assertEquals(Bounds.item(50, 40), manager.active().get(0).bounds(50));
        List<Long> completed = new ArrayList<>(); manager.update(100, Bounds.item(100, 80), a -> completed.add(a.handle));
        assertEquals(1, completed.size()); assertTrue(manager.active().isEmpty());
    }
    @Test void inlineParticlePathDoesNotWithholdNormalRender() {
        var manager = new AnimationManager(); var t = transition(16, null);
        manager.add(1, "tx", t, AnimationOptions.move(100, Easing.LINEAR, MovementStyle.LINEAR), 0, 100, 128, true, t.source());
        assertEquals(16, manager.normalStack("b", new ItemStack(Items.IRON_INGOT, 16), 50).getCount());
    }
    @Test void interruptionCancelsAllRelatedClaims() {
        var manager = new AnimationManager(); add(manager, 12, 128); add(manager, 20, 128);
        manager.cancelTouching(Set.of("b")); assertTrue(manager.active().isEmpty());
    }
}
