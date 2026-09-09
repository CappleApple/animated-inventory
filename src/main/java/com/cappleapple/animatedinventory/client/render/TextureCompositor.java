package com.cappleapple.animatedinventory.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/** Composites a transparent render target; captured color is premultiplied by its alpha. */
public final class TextureCompositor {
    public static void draw(GuiGraphics graphics, RenderTarget target, double x, double y, double width, double height, float alpha, float z) {
        graphics.flush();
        float a = Math.clamp(alpha, 0, 1);
        boolean depth = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
        boolean blend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
        RenderSystem.disableDepthTest(); RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, target.getColorTextureId());
        RenderSystem.setShaderColor(1, 1, 1, 1);
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(matrix, (float)x, (float)(y + height), z).setUv(0, 0).setColor(a, a, a, a);
        buffer.addVertex(matrix, (float)(x + width), (float)(y + height), z).setUv(1, 0).setColor(a, a, a, a);
        buffer.addVertex(matrix, (float)(x + width), (float)y, z).setUv(1, 1).setColor(a, a, a, a);
        buffer.addVertex(matrix, (float)x, (float)y, z).setUv(0, 1).setColor(a, a, a, a);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.defaultBlendFunc(); RenderSystem.depthMask(true);
        if (depth) RenderSystem.enableDepthTest();
        if (!blend) RenderSystem.disableBlend();
    }
    private TextureCompositor() { }
}
