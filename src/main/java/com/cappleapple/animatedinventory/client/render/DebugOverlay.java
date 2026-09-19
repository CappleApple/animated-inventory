package com.cappleapple.animatedinventory.client.render;

import com.cappleapple.animatedinventory.api.animation.Bounds;
import com.cappleapple.animatedinventory.client.ClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class DebugOverlay {
    public static void render(GuiGraphicsExtractor graphics, ClientRuntime runtime, long now) {
        var snapshot = runtime.snapshot();
        if (snapshot == null) return;
        graphics.pose().pushMatrix();
        try {
            graphics.nextStratum();
            for (var item : snapshot.items().values()) if (item.visible()) outline(graphics, item.bounds(), 0xff77bbff);
            for (var a : runtime.animations.active()) {
                outline(graphics, a.source, 0xfff5c36b); outline(graphics, a.destination, 0xffa2e68c); outline(graphics, a.bounds(now), 0xffffffff);
                if (a.transition.clipRegion() != null) outline(graphics, a.transition.clipRegion(), 0xffe2a9f1);
                for (int i = 0; i <= 12; i++) {
                    Bounds b = a.options.style().sample(a.source, a.destination, i / 12.0, a.options.easing(), a.options.arcHeight());
                    graphics.fill((int)b.centerX(), (int)b.centerY(), (int)b.centerX() + 1, (int)b.centerY() + 1, 0xffeeeeee);
                }
                graphics.text(Minecraft.getInstance().font, a.transition.type() + " " + a.transactionId, (int)a.source.x(), (int)a.source.y() - 8, 0xffffffff);
            }
            var font = Minecraft.getInstance().font;
            String[] labels = { "Animated Inventory | screen " + snapshot.owner() + " | " + snapshot.providerId(),
                    "Active " + runtime.animations.active().size() + " | transactions " + runtime.transactions + " | comparisons " + runtime.comparisons,
                    "Snapshots " + runtime.snapshots + " | cancelled " + runtime.animations.cancelled + " | render " + runtime.renderNanos / 1000 + " us",
                    "Boxes: logical / source / destination / current / clip; dotted line: trajectory" };
            for (int i = 0; i < labels.length; i++) {
                graphics.fill(3, 3 + i * 11, 8 + font.width(labels[i]), 14 + i * 11, 0xcc101010);
                graphics.text(font, labels[i], 5, 5 + i * 11, 0xffffffff);
            }
        } finally { graphics.pose().popMatrix(); }
    }
    private static void outline(GuiGraphicsExtractor graphics, Bounds bounds, int color) {
        graphics.outline((int)bounds.x(), (int)bounds.y(), (int)bounds.width(), (int)bounds.height(), color);
    }
    private DebugOverlay() { }
}
