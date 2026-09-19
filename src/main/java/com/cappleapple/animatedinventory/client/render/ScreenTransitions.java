package com.cappleapple.animatedinventory.client.render;

import com.cappleapple.animatedinventory.client.ClientConfig;
import com.cappleapple.animatedinventory.client.compat.bundlednotsiloed.BundledCompatibility;
import com.cappleapple.animatedinventory.client.compat.inventoryparticles.InventoryParticlesCompatibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.joml.Matrix3x2f;

/** Only extracted presentation state survives removal; Screens, menus, Slots and callbacks are not retained. */
public final class ScreenTransitions {
    private TextureCompositor.Snapshot live, exit;
    private TextureCompositor.CaptureState capture;
    private GuiGraphicsExtractor destination;
    private long opened, closed;
    private int guiWidth, guiHeight, exitWidth, exitHeight;

    public void open() { opened = System.nanoTime(); }

    public GuiGraphicsExtractor begin(Screen screen, GuiGraphicsExtractor graphics) {
        if (!(screen instanceof AbstractContainerScreen<?>) || capture != null || !ClientConfig.ENABLED.get()
                || Minecraft.getInstance().level == null || InventoryParticlesCompatibility.inlineOnly() || BundledCompatibility.active()
                || !screen.getClass().getPackageName().equals("net.minecraft.client.gui.screens.inventory")) return graphics;
        long now = System.nanoTime();
        boolean opening = ClientConfig.OPEN.get() != ClientConfig.ScreenEffect.NONE
                && now - opened < ClientConfig.nanos(ClientConfig.OPEN_MS.get());
        if (!opening && ClientConfig.CLOSE.get() == ClientConfig.ScreenEffect.NONE) return graphics;
        guiWidth = screen.width;
        guiHeight = screen.height;
        destination = graphics;
        capture = new TextureCompositor.CaptureState();
        return TextureCompositor.isolate(graphics, capture);
    }

    public void finish(GuiGraphicsExtractor graphics, int left, int top) { finishIfNeeded(graphics); }

    public void finishIfNeeded(GuiGraphicsExtractor graphics) {
        if (capture == null) return;
        var recorded = capture;
        var output = destination;
        capture = null;
        destination = null;
        live = recorded.snapshot();
        // The same extractor continues with carried items and tooltips after this boundary.
        recorded.forwardTo(TextureCompositor.state(output));
        double progress = ClientConfig.OPEN_EASING.get().apply((double)(System.nanoTime() - opened) / duration(true));
        draw(output, live, guiWidth, guiHeight, progress, true);
    }

    public void complete(GuiGraphicsExtractor extracted, GuiGraphicsExtractor original) {
        finishIfNeeded(original);
        if (extracted != original) TextureCompositor.copyCursor(extracted, original);
    }
    public void close() {
        exit = null;
        if (live != null && ClientConfig.ENABLED.get() && ClientConfig.CLOSE.get() != ClientConfig.ScreenEffect.NONE) {
            exit = live;
            live = null;
            exitWidth = guiWidth;
            exitHeight = guiHeight;
            closed = System.nanoTime();
        } else discardLive();
    }

    public void renderExit(GuiGraphicsExtractor graphics) {
        if (exit == null) return;
        if (!ClientConfig.ENABLED.get() || Minecraft.getInstance().level == null || graphics.guiWidth() != exitWidth
                || graphics.guiHeight() != exitHeight || System.nanoTime() - closed >= duration(false)) {
            exit = null;
            return;
        }
        graphics.nextStratum();
        draw(graphics, exit, exitWidth, exitHeight,
                ClientConfig.CLOSE_EASING.get().apply((double)(System.nanoTime() - closed) / duration(false)), false);
    }

    private long duration(boolean opening) {
        long value = ClientConfig.nanos(opening ? ClientConfig.OPEN_MS.get() : ClientConfig.CLOSE_MS.get());
        return Math.max(1, ClientConfig.REDUCE_MOTION.get() ? Math.min(value, 60_000_000) : value);
    }

    private void draw(GuiGraphicsExtractor graphics, TextureCompositor.Snapshot state, int width, int height, double progress, boolean opening) {
        var effect = opening ? ClientConfig.OPEN.get() : ClientConfig.CLOSE.get();
        double amount = effect == ClientConfig.ScreenEffect.NONE ? 0 : opening ? 1 - progress : progress;
        boolean reduced = ClientConfig.REDUCE_MOTION.get();
        double scale = !reduced && (effect == ClientConfig.ScreenEffect.SCALE || effect == ClientConfig.ScreenEffect.FADE_SCALE)
                ? 1 + ((opening ? ClientConfig.OPEN_SCALE.get() : ClientConfig.CLOSE_SCALE.get()) - 1) * amount : 1;
        double slide = !reduced && effect == ClientConfig.ScreenEffect.SLIDE
                ? (opening ? ClientConfig.OPEN_SLIDE.get() : ClientConfig.CLOSE_SLIDE.get()) * amount : 0;
        float alpha = (float)(reduced || effect == ClientConfig.ScreenEffect.FADE || effect == ClientConfig.ScreenEffect.FADE_SCALE ? 1 - amount : 1);
        Matrix3x2f transform = new Matrix3x2f().translate((float)(width * (1 - scale) / 2), (float)(height * (1 - scale) / 2 + slide)).scale((float)scale);
        TextureCompositor.draw(graphics, state, transform, alpha);
    }

    public void discardLive() {
        if (capture != null && destination != null) capture.forwardTo(TextureCompositor.state(destination));
        capture = null;
        destination = null;
        live = null;
    }
    public void discard() { discardLive(); exit = null; TextureCompositor.clear(); }
}
