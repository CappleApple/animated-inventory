package com.cappleapple.animatedinventory;

import com.cappleapple.animatedinventory.api.animation.*;
import com.cappleapple.animatedinventory.api.inventory.*;
import com.cappleapple.animatedinventory.client.animation.ItemAnimation;
import com.cappleapple.animatedinventory.client.compat.bundlednotsiloed.StowedSlotGeometry;
import com.cappleapple.animatedinventory.client.transaction.TransactionInference;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class StowedTransferTest {
    @BeforeAll static void bootstrap() { TestBootstrap.initialize(); }
    private static final Bounds EDGE = Bounds.item(46, 84);
    private static VisualItem visible(int count) {
        return new VisualItem("source", new ItemStack(Items.APPLE, count), Bounds.item(10, 10), "container");
    }
    private static VisualItem stowed(String id, int count) {
        return new VisualItem(id, new ItemStack(Items.APPLE, count), EDGE, "storage", false, true, false, null, EDGE, null, null);
    }
    private static InventoryVisualSnapshot snapshot(VisualItem... items) {
        return InventoryVisualSnapshot.of(1, "test", 1, true, List.of(items));
    }
    private static List<ItemTransition> compare(InventoryVisualSnapshot before, InventoryVisualSnapshot after) {
        return new TransactionInference().compare(before, after, "stow", true, 128).transitions();
    }
    @Test void visibleTransferReachesExplicitHiddenArrivalPoint() {
        var tx = compare(snapshot(visible(16)), snapshot(stowed("hidden", 16)));
        assertEquals(1, tx.size());
        assertEquals(TransitionType.STOW, tx.get(0).type());
        assertEquals(EDGE, tx.get(0).destination());
        assertEquals("source", tx.get(0).sourceId());
        assertEquals("hidden", tx.get(0).destinationId());
    }
    @Test void stowedMergeUsesOnlyTheIncreasedQuantity() {
        var tx = compare(snapshot(visible(12), stowed("hidden", 52)), snapshot(stowed("hidden", 64)));
        assertEquals(1, tx.size());
        assertEquals(12, tx.get(0).stack().getCount());
        assertEquals(TransitionType.STOW, tx.get(0).type());
    }
    @Test void splitBetweenTwoStowedSlotsPreservesTheVisibleRemainder() {
        var tx = compare(snapshot(visible(40), stowed("a", 52)),
                snapshot(visible(8), stowed("a", 64), stowed("b", 20)));
        assertEquals(2, tx.size());
        assertEquals(Set.of(12, 20), new HashSet<>(tx.stream().map(t -> t.stack().getCount()).toList()));
        assertTrue(tx.stream().allMatch(t -> t.type() == TransitionType.STOW));
    }
    @Test void hiddenAcquisitionWithoutVisibleSourceDoesNotInventTravel() {
        assertTrue(compare(snapshot(), snapshot(stowed("hidden", 20))).isEmpty());
    }
    @Test void hiddenRearrangementDoesNotCreateVisibleAnimation() {
        assertTrue(compare(snapshot(stowed("a", 20)), snapshot(stowed("b", 20))).isEmpty());
    }
    @Test void removalWithoutStorageIncreaseIsNotReportedAsStow() {
        var tx = compare(snapshot(visible(16), stowed("hidden", 32)), snapshot(stowed("hidden", 32)));
        assertEquals(TransitionType.DISAPPEAR, tx.get(0).type());
    }
    @Test void changedComponentsDoNotMatchHiddenStorage() {
        var target = stowed("hidden", 16);
        target.stack().setHoverName(Component.literal("Different"));
        var tx = compare(snapshot(visible(16)), snapshot(target));
        assertTrue(tx.stream().noneMatch(t -> t.type() == TransitionType.STOW));
    }
    @Test void pagingClearsStowInference() {
        var after = InventoryVisualSnapshot.of(1, "test", 2, true, List.of(stowed("hidden", 16)));
        assertTrue(compare(snapshot(visible(16)), after).isEmpty());
    }
    @Test void invisibleDestinationMustExplicitlyOptIn() {
        var hidden = new VisualItem("hidden", new ItemStack(Items.APPLE, 16), EDGE, "storage", false, false, false, null, EDGE, null, null);
        assertTrue(compare(snapshot(visible(16)), snapshot(hidden)).stream().noneMatch(t -> t.type() == TransitionType.STOW));
    }
    @Test void lowerRowUsesBottomEdgeAndKeepsColumn() {
        assertEquals(Bounds.item(154, 236), StowedSlotGeometry.destination(39, 9, Bounds.item(100, 200), 18, 18));
    }
    @Test void higherRowUsesTopEdgeAndKeepsColumn() {
        assertEquals(Bounds.item(244, 200), StowedSlotGeometry.destination(44, 45, Bounds.item(100, 200), 18, 18));
    }
    @Test void distantRowDoesNotSendItemOutsideTheGrid() {
        assertEquals(Bounds.item(136, 236), StowedSlotGeometry.destination(90002, 9, Bounds.item(100, 200), 18, 18));
    }
    @Test void stowStaysOpaqueUntilApproachThenDisappearsAtEdge() {
        var t = compare(snapshot(visible(16)), snapshot(stowed("hidden", 16))).get(0);
        var a = new ItemAnimation(1, 1, "tx", t, AnimationOptions.move(100, Easing.LINEAR, MovementStyle.LINEAR),
                t.source(), 0, 100, false);
        assertEquals(1, a.alpha(50));
        assertTrue(a.alpha(85) > 0 && a.alpha(85) < 1);
        assertEquals(0, a.alpha(100));
        assertEquals(EDGE, a.bounds(100));
    }
}
