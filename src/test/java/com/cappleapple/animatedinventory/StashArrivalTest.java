package com.cappleapple.animatedinventory;
import com.cappleapple.animatedinventory.api.animation.*;
import com.cappleapple.animatedinventory.api.inventory.*;
import com.cappleapple.animatedinventory.client.animation.ItemAnimation;
import com.cappleapple.animatedinventory.client.transaction.*;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.cappleapple.animatedinventory.TransactionInferenceTest.*;
class StashArrivalTest {
    @BeforeAll static void init() { bootstrap(); }
    static VisualItem hidden(String id, int count) {
        return new VisualItem(id, new ItemStack(Items.OAK_PLANKS,count), Bounds.item(10,100), "stash", false, true, false,
                null, Bounds.item(10,100), null, null, Bounds.item(10,116));
    }
    @Test void recipeFillMatchesVisibleAndStashedQuantitiesSeparately() {
        var before = snapshot(item("visible", Items.OAK_PLANKS,2,40), hidden("stash",2));
        var after = snapshot(item("grid1",Items.OAK_PLANKS,1,50),item("grid2",Items.OAK_PLANKS,1,68),
                item("grid3",Items.OAK_PLANKS,1,86),item("grid4",Items.OAK_PLANKS,1,104));
        var moves = compare(before, after).transitions();
        assertEquals(4, moves.size());
        assertEquals(2, moves.stream().filter(t -> t.type()==TransitionType.RETRIEVE).count());
        assertEquals(2, moves.stream().filter(t -> "visible".equals(t.sourceId())).count());
        assertTrue(moves.stream().filter(t -> t.type()==TransitionType.RETRIEVE).allMatch(t -> t.source().y()==116));
    }
    @Test void hiddenRearrangementAndConsumptionStayInvisible() {
        assertTrue(compare(snapshot(hidden("a",20)),snapshot(hidden("b",20))).transitions().isEmpty());
        assertTrue(compare(snapshot(hidden("a",20)),snapshot(hidden("a",10))).transitions().isEmpty());
    }
    @Test void partialStashWithdrawalKeepsRemainder() {
        var moves=compare(snapshot(hidden("a",64)),snapshot(hidden("a",60),item("grid",Items.OAK_PLANKS,4,50))).transitions();
        assertEquals(1,moves.size()); assertEquals(4,moves.getFirst().stack().getCount());
    }
    @Test void stashArrivalFadesInAndFinishesAtVisibleCell() {
        var t=compare(snapshot(hidden("a",4)),snapshot(item("grid",Items.OAK_PLANKS,4,50))).transitions().getFirst();
        var a=new ItemAnimation(1,1,"tx",t,AnimationOptions.move(100,Easing.LINEAR,MovementStyle.LINEAR),t.source(),0,100,false);
        assertEquals(0,a.alpha(0)); assertTrue(a.alpha(10)>0 && a.alpha(10)<1); assertEquals(1,a.alpha(50));
        assertEquals(t.destination(),a.bounds(100));
    }
    @Test void recipeResponseWithoutBnsWaitsForChangedContents() {
        var before=snapshot(item("visible",Items.OAK_PLANKS,4,40));
        var after=snapshot(item("grid",Items.OAK_PLANKS,4,60));
        var pending=new SynchronizedTransfer(before,"recipe",-1,1,0);
        assertEquals(SynchronizedTransfer.Decision.WAIT,pending.observe(before,-1,2,50_000_000));
        assertEquals(SynchronizedTransfer.Decision.WAIT,pending.observe(after,-1,3,100_000_000));
        assertEquals(SynchronizedTransfer.Decision.READY,pending.observe(after,-1,3,150_000_000));
    }
}
