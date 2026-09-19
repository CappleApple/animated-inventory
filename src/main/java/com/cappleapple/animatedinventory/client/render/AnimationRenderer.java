package com.cappleapple.animatedinventory.client.render;

import com.cappleapple.animatedinventory.api.animation.Bounds;
import com.cappleapple.animatedinventory.api.animation.TransitionType;
import com.cappleapple.animatedinventory.client.*;
import com.cappleapple.animatedinventory.client.animation.ItemAnimation;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;


public final class AnimationRenderer {
    public static void render(GuiGraphics graphics, ClientRuntime runtime) {
        long start = ClientConfig.DEBUG.get() ? System.nanoTime() : 0;
        long now = System.nanoTime();
        try {
            for (ItemAnimation a : runtime.animations.active()) {
                if (a.inline || a.finished(now)) continue;
                Bounds clip = a.transition.clipRegion();
                if (clip != null) graphics.enableScissor((int)Math.ceil(clip.x()), (int)Math.ceil(clip.y()),
                        (int)Math.floor(clip.x() + clip.width()), (int)Math.floor(clip.y() + clip.height()));
                graphics.pose().pushPose();
                try {
                    Bounds b = a.bounds(now);
                    double scale = a.scale(now) * runtime.emphasis.scale(a.transition.destinationId() == null ? "animation:" + a.handle : a.transition.destinationId(), false, now);
                    graphics.pose().translate(b.centerX(), b.centerY(), com.cappleapple.animatedinventory.api.animation.Clamp.value(a.options.zOrder(), 110, 200));
                    graphics.pose().mulPose(Axis.ZP.rotationDegrees((float)a.rotation(now)));
                    graphics.pose().scale((float)(b.width() / 16 * scale), (float)(b.height() / 16 * scale), 1);
                    graphics.pose().translate(-8, -8, 0);
                    float alpha = (float)a.alpha(now);
                    boolean detached = a.transition.type() == TransitionType.STOW || a.transition.type() == TransitionType.CRAFT || a.transition.type() == TransitionType.RETRIEVE
                            || com.cappleapple.animatedinventory.client.compat.inventoryparticles.InventoryParticlesCompatibility.inlineOnly();
                    if (alpha < .999f) ItemOpacityLayer.draw(graphics, a.transition.stack(), alpha, detached);
                    else if (detached) DetachedItemRenderer.draw(graphics, a.transition.stack(), 0, 0);
                    else drawStack(graphics, a.transition.stack(), 0, 0);
                } finally {
                    graphics.flush(); graphics.pose().popPose();
                    if (clip != null) graphics.disableScissor();
                }
            }
            if (ClientConfig.DEBUG.get()) DebugOverlay.render(graphics, runtime, now);
        } catch (RuntimeException | LinkageError error) { runtime.fail(error); }
        if (start != 0) runtime.renderNanos = System.nanoTime() - start;
    }
    public static void drawStack(GuiGraphics graphics, ItemStack stack, int x, int y) {
        if (Minecraft.getInstance().screen instanceof com.cappleapple.animatedinventory.client.compat.sophisticated.SophisticatedView view) {
            view.animatedinventory$drawStack(graphics, x, y, stack, false, null); return;
        }
        graphics.renderItem(stack, x, y);
        graphics.renderItemDecorations(Minecraft.getInstance().font, stack, x, y);
    }
    private AnimationRenderer() { }
}
