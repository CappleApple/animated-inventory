package com.cappleapple.animatedinventory;

import com.cappleapple.animatedinventory.api.animation.Bounds;
import com.cappleapple.animatedinventory.api.inventory.*;
import com.cappleapple.animatedinventory.client.compat.sophisticated.SophisticatedCrafting;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SophisticatedCraftingTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    private static VisualItem item(String id, Item item, int count) { return new VisualItem(id, new ItemStack(item, count), Bounds.item(0, 0), "test"); }
    private static InventoryVisualSnapshot snapshot(long owner, long layout, VisualItem... items) { return InventoryVisualSnapshot.of(owner, "test", layout, true, List.of(items)); }
    private final VisualItem ingredient = item("input", Items.OAK_LOG, 3), result = item("result", Items.OAK_PLANKS, 4);
    private SophisticatedCrafting.Take take(VisualItem... more) {
        List<VisualItem> all = new ArrayList<>(List.of(ingredient, result)); all.addAll(List.of(more));
        return new SophisticatedCrafting.Take(snapshot(1, 1, all.toArray(VisualItem[]::new)), "result", List.of("input"));
    }
    @Test void successfulTakeFlowsEvenWhenServerHasNotConsumedTheGridYet() {
        var uses = take().observe(snapshot(1, 1, ingredient, item("cursor", Items.OAK_PLANKS, 4)));
        assertEquals(1, uses.size()); assertEquals(1, uses.getFirst().ingredient().getCount());
    }
    @Test void failedTakeAndPreviewRefreshDoNotCraft() { assertTrue(take().observe(snapshot(1, 1, ingredient, result)).isEmpty()); }
    @Test void existingCursorQuantityIsNotCountedAgain() {
        var uses = take(item("cursor", Items.OAK_PLANKS, 12)).observe(snapshot(1, 1, ingredient, result, item("cursor", Items.OAK_PLANKS, 16)));
        assertEquals(1, uses.getFirst().ingredient().getCount());
    }
    @Test void SplitOutputGainsCountActualBatches() {
        var uses = take().observe(snapshot(1, 1, ingredient, result, item("a", Items.OAK_PLANKS, 4), item("b", Items.OAK_PLANKS, 8)));
        assertEquals(3, uses.getFirst().ingredient().getCount());
    }
    @Test void MovingExistingProductDoesNotCreateFalseCrafting() {
        assertTrue(take(item("a", Items.OAK_PLANKS, 4)).observe(snapshot(1, 1, ingredient, result, item("a", Items.OAK_PLANKS, 0), item("b", Items.OAK_PLANKS, 4))).isEmpty());
    }
    @Test void ChangedOwnerOrLayoutRejectsTake() {
        assertTrue(take().observe(snapshot(2, 1, ingredient, item("cursor", Items.OAK_PLANKS, 4))).isEmpty());
        assertTrue(take().observe(snapshot(1, 2, ingredient, item("cursor", Items.OAK_PLANKS, 4))).isEmpty());
    }
}
