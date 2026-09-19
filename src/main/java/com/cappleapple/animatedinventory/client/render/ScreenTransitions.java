package com.cappleapple.animatedinventory.client.render;

import com.cappleapple.animatedinventory.client.ClientConfig;
import com.cappleapple.animatedinventory.client.compat.bundlednotsiloed.BundledCompatibility;
import com.cappleapple.animatedinventory.client.compat.inventoryparticles.InventoryParticlesCompatibility;
import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Only GPU images survive removal. No removed Screen, menu, callback, or Slot is retained. */
public final class ScreenTransitions {
    private TextureTarget live, exit;
    private boolean capturing;
    private long opened, closed;
    private int guiWidth, guiHeight, exitWidth, exitHeight;
    public void open() { opened = System.nanoTime(); }
    public void begin(AbstractContainerScreen<?> screen, GuiGraphics graphics) {
        if (capturing || !ClientConfig.ENABLED.get() || Minecraft.getInstance().level == null
                || InventoryParticlesCompatibility.inlineOnly() || BundledCompatibility.active()
                || !screen.getClass().getName().startsWith("net.minecraft.")) return;
        long now = System.nanoTime();
        boolean opening = ClientConfig.OPEN.get() != ClientConfig.ScreenEffect.NONE && now - opened < ClientConfig.nanos(ClientConfig.OPEN_MS.get());
        if (!opening && ClientConfig.CLOSE.get() == ClientConfig.ScreenEffect.NONE) return;
        graphics.flush();
        var main = Minecraft.getInstance().getMainRenderTarget();
        try {
            if (live == null || live.width != main.width || live.height != main.height) {
                discardLive();
                live = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
                live.setClearColor(0, 0, 0, 0);
            }
            guiWidth = screen.width; guiHeight = screen.height;
            live.clear(Minecraft.ON_OSX); live.bindWrite(true); capturing = true;
        } catch (RuntimeException error) { discardLive(); main.bindWrite(true); }
    }
    public void finish(GuiGraphics graphics, int left, int top) {
        if (!capturing) return;
        graphics.flush(); capturing = false;
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(-left, -top, 0);
            double time = (double)(System.nanoTime() - opened) / duration(true);
            double progress = ClientConfig.OPEN_EASING.get().apply(time);
            draw(graphics, live, guiWidth, guiHeight, progress, true);
        } catch (RuntimeException error) {
            com.mojang.logging.LogUtils.getLogger().warn("Animated Inventory screen image effect skipped", error);
            discardLive();
        } finally { graphics.pose().popPose(); }
    }
    /** Wrapper cleanup if another mod short-circuits the foreground event. */
    public void finishIfNeeded(GuiGraphics graphics) { finish(graphics, 0, 0); }
    public void close() {
        if (exit != null) { exit.destroyBuffers(); exit = null; }
        if (live != null && ClientConfig.ENABLED.get() && ClientConfig.CLOSE.get() != ClientConfig.ScreenEffect.NONE) {
            exit = live; live = null; exitWidth = guiWidth; exitHeight = guiHeight; closed = System.nanoTime();
        } else discardLive();
        Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
    }
    public void renderExit(GuiGraphics graphics) {
        if (exit == null) return;
        var mc = Minecraft.getInstance();
        if (!ClientConfig.ENABLED.get() || mc.level == null || graphics.guiWidth() != exitWidth || graphics.guiHeight() != exitHeight
                || System.nanoTime() - closed >= duration(false)) {
            exit.destroyBuffers(); exit = null; mc.getMainRenderTarget().bindWrite(true); return;
        }
        try {
            draw(graphics, exit, exitWidth, exitHeight, ClientConfig.CLOSE_EASING.get().apply((double)(System.nanoTime() - closed) / duration(false)), false);
        } catch (RuntimeException error) {
            com.mojang.logging.LogUtils.getLogger().warn("Animated Inventory exit image discarded", error);
            discard();
        }
    }
    private long duration(boolean opening) {
        long value = ClientConfig.nanos(opening ? ClientConfig.OPEN_MS.get() : ClientConfig.CLOSE_MS.get());
        return ClientConfig.REDUCE_MOTION.get() ? Math.min(value, 60_000_000) : value;
    }
    private void draw(GuiGraphics graphics, TextureTarget target, int width, int height, double progress, boolean opening) {
        var effect = opening ? ClientConfig.OPEN.get() : ClientConfig.CLOSE.get();
        double amount = opening ? 1 - progress : progress;
        boolean reduced = ClientConfig.REDUCE_MOTION.get();
        double scale = !reduced && (effect == ClientConfig.ScreenEffect.SCALE || effect == ClientConfig.ScreenEffect.FADE_SCALE)
                ? 1 + ((opening ? ClientConfig.OPEN_SCALE.get() : ClientConfig.CLOSE_SCALE.get()) - 1) * amount : 1;
        double slide = !reduced && effect == ClientConfig.ScreenEffect.SLIDE
                ? (opening ? ClientConfig.OPEN_SLIDE.get() : ClientConfig.CLOSE_SLIDE.get()) * amount : 0;
        float alpha = (float)(reduced || effect == ClientConfig.ScreenEffect.FADE || effect == ClientConfig.ScreenEffect.FADE_SCALE ? 1 - amount : 1);
        TextureCompositor.draw(graphics, target, width * (1 - scale) / 2, height * (1 - scale) / 2 + slide,
                width * scale, height * scale, alpha, 0);
    }
    public void discardLive() {
        capturing = false;
        if (live != null) { live.destroyBuffers(); live = null; Minecraft.getInstance().getMainRenderTarget().bindWrite(true); }
    }
    public void discard() {
        discardLive();
        if (exit != null) { exit.destroyBuffers(); exit = null; Minecraft.getInstance().getMainRenderTarget().bindWrite(true); }
    }
}
