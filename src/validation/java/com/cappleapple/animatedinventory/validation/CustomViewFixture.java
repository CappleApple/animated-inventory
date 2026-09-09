package com.cappleapple.animatedinventory.validation;

import com.cappleapple.animatedinventory.api.AnimatedInventoryApi;
import com.cappleapple.animatedinventory.api.animation.*;
import com.cappleapple.animatedinventory.api.inventory.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.List;

/** A functioning non-Slot, non-container integration example. */
public final class CustomViewFixture extends Screen {
    private Bounds position = Bounds.item(140, 90);
    private long revision;
    private int count = 16;
    private final Bounds clip = new Bounds(110, 65, 180, 120);
    public CustomViewFixture() { super(Component.literal("Animated Inventory custom view fixture")); }
    public void reflow() { position = position.x() == 140 ? Bounds.item(240, 130) : Bounds.item(140, 90); revision++; AnimatedInventoryApi.notifyLayoutChanged(this, true); }
    public void appear() { count = 32; revision++; AnimatedInventoryApi.notifyLayoutChanged(this, false); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.fill(110, 65, 290, 185, 0xffc6c6c6);
        graphics.renderOutline(110, 65, 180, 120, 0xff373737);
        graphics.drawString(font, "Custom logical item / no Slot", 112, 68, 0xff202020, false);
        graphics.enableScissor(110, 65, 290, 185);
        ItemStack visible = AnimatedInventoryApi.normalRenderStack(this, "fixture:wood", new ItemStack(Items.OAK_PLANKS, count));
        Bounds normal = AnimatedInventoryApi.getNormalRenderBounds(this, "fixture:wood").orElse(position);
        graphics.renderItem(visible, (int)normal.x(), (int)normal.y());
        graphics.renderItemDecorations(font, visible, (int)normal.x(), (int)normal.y());
        AnimatedInventoryApi.renderAnimations(this, graphics);
        graphics.disableScissor();
    }
    public static final InventoryViewProvider PROVIDER = new InventoryViewProvider() {
        @Override public String id() { return "animatedinventory:validation"; }
        @Override public boolean supports(Screen screen) { return screen instanceof CustomViewFixture; }
        @Override public long revision(Screen screen) { return ((CustomViewFixture)screen).revision; }
        @Override public boolean controlsNormalRendering() { return true; }
        @Override public InventoryVisualSnapshot capture(Screen screen, long owner, double mouseX, double mouseY) {
            var view = (CustomViewFixture)screen;
            var item = new VisualItem("fixture:wood", new ItemStack(Items.OAK_PLANKS, view.count), view.position, "panel",
                    true, true, false, view.clip, null, "fixture-transaction-" + view.revision,
                    AnimationOptions.move(600, Easing.EASE_IN_OUT_CUBIC, MovementStyle.ARC));
            return InventoryVisualSnapshot.of(owner, id(), view.revision, false, List.of(item));
        }
    };
}
