package com.cappleapple.animatedinventory.client.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/** Fades complete GUI item output, including the item atlas, glint, counts and decorations. */
public final class ItemOpacityLayer {
    private static final Map<GuiItemRenderState, Float> OPACITY = Collections.synchronizedMap(new WeakHashMap<>());

    public static void draw(GuiGraphicsExtractor graphics, ItemStack stack, float opacity, boolean detached) {
        var captured = new TextureCompositor.CaptureState();
        var isolated = TextureCompositor.isolate(graphics, captured);
        if (detached) DetachedItemRenderer.draw(isolated, stack, 0, 0);
        else AnimationRenderer.drawStack(isolated, stack, 0, 0);
        TextureCompositor.draw(graphics, captured.snapshot(), new Matrix3x2f(), opacity);
    }
    static void opacity(GuiItemRenderState state, float alpha) { if (alpha < .999f) OPACITY.put(state, alpha); }
    static float opacity(GuiItemRenderState state) { return OPACITY.getOrDefault(state, 1f); }
    public static BlitRenderState itemBlit(BlitRenderState blit, GuiItemRenderState item) {
        return TextureCompositor.tint(blit, opacity(item));
    }
    public static void close() { OPACITY.clear(); TextureCompositor.clear(); }
    private ItemOpacityLayer() { }
}
