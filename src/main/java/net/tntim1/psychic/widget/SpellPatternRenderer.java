package net.tntim1.psychic.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

import java.util.List;

public class SpellPatternRenderer {
    private static final float[] BUTTON_X = { 54f, 23f, 21f, 56f, 103f, 135f, 137f, 104f };
    private static final float[] BUTTON_Y = { 135f, 103f, 55f, 22f, 23f, 54f, 103f, 134f };
    private static final float TEX_SIZE = 160f;

    public static void render(GuiGraphics gfx, int x, int y, int size, List<Integer> pattern, String title, net.minecraft.client.gui.Font font, int color) {
        float scale = size / TEX_SIZE;
        Matrix4f matrix = gfx.pose().last().pose();

        // 1. Render Background Card & Border
        // Padding and thickness scale with the widget
        float offset = 4f * scale;
        float borderThickness = Math.max(0.1f, 1.0f * scale);

        // Main Background
        renderRect(matrix, x - offset, y - offset, size + (offset * 2), size + (offset * 2), color);

        // Border
        drawScalableBorder(matrix, x - offset, y - offset, size + (offset * 2), size + (offset * 2), borderThickness, 0xFF444444);

        // 2. Render Scaled Title
        float textScale = scale * 1.1f;
        if (textScale > 0.1f) {
            gfx.pose().pushPose();
            gfx.pose().translate(x + (size / 2f), y - (12 * textScale), 0);
            gfx.pose().scale(textScale, textScale, 1.0f);
            int tw = font.width(title);
            gfx.drawString(font, title, -tw / 2, 0, 0xFFFFFFFF, true);
            gfx.pose().popPose();
        }

        // 3. Draw Nodes (Scaling dots)
        float dotSize = 2.0f * scale;
        for (int i = 0; i < 8; i++) {
            float bx = x + (BUTTON_X[i] * scale);
            float by = y + (BUTTON_Y[i] * scale);
            // Render dot centered on the coordinate
            renderRect(matrix, bx - (dotSize / 2f), by - (dotSize / 2f), dotSize, dotSize, 0xFFAAAAAA);
        }

        // 4. Draw the Pattern Lines
        if (pattern == null || pattern.size() < 2) return;

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < pattern.size() - 1; i++) {
            int startIdx = Math.min(Math.max(pattern.get(i) - 1, 0), 7);
            int endIdx = Math.min(Math.max(pattern.get(i + 1) - 1, 0), 7);

            float x1 = x + BUTTON_X[startIdx] * scale;
            float y1 = y + BUTTON_Y[startIdx] * scale;
            float x2 = x + BUTTON_X[endIdx] * scale;
            float y2 = y + BUTTON_Y[endIdx] * scale;

            bufferbuilder.vertex(matrix, x1, y1, 0).color(1f, 1f, 1f, 1f).endVertex();
            bufferbuilder.vertex(matrix, x2, y2, 0).color(0.7f, 0.8f, 1f, 1f).endVertex();
        }
        tesselator.end();
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