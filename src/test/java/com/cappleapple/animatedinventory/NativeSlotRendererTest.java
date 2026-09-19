package com.cappleapple.animatedinventory;

import com.cappleapple.animatedinventory.client.animation.AnimationManager;
import com.cappleapple.animatedinventory.client.render.NativeSlotRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class NativeSlotRendererTest {
    @BeforeAll static void bootstrap() { TestBootstrap.initialize(); }

    @Test void changedDragPreviewDoesNotTouchTheExistingArrivalClaim() {
        var manager = new AnimationManager();
        AnimationOwnershipTest.add(manager, 16, 128);
        ItemStack real = new ItemStack(Items.IRON_INGOT, 32);
        ItemStack preview = real.copyWithCount(40);
        ItemStack rendered = NativeSlotRenderer.selectStack(preview, real, null, () -> {
            manager.cancelTouching(Set.of("b"));
            return preview;
        });
        assertSame(preview, rendered);
        assertEquals(1, manager.active().size());
        assertEquals(16, manager.normalStack("b", real, 50).getCount());
    }

    @Test void cappedPreviewLabelKeepsEvenAnEqualRealStackUnsuppressed() {
        var manager = new AnimationManager();
        AnimationOwnershipTest.add(manager, 16, 128);
        ItemStack real = new ItemStack(Items.IRON_INGOT, 64);
        ItemStack rendered = NativeSlotRenderer.selectStack(real.copy(), real, "\u00a7e64", () -> manager.normalStack("b", real, 50));
        assertEquals(64, rendered.getCount());
        assertEquals(1, manager.active().size());
    }

    @Test void realItemAndDecorationsStillWithholdExactlyTheInFlightQuantity() {
        var manager = new AnimationManager();
        AnimationOwnershipTest.add(manager, 16, 128);
        ItemStack real = new ItemStack(Items.IRON_INGOT, 64);
        assertEquals(48, NativeSlotRenderer.selectStack(real, real, null, () -> manager.normalStack("b", real, 50)).getCount());
        assertEquals(48, NativeSlotRenderer.selectStack(real, real, null, () -> manager.normalStack("b", real, 50)).getCount());
        assertEquals(64, real.getCount());
        assertEquals(1, manager.active().size());
    }
}
