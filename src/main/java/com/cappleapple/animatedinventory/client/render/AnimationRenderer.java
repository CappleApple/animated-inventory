package com.cappleapple.animatedinventory.client.render;

import com.cappleapple.animatedinventory.api.animation.Bounds;
import com.cappleapple.animatedinventory.api.animation.TransitionType;
import com.cappleapple.animatedinventory.client.*;
import com.cappleapple.animatedinventory.client.animation.ItemAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

public final class AnimationRenderer {
    public static void render(GuiGraphicsExtractor graphics, ClientRuntime runtime) {
        long start = ClientConfig.DEBUG.get() ? System.nanoTime() : 0;
        long now = System.nanoTime();
        try {
            var animations = new java.util.ArrayList<>(runtime.animations.active());
            animations.sort(java.util.Comparator.comparingDouble(a -> Math.clamp(a.options.zOrder(), 110, 200)));
            for (ItemAnimation a : animations) {
                if (a.inline || a.finished(now)) continue;
                graphics.nextStratum();
                Bounds clip = a.transition.clipRegion();
                if (clip != null) graphics.enableScissor((int)Math.ceil(clip.x()), (int)Math.ceil(clip.y()),
                        (int)Math.floor(clip.x() + clip.width()), (int)Math.floor(clip.y() + clip.height()));
                graphics.pose().pushMatrix();
                try {
                    Bounds b = a.bounds(now);
                    double scale = a.scale(now) * runtime.emphasis.scale(a.transition.destinationId() == null ? "animation:" + a.handle : a.transition.destinationId(), false, now);
                    graphics.pose().translate((float)b.centerX(), (float)b.centerY());
                    graphics.pose().rotate((float)Math.toRadians(a.rotation(now)));
                    graphics.pose().scale((float)(b.width() / 16 * scale), (float)(b.height() / 16 * scale));
                    graphics.pose().translate(-8, -8);
                    float alpha = (float)a.alpha(now);
                    boolean detached = a.transition.type() == TransitionType.STOW || a.transition.type() == TransitionType.CRAFT || a.transition.type() == TransitionType.RETRIEVE
                            || com.cappleapple.animatedinventory.client.compat.inventoryparticles.InventoryParticlesCompatibility.inlineOnly();
                    if (alpha < .999f) ItemOpacityLayer.draw(graphics, a.transition.stack(), alpha, detached);
                    else if (detached) DetachedItemRenderer.draw(graphics, a.transition.stack(), 0, 0);
                    else drawStack(graphics, a.transition.stack(), 0, 0);
                } finally {
                    graphics.pose().popMatrix();
                    if (clip != null) graphics.disableScissor();
                }
            }
            if (ClientConfig.DEBUG.get()) DebugOverlay.render(graphics, runtime, now);
        } catch (RuntimeException | LinkageError error) { runtime.fail(error); }
        if (start != 0) runtime.renderNanos = System.nanoTime() - start;
    }
    public static void drawStack(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        if (Minecraft.getInstance().gui.screen() instanceof com.cappleapple.animatedinventory.client.compat.sophisticated.SophisticatedView view) {
            view.animatedinventory$drawStack(graphics, x, y, stack, false, null); return;
        }
        graphics.item(stack, x, y);
        graphics.itemDecorations(Platform.countFont(stack), stack, x, y);
    }
    private AnimationRenderer() { }
}
