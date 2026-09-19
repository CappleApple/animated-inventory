package com.cappleapple.animatedinventory;

import com.cappleapple.animatedinventory.api.animation.*;
import com.cappleapple.animatedinventory.api.inventory.*;
import com.cappleapple.animatedinventory.client.animation.*;
import com.cappleapple.animatedinventory.client.transaction.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CraftingFlowTest {
    @BeforeAll static void bootstrap() { TestBootstrap.initialize(); }
    private static VisualItem item(String id, Item item, int count, int x) {
        return new VisualItem(id, new ItemStack(item, count), Bounds.item(x, 20), "test");
    }
    private static InventoryVisualSnapshot snapshot(VisualItem... items) {
        return InventoryVisualSnapshot.of(1, "test", 1, false, List.of(items));
    }
    private static CraftingFlow.Consumption use(String id, Item ingredient, int count, Item product) {
        return new CraftingFlow.Consumption(id, "result", new ItemStack(ingredient, count), new ItemStack(product));
    }
    private static List<ItemTransition> compare(InventoryVisualSnapshot before, InventoryVisualSnapshot after,
                                                CraftingFlow.Consumption... uses) {
        return CraftingFlow.compare(new TransactionInference(), before, after, List.of(uses), "craft", false, 128).transitions();
    }
    @Test void pickupFlowsConsumedUnitsIntoCarriedProduct() {
        var before = snapshot(item("a", Items.OAK_PLANKS, 8, 10), item("b", Items.OAK_PLANKS, 8, 30), item("result", Items.STICK, 4, 80));
        var after = snapshot(item("a", Items.OAK_PLANKS, 7, 10), item("b", Items.OAK_PLANKS, 7, 30), item("cursor", Items.STICK, 4, 120));
        var tx = compare(before, after, use("a", Items.OAK_PLANKS, 1, Items.STICK), use("b", Items.OAK_PLANKS, 1, Items.STICK));
        assertEquals(2, tx.stream().filter(t -> t.type() == TransitionType.CRAFT).count());
        assertTrue(tx.stream().filter(t -> t.type() == TransitionType.CRAFT).allMatch(t ->
                "cursor".equals(t.destinationId()) && t.stack().getCount() == 1 && t.stack().is(Items.OAK_PLANKS)));
        assertTrue(tx.stream().noneMatch(t -> t.type() == TransitionType.DISAPPEAR || t.type() == TransitionType.COUNT_CHANGE));
        assertTrue(tx.stream().anyMatch(t -> t.sourceId().equals("result") && t.destinationId().equals("cursor") && t.stack().getCount() == 4));
    }
    @Test void regeneratedPreviewDoesNotHideCraftedProductMovement() {
        var tx = compare(snapshot(item("a", Items.OAK_LOG, 4, 10), item("result", Items.OAK_PLANKS, 4, 80)),
                snapshot(item("a", Items.OAK_LOG, 3, 10), item("result", Items.OAK_PLANKS, 4, 80), item("cursor", Items.OAK_PLANKS, 4, 120)),
                use("a", Items.OAK_LOG, 1, Items.OAK_PLANKS));
        assertEquals(2, tx.size());
        assertTrue(tx.stream().anyMatch(t -> "result".equals(t.sourceId()) && "cursor".equals(t.destinationId()) && t.stack().getCount() == 4));
        assertTrue(tx.stream().noneMatch(t -> t.type() == TransitionType.APPEAR));
    }
    @Test void previewOrRecipePlacementAloneCannotCreateCraftingFlows() {
        var tx = compare(snapshot(), snapshot(item("a", Items.OAK_LOG, 1, 10), item("result", Items.OAK_PLANKS, 4, 80)));
        assertTrue(tx.stream().noneMatch(t -> t.type() == TransitionType.CRAFT));
    }
    @Test void returnedBucketsStayInNormalGridRendering() {
        var tx = compare(snapshot(item("milk", Items.MILK_BUCKET, 1, 10), item("result", Items.CAKE, 1, 80)),
                snapshot(item("milk", Items.BUCKET, 1, 10), item("cursor", Items.CAKE, 1, 120)),
                use("milk", Items.MILK_BUCKET, 1, Items.CAKE));
        assertTrue(tx.stream().noneMatch(t -> t.stack().is(Items.BUCKET)));
        assertTrue(tx.stream().anyMatch(t -> t.type() == TransitionType.CRAFT && t.stack().is(Items.MILK_BUCKET)));
    }
    @Test void repeatedTakesAggregateOnlyConsumedQuantities() {
        var tx = compare(snapshot(item("a", Items.OAK_LOG, 8, 10), item("result", Items.OAK_PLANKS, 4, 80)),
                snapshot(item("a", Items.OAK_LOG, 6, 10), item("result", Items.OAK_PLANKS, 4, 80), item("bag", Items.OAK_PLANKS, 8, 160)),
                use("a", Items.OAK_LOG, 1, Items.OAK_PLANKS), use("a", Items.OAK_LOG, 1, Items.OAK_PLANKS));
        var flow = tx.stream().filter(t -> t.type() == TransitionType.CRAFT).toList();
        assertEquals(1, flow.size());
        assertEquals(2, flow.get(0).stack().getCount());
        assertEquals("bag", flow.get(0).destinationId());
        assertTrue(tx.stream().anyMatch(t -> "result".equals(t.sourceId()) && "bag".equals(t.destinationId()) && t.stack().getCount() == 8));
    }
    @Test void shiftCraftFollowsTheLargestOutputDestination() {
        var tx = compare(snapshot(item("a", Items.OAK_LOG, 1, 10), item("result", Items.OAK_PLANKS, 4, 80), item("bag", Items.OAK_PLANKS, 63, 140)),
                snapshot(item("bag", Items.OAK_PLANKS, 64, 140), item("hotbar", Items.OAK_PLANKS, 3, 180)),
                use("a", Items.OAK_LOG, 1, Items.OAK_PLANKS));
        assertEquals("hotbar", tx.stream().filter(t -> t.type() == TransitionType.CRAFT).findFirst().orElseThrow().destinationId());
    }
    @Test void stowedProductUsesItsExplicitEdgeDestination() {
        var edge = Bounds.item(140, 100);
        var hidden = new VisualItem("hidden", new ItemStack(Items.OAK_PLANKS, 4), edge, "storage", false, true, false, null, edge, null, null);
        var tx = compare(snapshot(item("a", Items.OAK_LOG, 1, 10), item("result", Items.OAK_PLANKS, 4, 80)),
                snapshot(hidden), use("a", Items.OAK_LOG, 1, Items.OAK_PLANKS));
        var flow = tx.stream().filter(t -> t.type() == TransitionType.CRAFT).findFirst().orElseThrow();
        assertEquals("hidden", flow.destinationId()); assertEquals(edge, flow.destination());
    }
    @Test void emptyOrFailedTakeCannotCreateCraftingFlows() {
        assertTrue(compare(snapshot(), snapshot()).isEmpty());
    }
    @Test void changedOwnerRejectsPendingConsumption() {
        var before = snapshot(item("a", Items.OAK_LOG, 1, 10), item("result", Items.OAK_PLANKS, 4, 80));
        var after = InventoryVisualSnapshot.of(2, "test", 1, false, List.of(item("cursor", Items.OAK_PLANKS, 4, 120)));
        assertTrue(compare(before, after, use("a", Items.OAK_LOG, 1, Items.OAK_PLANKS)).isEmpty());
    }
    @Test void ingredientCopiesNeverSuppressOrReplaceProductEvenWithSameItem() {
        var manager = new AnimationManager();
        var t = new ItemTransition(TransitionType.CRAFT, "a", "product", new ItemStack(Items.DIAMOND_SWORD),
                Bounds.item(10, 10), Bounds.item(100, 10), null, null);
        manager.add(1, "craft", t, AnimationOptions.move(100, Easing.LINEAR, MovementStyle.LINEAR), 0, 100, 128, false, t.source());
        assertEquals(1, manager.normalStack("product", new ItemStack(Items.DIAMOND_SWORD), 50).getCount());
        assertNull(manager.destination("product", 50));
    }
    @Test void ingredientDestinationTracksMovingCraftedStackAndCursor() {
        var manager = new AnimationManager();
        var product = new ItemTransition(TransitionType.MOVE, "result", "cursor", new ItemStack(Items.STICK, 4),
                Bounds.item(80, 20), Bounds.item(160, 20), null, null);
        var ingredient = new ItemTransition(TransitionType.CRAFT, "a", "cursor", new ItemStack(Items.OAK_PLANKS),
                Bounds.item(10, 20), Bounds.item(160, 20), null, null);
        manager.add(1, "craft", product, AnimationOptions.move(100, Easing.LINEAR, MovementStyle.LINEAR), 0, 100, 128, true, product.source());
        manager.add(1, "craft", ingredient, AnimationOptions.move(200, Easing.LINEAR, MovementStyle.LINEAR), 0, 200, 128, false, ingredient.source());
        manager.update(50, Bounds.item(200, 20), a -> {});
        var flow = manager.active().stream().filter(a -> a.transition.type() == TransitionType.CRAFT).findFirst().orElseThrow();
        assertEquals(Bounds.item(120, 20), flow.destination);
        manager.update(150, Bounds.item(240, 20), a -> {});
        assertEquals(Bounds.item(240, 20), flow.destination);
    }
    @Test void animationBudgetBoundsLargeBatch() {
        var before = snapshot(item("a", Items.OAK_LOG, 1, 10), item("result", Items.OAK_PLANKS, 4, 80));
        var after = snapshot(item("cursor", Items.OAK_PLANKS, 4, 120));
        var tx = CraftingFlow.compare(new TransactionInference(), before, after, List.of(use("a", Items.OAK_LOG, 1, Items.OAK_PLANKS)), "craft", false, 1);
        assertEquals(1, tx.transitions().size());
    }
}
