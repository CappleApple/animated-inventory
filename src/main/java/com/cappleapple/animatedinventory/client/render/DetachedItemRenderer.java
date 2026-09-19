package com.cappleapple.animatedinventory.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;

/** Extracts a departing model without replaying a GUI item interaction/render entry. */
public final class DetachedItemRenderer {
    public static void draw(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        var client = Minecraft.getInstance();
        var model = new TrackingItemStackRenderState();
        client.getItemModelResolver().updateForTopItem(model, stack, ItemDisplayContext.GUI, client.level, client.player, 0);
        TextureCompositor.state(graphics).addItem(new GuiItemRenderState(new Matrix3x2f(graphics.pose()), model, x, y, TextureCompositor.clip(graphics)));
        graphics.itemDecorations(com.cappleapple.animatedinventory.client.Platform.countFont(stack), stack, x, y);
    }
    private DetachedItemRenderer() { }
}
