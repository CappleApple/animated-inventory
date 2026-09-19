package com.cappleapple.animatedinventory.client.render;

import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;


/** Draws a departing representation through the model renderer, without replaying a GUI item interaction/render entry. */
public final class DetachedItemRenderer {
    public static void draw(GuiGraphics graphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        var client = Minecraft.getInstance();
        var renderer = client.getItemRenderer();
        var model = renderer.getModel(stack, client.level, client.player, 0);
        boolean flat = !model.usesBlockLight();
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(x + 8, y + 8, 150);
            graphics.pose().scale(16, -16, 16);
            if (flat) Lighting.setupForFlatItems();
            renderer.render(stack, ItemDisplayContext.GUI, false, graphics.pose(), graphics.bufferSource(),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
            graphics.flush();
        } finally {
            if (flat) Lighting.setupFor3DItems();
            graphics.pose().popPose();
        }
        graphics.renderItemDecorations(client.font, stack, x, y);
    }
    private DetachedItemRenderer() { }
}
