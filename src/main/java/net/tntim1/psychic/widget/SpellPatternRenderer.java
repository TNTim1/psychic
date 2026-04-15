package net.tntim1.psychic.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.List;

public class SpellPatternRenderer {
    private static final float[] BUTTON_X = { 54f, 23f, 21f, 56f, 103f, 135f, 137f, 104f };
    private static final float[] BUTTON_Y = { 135f, 103f, 55f, 22f, 23f, 54f, 103f, 134f };
    private static final float TEX_SIZE = 160f;

    public static void render(GuiGraphics gfx, int x, int y, int size, List<Integer> pattern,
                              String title, ResourceLocation texture, // Add texture parameter
                              net.minecraft.client.gui.Font font, int color) {

        float scale = size / TEX_SIZE;
        Matrix4f matrix = gfx.pose().last().pose();

        // 1. Draw the Background Card (The green/gray tint)
        float offset = 4f * scale;
        renderRect(matrix, x - offset, y - offset, size + (offset * 2), size + (offset * 2), color);

        // 2. DRAW THE SPELL IMAGE
        // We render the texture inside the widget box
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();

        // Draw the texture slightly smaller than the full box for a nice margin
        int imgSize = Math.round(size * 0.8f);
        int imgOffset = (size - imgSize) / 2;
        gfx.blit(texture, x + imgOffset, y + imgOffset, 0, 0, imgSize, imgSize, imgSize, imgSize);
        // 2. Render Nodes (Dots)
        float dotSize = 2.5f * scale;
        for (int i = 0; i < 8; i++) {
            float bx = x + (BUTTON_X[i] * scale);
            float by = y + (BUTTON_Y[i] * scale);
            renderRect(matrix, bx - (dotSize / 2f), by - (dotSize / 2f), dotSize, dotSize, 0xFFAAAAAA);
        }

        // 3. Draw the Pattern Connections as WHITE Quads
        if (pattern == null || pattern.size() < 2) return;

        // Increased thickness for better visibility
        float thickness = 2.0f * scale;

        // Explicitly set to WHITE instead of using the 'color' variable
        float r = 1.0f;
        float g = 1.0f;
        float b = 1.0f;
        float a = 0.9f;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        // Step by 2 for discrete connections
        for (int i = 0; i < pattern.size() - 1; i += 2) {
            int startIdx = Math.min(Math.max(pattern.get(i) - 1, 0), 7);
            int endIdx = Math.min(Math.max(pattern.get(i + 1) - 1, 0), 7);

            float x1 = x + BUTTON_X[startIdx] * scale;
            float y1 = y + BUTTON_Y[startIdx] * scale;
            float x2 = x + BUTTON_X[endIdx] * scale;
            float y2 = y + BUTTON_Y[endIdx] * scale;

            float dx = x2 - x1;
            float dy = y2 - y1;
            float len = (float) Math.sqrt(dx * dx + dy * dy);

            if (len < 0.1f) continue;

            // Normal vector for thickness
            float nx = -dy / len * (thickness / 2f);
            float ny = dx / len * (thickness / 2f);

            // Quad corners
            bufferbuilder.vertex(matrix, x1 - nx, y1 - ny, 0).color(r, g, b, a).endVertex();
            bufferbuilder.vertex(matrix, x1 + nx, y1 + ny, 0).color(r, g, b, a).endVertex();
            bufferbuilder.vertex(matrix, x2 + nx, y2 + ny, 0).color(r, g, b, a).endVertex();
            bufferbuilder.vertex(matrix, x2 - nx, y2 - ny, 0).color(r, g, b, a).endVertex();
        }

        Tesselator.getInstance().end();
        RenderSystem.disableBlend();
    }

    private static void renderRect(Matrix4f matrix, float x, float y, float w, float h, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferbuilder.vertex(matrix, x, y + h, 0).color(r, g, b, a).endVertex();
        bufferbuilder.vertex(matrix, x + w, y + h, 0).color(r, g, b, a).endVertex();
        bufferbuilder.vertex(matrix, x + w, y, 0).color(r, g, b, a).endVertex();
        bufferbuilder.vertex(matrix, x, y, 0).color(r, g, b, a).endVertex();
        tesselator.end();

        RenderSystem.disableBlend();
    }

    private static void drawScalableBorder(Matrix4f matrix, float x, float y, float w, float h, float thickness, int color) {
        renderRect(matrix, x, y, w, thickness, color); // Top
        renderRect(matrix, x, y + h - thickness, w, thickness, color); // Bottom
        renderRect(matrix, x, y, thickness, h, color); // Left
        renderRect(matrix, x + w - thickness, y, thickness, h, color); // Right
    }
}