package com.cappleapple.animatedinventory;
import com.cappleapple.animatedinventory.client.transaction.*;
import com.cappleapple.animatedinventory.api.inventory.*;
import com.cappleapple.animatedinventory.api.animation.*;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.cappleapple.animatedinventory.TransactionInferenceTest.*;
class SynchronizedTransferTest {
    @org.junit.jupiter.api.BeforeAll static void init() { bootstrap(); }
    @Test void predictionDoesNotBecomeAnIntermediateDestination() {
        var before = snapshot(item("storage", Items.DIAMOND, 32, 0));
        var predicted = snapshot(item("grid", Items.DIAMOND, 32, 30));
        var finalState = snapshot(item("hotbar", Items.DIAMOND, 32, 60));
        var pending = new SynchronizedTransfer(before, "click", 10, 100, 0);
        assertEquals(SynchronizedTransfer.Decision.WAIT, pending.observe(predicted, 10, 101, 20_000_000));
        assertEquals(SynchronizedTransfer.Decision.WAIT, pending.observe(finalState, 11, 102, 50_000_000));
        assertEquals(SynchronizedTransfer.Decision.READY, pending.observe(finalState, 11, 102, 100_000_000));
        var move = compare(pending.before, finalState).transitions().get(0);
        assertEquals("storage", move.sourceId()); assertEquals("hotbar", move.destinationId());
    }
    @Test void reverseTransferWaitsForSeparatelyDeliveredMenuPacket() {
        var before = snapshot(item("player", Items.DIAMOND, 32, 0));
        var after = snapshot(item("storage", Items.DIAMOND, 32, 60));
        var pending = new SynchronizedTransfer(before, "click", 10, 100, 0);
        assertEquals(SynchronizedTransfer.Decision.WAIT, pending.observe(before, 11, 101, 20_000_000));
        assertEquals(SynchronizedTransfer.Decision.WAIT, pending.observe(after, 11, 102, 40_000_000));
        assertEquals(SynchronizedTransfer.Decision.READY, pending.observe(after, 11, 102, 80_000_000));
    }
    @Test void missingAcknowledgementReleasesVisualBaseline() {
        var before = snapshot();
        var pending = new SynchronizedTransfer(before, "click", 10, 100, 0);
        assertEquals(SynchronizedTransfer.Decision.REBASE, pending.observe(before, 10, 101, 750_000_000));
    }
    @Test void pageChangesDiscardPendingOwnership() {
        var before = snapshot();
        var pending = new SynchronizedTransfer(before, "click", 10, 100, 0);
        assertEquals(SynchronizedTransfer.Decision.REBASE, pending.observe(
                new InventoryVisualSnapshot(1,"test",2,true,Map.of()), 11, 101, 20_000_000));
    }
    @Test void doubleClickRetainsEveryDonorAndPartialRemainder() {
        var before = snapshot(item("cursor", Items.DIAMOND, 16, 70),
                item("a", Items.DIAMOND, 12, 0), item("b", Items.DIAMOND, 20, 20), item("c", Items.DIAMOND, 40, 40));
        var after = snapshot(item("cursor", Items.DIAMOND, 64, 70), item("c", Items.DIAMOND, 24, 40));
        var moves = compare(before, after).transitions();
        assertEquals(3, moves.size());
        assertEquals(List.of(12,20,16), moves.stream().map(t -> t.stack().getCount()).toList());
        assertTrue(moves.stream().allMatch(t -> t.destinationId().equals("cursor") && t.type() == TransitionType.MERGE));
    }
}
