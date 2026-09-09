package com.cappleapple.animatedinventory.client.render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/** A reusable 64-GUI-pixel surface makes fades work for opaque models, glint, and decorations too. */
public final class ItemOpacityLayer {
    private static TextureTarget target;
    public static void draw(GuiGraphics graphics, ItemStack stack, float opacity, boolean detached) {
        var mc = Minecraft.getInstance();
        graphics.flush();
        int framebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4], scissor = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        boolean clipped = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        if (clipped) GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissor);
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting sorting = RenderSystem.getVertexSorting();
        try {
            RenderSystem.disableScissor();
            int size = Math.max(64, (int)Math.ceil(64 * mc.getWindow().getGuiScale()));
            if (target == null || target.width != size) {
                if (target != null) target.destroyBuffers();
                target = new TextureTarget(size, size, true, Minecraft.ON_OSX);
                target.setClearColor(0, 0, 0, 0);
            }
            target.clear(Minecraft.ON_OSX); target.bindWrite(true);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 64, 64, 0, 1000, 21000), VertexSorting.ORTHOGRAPHIC_Z);
            GuiGraphics isolated = new GuiGraphics(mc, graphics.bufferSource());
            if (detached) DetachedItemRenderer.draw(isolated, stack, 24, 24);
            else AnimationRenderer.drawStack(isolated, stack, 24, 24);
            isolated.flush();
        } finally {
            RenderSystem.setProjectionMatrix(projection, sorting);
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            if (clipped) RenderSystem.enableScissor(scissor[0], scissor[1], scissor[2], scissor[3]);
        }
        TextureCompositor.draw(graphics, target, -24, -24, 64, 64, opacity, 0);
    }
    public static void close() { if (target != null) { target.destroyBuffers(); target = null; Minecraft.getInstance().getMainRenderTarget().bindWrite(true); } }
    private ItemOpacityLayer() { }
}
