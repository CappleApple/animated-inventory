package com.cappleapple.animatedinventory;

import com.cappleapple.animatedinventory.api.animation.*;
import com.cappleapple.animatedinventory.api.inventory.*;
import com.cappleapple.animatedinventory.client.transaction.TransactionInference;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TransactionInferenceTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    static VisualItem item(String id, Item type, int count, int x) { return new VisualItem(id, new ItemStack(type, count), Bounds.item(x, 0), "test"); }
    static InventoryVisualSnapshot snapshot(VisualItem... items) { return InventoryVisualSnapshot.of(1, "test", 1, false, List.of(items)); }
    static InventoryVisualTransaction compare(InventoryVisualSnapshot before, InventoryVisualSnapshot after) {
        return new TransactionInference().compare(before, after, "tx", true, 128);
    }
    @Test void quickMoveSplitsThirtyTwoIntoTwelveAndTwenty() {
        var before = snapshot(item("source", Items.IRON_INGOT, 32, 0), item("a", Items.IRON_INGOT, 52, 40));
        var after = snapshot(item("a", Items.IRON_INGOT, 64, 40), item("b", Items.IRON_INGOT, 20, 70));
        var transitions = compare(before, after).transitions();
        assertEquals(2, transitions.size());
        assertEquals(32, transitions.stream().mapToInt(t -> t.stack().getCount()).sum());
        assertTrue(transitions.stream().anyMatch(t -> t.destinationId().equals("a") && t.stack().getCount() == 12 && t.type() == TransitionType.MERGE));
        assertTrue(transitions.stream().anyMatch(t -> t.destinationId().equals("b") && t.stack().getCount() == 20));
    }
    @Test void partialMoveLeavesSourceRemainder() {
        var tx = compare(snapshot(item("a", Items.DIAMOND, 64, 0)), snapshot(item("a", Items.DIAMOND, 60, 0), item("b", Items.DIAMOND, 4, 20)));
        assertEquals(1, tx.transitions().size()); assertEquals(4, tx.transitions().get(0).stack().getCount());
    }
    @Test void rightClickHalfPickupAndSinglePlacement() {
        var tx = compare(snapshot(item("a", Items.APPLE, 9, 0)), snapshot(item("a", Items.APPLE, 4, 0), item("cursor", Items.APPLE, 5, 50)));
        assertEquals(5, tx.transitions().get(0).stack().getCount());
        var placement = compare(snapshot(item("cursor", Items.APPLE, 5, 50)), snapshot(item("cursor", Items.APPLE, 4, 50), item("b", Items.APPLE, 1, 80)));
        assertEquals(1, placement.transitions().get(0).stack().getCount());
    }
    @Test void dragDistributionConservesMovedQuantity() {
        var tx = compare(snapshot(item("cursor", Items.APPLE, 12, 0)), snapshot(item("cursor", Items.APPLE, 3, 0),
                item("a", Items.APPLE, 3, 20), item("b", Items.APPLE, 3, 40), item("c", Items.APPLE, 3, 60)));
        assertEquals(9, tx.transitions().stream().mapToInt(t -> t.stack().getCount()).sum());
        assertEquals(3, tx.transitions().size());
    }
    @Test void numberKeyAndOffhandSwapProduceTwoIndependentMovements() {
        var tx = compare(snapshot(item("a", Items.APPLE, 2, 0), item("b", Items.STONE, 8, 20)),
                snapshot(item("a", Items.STONE, 8, 0), item("b", Items.APPLE, 2, 20)));
        assertEquals(2, tx.transitions().size());
        assertTrue(tx.transitions().stream().allMatch(t -> t.type() == TransitionType.SWAP));
    }
    @Test void identicalStacksStayWithTheirLogicalIdentity() {
        var tx = compare(snapshot(item("keep", Items.APPLE, 10, 0), item("move", Items.APPLE, 10, 20)),
                snapshot(item("keep", Items.APPLE, 10, 0), item("new", Items.APPLE, 10, 40)));
        assertEquals(1, tx.transitions().size()); assertEquals("move", tx.transitions().get(0).sourceId());
    }
    @Test void renamedComponentsDoNotMatch() {
        VisualItem old = item("a", Items.APPLE, 3, 0);
        ItemStack named = new ItemStack(Items.APPLE, 3); named.setHoverName(Component.literal("Special"));
        var tx = compare(snapshot(old), snapshot(new VisualItem("b", named, Bounds.item(20, 0), "test")));
        assertEquals(Set.of(TransitionType.APPEAR, TransitionType.DISAPPEAR), new HashSet<>(tx.transitions().stream().map(ItemTransition::type).toList()));
    }
    @Test void viewReflowIsDistinctFromInventoryMutation() {
        var tx = compare(snapshot(item("logical", Items.APPLE, 8, 0)), snapshot(item("logical", Items.APPLE, 8, 80)));
        assertEquals(TransitionType.LAYOUT_REFLOW, tx.transitions().get(0).type());
    }
    @Test void movingTheCursorDoesNotInferLayoutReflow() {
        assertTrue(compare(snapshot(item("cursor", Items.APPLE, 3, 0)), snapshot(item("cursor", Items.APPLE, 3, 80))).transitions().isEmpty());
    }
    @Test void creativeVirtualizationIsRebased() {
        var a = InventoryVisualSnapshot.of(1, "test", 1, true, List.of(item("a", Items.APPLE, 1, 0)));
        var b = InventoryVisualSnapshot.of(1, "test", 2, true, List.of(item("b", Items.STONE, 1, 20)));
        assertTrue(compare(a, b).transitions().isEmpty());
    }
    @Test void newScreenOwnerCannotInheritAnOldTransaction() {
        var after = InventoryVisualSnapshot.of(2, "test", 1, false, List.of(item("b", Items.APPLE, 1, 20)));
        assertTrue(compare(snapshot(item("a", Items.APPLE, 1, 0)), after).transitions().isEmpty());
    }
    @Test void hiddenAndNonanimatableViewsNeverBecomeSuppressionClaims() {
        var hidden = new VisualItem("b", new ItemStack(Items.APPLE, 1), Bounds.item(30, 0), "test", false, false, false, null, null, null, null);
        var tx = compare(snapshot(), snapshot(hidden));
        assertTrue(tx.transitions().isEmpty());
    }
    @Test void equipmentClassificationUsesProviderMetadata() {
        var equipment = new VisualItem("custom-equipment", new ItemStack(Items.DIAMOND_HELMET), Bounds.item(40, 0), "equipment", true, true, true, null, null, null, null);
        assertEquals(TransitionType.EQUIP, compare(snapshot(item("bag", Items.DIAMOND_HELMET, 1, 0)), snapshot(equipment)).transitions().get(0).type());
        assertEquals(TransitionType.UNEQUIP, compare(snapshot(equipment), snapshot(item("bag", Items.DIAMOND_HELMET, 1, 0))).transitions().get(0).type());
    }
    @Test void countOnlyChangeIsEmphasis() {
        assertEquals(TransitionType.COUNT_CHANGE, compare(snapshot(item("a", Items.APPLE, 5, 0)), snapshot(item("a", Items.APPLE, 7, 0))).transitions().get(0).type());
    }
    @Test void duplicateIdsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> snapshot(item("a", Items.APPLE, 1, 0), item("a", Items.APPLE, 2, 30)));
    }
    @Test void sortingBudgetDoesNotExceedCap() {
        List<VisualItem> before = new ArrayList<>(), after = new ArrayList<>();
        for (int i = 0; i < 100; i++) { before.add(item("a" + i, Items.STONE, 1, i * 18)); after.add(item("b" + i, Items.STONE, 1, i * 18)); }
        var tx = new TransactionInference().compare(InventoryVisualSnapshot.of(1, "test", 1, false, before),
                InventoryVisualSnapshot.of(1, "test", 1, false, after), "sort", false, 16);
        assertEquals(16, tx.transitions().size());
    }
    @Test void randomizedRedistributionConservesQuantities() {
        Random random = new Random(47);
        for (int trial = 0; trial < 300; trial++) {
            List<VisualItem> before = new ArrayList<>(), after = new ArrayList<>();
            int total = 0;
            for (int i = 0; i < 12; i++) { int count = 1 + random.nextInt(64); total += count; before.add(item("old" + i, Items.IRON_INGOT, count, i * 18)); }
            int remaining = total, index = 0;
            while (remaining > 0) { int count = Math.min(remaining, 1 + random.nextInt(64)); remaining -= count; after.add(item("new" + index++, Items.IRON_INGOT, count, index * 18)); }
            var tx = compare(InventoryVisualSnapshot.of(1, "test", 1, false, before), InventoryVisualSnapshot.of(1, "test", 1, false, after));
            assertEquals(total, tx.transitions().stream().mapToInt(t -> t.stack().getCount()).sum());
            assertTrue(tx.transitions().stream().noneMatch(t -> t.type() == TransitionType.APPEAR || t.type() == TransitionType.DISAPPEAR));
        }
    }
}
