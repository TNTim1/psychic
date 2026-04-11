package net.tntim1.psychic.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.tntim1.psychic.network.ActivateWidgetPacket;
import net.tntim1.psychic.network.DeactivateWidgetPacket;
import net.tntim1.psychic.network.ModPackets;
import net.tntim1.psychic.player_data.ClientKnowledge;
import net.tntim1.psychic.widget.TabDefinition;
import net.tntim1.psychic.widget.TabRegistry;
import net.tntim1.psychic.widget.WidgetDefinition;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Atlas Codex GUI with full dependency support.
 *
 * <h3>Widget states</h3>
 * <ul>
 *   <li><b>Locked</b>   – one or more dependencies not yet met.
 *       Widget is drawn with a lock icon tint. Clicking it does nothing.</li>
 *   <li><b>Available</b> – all dependencies met, but not yet activated.
 *       Popup opens; ACTIVATE button is enabled.</li>
 *   <li><b>Unlocked</b>  – activated. Popup shows "UNLOCKED" (green).</li>
 * </ul>
 *
 * <h3>Dependency lines</h3>
 * Lines are drawn from the centre of each dependency widget to the centre of
 * the widget that depends on it. Colour reflects the dependency's unlock state.
 */
public class AtlasCodexScreen extends Screen {

    // ── textures ──────────────────────────────────────────────────────────────
    private static final ResourceLocation WIDGET_BG_INACTIVE          = new ResourceLocation("psychic", "textures/gui/widgets/widget_background_inactive.png");
    private static final ResourceLocation WIDGET_BG_INACTIVE_SELECTED = new ResourceLocation("psychic", "textures/gui/widgets/widget_background_inactive_selected.png");
    private static final ResourceLocation WIDGET_BG_ACTIVE            = new ResourceLocation("psychic", "textures/gui/widgets/widget_background_active.png");
    private static final ResourceLocation WIDGET_BG_ACTIVE_SELECTED   = new ResourceLocation("psychic", "textures/gui/widgets/widget_background_active_selected.png");
    private static final ResourceLocation WIDGET_BG_LOCKED  = new ResourceLocation("psychic", "textures/gui/widgets/widget_background_locked.png");
    // ── palette ───────────────────────────────────────────────────────────────
    private static final int C_BORDER         = 0xFFd4a9a2;
    private static final int C_ACCENT         = 0xFF392420;
    private static final int C_TAB_INACTIVE   = 0xFF000000;
    private static final int C_TAB_HOVER      = 0xFFd18a7d;
    private static final int C_CANVAS_BG      = 0xFF333333;
    private static final int C_POPUP_BG       = 0xFFd4a9a2;
    private static final int C_POPUP_BORDER   = 0xFF392420;
    private static final int C_TEXT           = 0xFF000000;
    private static final int C_TEXT_DIM       = 0xFF000000;

    // ── layout ───────────────────────────────────────────────────────────────
    private static final int TAB_H     = 26;
    private static final int FRAME_PAD = 6;
    private static final int HEADER_H  = 26;

    // ── zoom ─────────────────────────────────────────────────────────────────
    private static final float ZOOM_MIN  = 0.02f;
    private static final float ZOOM_MAX  = 5.0f;
    private static final float ZOOM_STEP = 0.12f;

    // ── state ─────────────────────────────────────────────────────────────────
    private final List<TabDefinition> tabs = TabRegistry.getTabs();
    private int   activeTab = 0;
    private float scrollX   = 0, scrollY = 0;
    private float zoom      = 1.0f;

    private boolean dragging;
    private double  dragLastX, dragLastY;
    private int     hoveredWidget = -1;
    private PopupState popup = null;

    // ── derived layout ────────────────────────────────────────────────────────
    private int frameX, frameY, frameW, frameH;
    private int canvasX, canvasY, canvasW, canvasH;

    // ── popup record ──────────────────────────────────────────────────────────
    private record PopupState(WidgetDefinition widget, int originX, int originY) {}

    // ─────────────────────────────────────────────────────────────────────────

    public AtlasCodexScreen() {
        super(Component.literal("Atlas Codex"));
    }

    // Track which widgets were already unlocked when we opened the GUI
    private final java.util.Set<String> alreadySeenUnlocked = new java.util.HashSet<>();
    // Global timer for the "new" connection growth
    private long openTime;

    @Override
    protected void init() {
        super.init();
        recalcLayout();
        this.openTime = System.currentTimeMillis();

        // Capture the state of knowledge AT THE MOMENT the screen is opened
        this.alreadySeenUnlocked.clear();
        for (TabDefinition tab : tabs) {
            for (WidgetDefinition w : tab.widgets) {
                if (ClientKnowledge.isUnlocked(w.id)) {
                    alreadySeenUnlocked.add(w.id);
                }
            }
        }
    }

    private void recalcLayout() {
        int m = 18;
        frameX = m;
        frameY = m;
        frameW = width  - m * 2 - TAB_H;
        frameH = height - m * 2;

        canvasX = frameX + FRAME_PAD;
        canvasY = frameY + FRAME_PAD + HEADER_H;
        canvasW = frameW - FRAME_PAD * 2;
        canvasH = frameH - FRAME_PAD * 2 - HEADER_H;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float pt) {
        recalcLayout();
        renderBackground(gfx);

        // Outer frame
        gfx.fill(frameX, frameY, frameX + frameW, frameY + frameH, C_BORDER);
        drawBorder(gfx, frameX, frameY, frameW, frameH, 2, C_ACCENT);

        // Section title
        if (!tabs.isEmpty()) {
            String title = tabs.get(activeTab).name.toUpperCase();
            gfx.drawString(font, title, frameX + 12, frameY + 12, C_ACCENT, false);
            int lineY = frameY + HEADER_H + 2;
            gfx.fill(frameX + 10, lineY, frameX + frameW - 10, lineY + 1, C_TEXT);
        }

        renderTabBar(gfx, mx, my);
        renderCanvas(gfx, mx, my);
        if (popup != null) renderPopup(gfx, mx, my);

        super.render(gfx, mx, my, pt);
    }

    // ── Tab bar ───────────────────────────────────────────────────────────────

    private void renderTabBar(GuiGraphics gfx, int mx, int my) {
        int tabW = 24, tabH = 24, spacing = 2;
        for (int i = 0; i < tabs.size(); i++) {
            TabDefinition tab = tabs.get(i);
            int tx = frameX + frameW;
            int ty = frameY + (i * (tabH + spacing));

            boolean active  = i == activeTab;
            boolean hovered = !active && mx >= tx && mx < tx + tabW && my >= ty && my < ty + tabH;

            int bg = active ? C_BORDER : (hovered ? C_TAB_HOVER : C_TAB_INACTIVE);
            gfx.fill(tx, ty, tx + tabW, ty + tabH, bg);
            drawBorder(gfx, tx, ty, tabW, tabH, active ? 2 : 1, C_ACCENT);
            if (active) gfx.fill(tx - 5, ty + 2, tx + tabW - 2, ty + tabH - 2, bg);

            RenderSystem.setShaderTexture(0, tab.icon);
            RenderSystem.enableBlend();
            gfx.blit(tab.icon, tx + 4, ty + 4, 0, 0, 16, 16, 16, 16);
            RenderSystem.disableBlend();
        }
    }

    // ── Canvas ────────────────────────────────────────────────────────────────

    private void renderCanvas(GuiGraphics gfx, int mx, int my) {
        gfx.fill(canvasX, canvasY, canvasX + canvasW, canvasY + canvasH, C_CANVAS_BG);
        enableScissor(canvasX, canvasY, canvasW, canvasH);

        if (!tabs.isEmpty()) {
            TabDefinition tab = tabs.get(activeTab);
            Map<String, int[]> centres = buildCentreMap(tab);

            // --- STEP 1: DRAW ALL LINES (Gray and Animated) ---
            // By calling this first, everything inside this method stays behind the widgets
            renderDependencyLines(gfx, tab, centres);


            // Finalize the line buffer before starting widgets
            gfx.flush();

            // --- STEP 2: DRAW ALL WIDGETS ---
            hoveredWidget = -1;
            for (int i = 0; i < tab.widgets.size(); i++) {
                WidgetDefinition w = tab.widgets.get(i);
                int wx = canvasToScreenX(w.canvasX);
                int wy = canvasToScreenY(w.canvasY);
                int ww = Math.max(1, (int)(w.iconW * zoom));
                int wh = Math.max(1, (int)(w.iconH * zoom));
                int pad = 4;

                boolean depsOk = ClientKnowledge.areDependenciesMet(w.id);
                boolean hov    = popup == null && depsOk
                        && mx >= wx - pad && mx < wx + ww + pad
                        && my >= wy - pad && my < wy + wh + pad;
                if (hov) hoveredWidget = i;

                renderWidget(gfx, w, wx, wy, ww, wh, hov, depsOk);
            }

            if (hoveredWidget >= 0) {
                drawTooltip(gfx, tab.widgets.get(hoveredWidget).label, mx, my);
            }
        }

        disableScissor();
    }

    /**
     * Builds a map of widgetId → [screenCentreX, screenCentreY] for every
     * widget in the current tab, used to draw dependency lines.
     */
    private Map<String, int[]> buildCentreMap(TabDefinition tab) {
        Map<String, int[]> map = new HashMap<>();
        for (WidgetDefinition w : tab.widgets) {
            int ww = Math.max(1, (int)(w.iconW * zoom));
            int wh = Math.max(1, (int)(w.iconH * zoom));
            int cx = canvasToScreenX(w.canvasX) + ww / 2;
            int cy = canvasToScreenY(w.canvasY) + wh / 2;
            map.put(w.id, new int[]{cx, cy});
        }
        return map;
    }

    /**
     * Draws lines from each dependency's centre to the dependent widget's centre.
     * Green = dependency is unlocked, Red = not yet unlocked.
     */
    /**
     * Renders animated, scaling gradient lines for dependencies.
     */

    /**
     * Renders animated, scaling gradient lines for dependencies.
     * Draws a base grey line for all connections, and an animated orange line for unlocked ones.
     */
    private void renderDependencyLines(GuiGraphics gfx, TabDefinition tab, Map<String, int[]> centres) {
        long currentTime = System.currentTimeMillis();
        float animationDuration = 1500f;
        float growthProgress = Mth.clamp((currentTime - openTime) / animationDuration, 0f, 1f);

        // PASS 1: The Grey "Inactive" Lines (Static)
        for (WidgetDefinition w : tab.widgets) {
            if (!w.hasDependencies()) continue;
            int[] to = centres.get(w.id);
            if (to == null) continue;

            for (String depId : w.dependencies) {
                int[] from = centres.get(depId);
                if (from == null) continue;

                // Simple static line: 1 segment, 0 jitter
                drawGradientLine(gfx, from[0], from[1], to[0], to[1],
                        0xFF999999, 0xFF999999, 1.0f, 0.0f, 0.5f,
                        0f, 1);
            }
        }

        // PASS 2: The Animated "Active" Lines (Jittery Energy)
        for (WidgetDefinition w : tab.widgets) {
            if (!w.hasDependencies()) continue;
            int[] to = centres.get(w.id);
            if (to == null) continue;

            for (String depId : w.dependencies) {
                int[] from = centres.get(depId);
                if (from == null) continue;

                if (ClientKnowledge.isUnlocked(depId)) {
                    // Determine animation progress
                    boolean isNew = !alreadySeenUnlocked.contains(depId);
                    float progress = isNew ? growthProgress : 1.0f;

                    float x2_anim = from[0] + (to[0] - from[0]) * progress;
                    float y2_anim = from[1] + (to[1] - from[1]) * progress;

                    // Calculate distance-based segments
                    float dx = to[0] - from[0];
                    float dy = to[1] - from[1];
                    float actualLen = Mth.sqrt(dx * dx + dy * dy);

                    // Scale segments based on length: approx 1 segment per 10 pixels, min 4
                    int segments = Math.max(4, (int)(actualLen / 10f));

                    // The Energy Line
                    drawGradientLine(gfx, from[0], from[1], x2_anim, y2_anim,
                            0xAACC5500, 0xAAFFD700, 2.2f, 0.0f, 1.0f,
                            6.0f, segments);

                    // Inner  Core
                    drawGradientLine(gfx, from[0], from[1], x2_anim, y2_anim,
                            0xFFFFFFFF, 0xAAFFD700, 0.8f, 0.0f, 0.7f,
                            10.0f, segments);
                }
            }
        }
    }

    private void drawGradientLine(GuiGraphics gfx, float x1, float y1, float x2, float y2,
                                  int color1, int color2, float thicknessMult, float z, float alphaMult,
                                  float jitter, int segments) {

        if (Math.abs(x1 - x2) < 0.1f && Math.abs(y1 - y2) < 0.1f) return;

        Matrix4f matrix = gfx.pose().last().pose();
        // Using the standard GUI render type for transparency support
        com.mojang.blaze3d.vertex.VertexConsumer consumer = gfx.bufferSource().getBuffer(net.minecraft.client.renderer.RenderType.gui());

        // Color extraction
        float a1 = ((color1 >> 24) & 0xFF) / 255f * alphaMult;
        float r1 = ((color1 >> 16) & 0xFF) / 255f;
        float g1 = ((color1 >> 8) & 0xFF) / 255f;
        float b1 = (color1 & 0xFF) / 255f;

        float a2 = ((color2 >> 24) & 0xFF) / 255f * alphaMult;
        float r2 = ((color2 >> 16) & 0xFF) / 255f;
        float g2 = ((color2 >> 8) & 0xFF) / 255f;
        float b2 = (color2 & 0xFF) / 255f;

        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);

        // Normal vectors for calculating thickness offsets
        float nx = -(dy / len);
        float ny = (dx / len);

        float baseThickness = thicknessMult * zoom;

        // Use a faster seed (approx 33 FPS) for erratic energy flickering
        long frameSeed = (System.currentTimeMillis() / 30);
        java.util.Random rand = new java.util.Random();

        float lastX = x1;
        float lastY = y1;
        float lastThickness = baseThickness;

        for (int i = 1; i <= segments; i++) {
            float t = (float) i / segments;

            // Linear interpolation target
            float nextX = x1 + dx * t;
            float nextY = y1 + dy * t;
            float currentThickness = baseThickness;

            // Apply chaotic displacement to intermediate points
            if (i < segments) {
                // Seed unique to this specific segment and time frame
                rand.setSeed(frameSeed + i + (long)(x1 * 7919));

                // Sharp jitter: Snap the vertex to a random offset
                float intensity = rand.nextFloat() * jitter * zoom;
                nextX += (rand.nextFloat() - 0.5f) * intensity;
                nextY += (rand.nextFloat() - 0.5f) * intensity;

                // Width jitter: Makes the beam pulse and look unstable
                currentThickness *= (0.6f + rand.nextFloat() * 0.8f);
            }

            // Lerp colors across the segments
            float currR = Mth.lerp(t, r1, r2);
            float currG = Mth.lerp(t, g1, g2);
            float currB = Mth.lerp(t, b1, b2);
            float currA = Mth.lerp(t, a1, a2);

            // Calculate segment offsets
            float offX1 = nx * (lastThickness / 2f);
            float offY1 = ny * (lastThickness / 2f);
            float offX2 = nx * (currentThickness / 2f);
            float offY2 = ny * (currentThickness / 2f);

            // Quad composition for this segment
            consumer.vertex(matrix, lastX - offX1, lastY - offY1, z).color(currR, currG, currB, currA).endVertex();
            consumer.vertex(matrix, lastX + offX1, lastY + offY1, z).color(currR, currG, currB, currA).endVertex();
            consumer.vertex(matrix, nextX + offX2, nextY + offY2, z).color(currR, currG, currB, currA).endVertex();
            consumer.vertex(matrix, nextX - offX2, nextY - offY2, z).color(currR, currG, currB, currA).endVertex();

            // Pass variables to the next segment iteration
            lastX = nextX;
            lastY = nextY;
            lastThickness = currentThickness;
        }
    }



    private void renderWidget(GuiGraphics gfx, WidgetDefinition w,
                              int wx, int wy, int ww, int wh,
                              boolean hovered, boolean depsOk) {
        int pad = (int)(4 * zoom);
        int bgX = wx - pad, bgY = wy - pad;
        int bgW = ww + (pad * 2), bgH = wh + (pad * 2);

        boolean isUnlocked = ClientKnowledge.isUnlocked(w.id);

        // Choose background texture: locked widgets always use inactive variant
        ResourceLocation bgTexture;
        if (!depsOk) {
            bgTexture = WIDGET_BG_LOCKED;
        } else if (isUnlocked) {
            bgTexture = hovered ? WIDGET_BG_ACTIVE_SELECTED : WIDGET_BG_ACTIVE;
        } else {
            bgTexture = hovered ? WIDGET_BG_INACTIVE_SELECTED : WIDGET_BG_INACTIVE;
        }

        RenderSystem.setShaderTexture(0, bgTexture);
        RenderSystem.enableBlend();
        gfx.blit(bgTexture, bgX, bgY, 0, 0, bgW, bgH, bgW, bgH);

        // Icon
        RenderSystem.setShaderTexture(0, w.iconTexture);
        gfx.blit(w.iconTexture, wx, wy, 0, 0, ww, wh, ww, wh);

        RenderSystem.disableBlend();
    }

    // ── Popup ─────────────────────────────────────────────────────────────────

    private void renderPopup(GuiGraphics gfx, int mx, int my) {
        if (popup == null) return;
        WidgetDefinition w = popup.widget;

        boolean isUnlocked  = ClientKnowledge.isUnlocked(w.id);
        boolean depsOk      = ClientKnowledge.areDependenciesMet(w.id);
        List<String> missing = ClientKnowledge.getMissingDependencyLabels(w.id);

        int popW = 210, popH = 160;   // slightly taller to fit missing-deps text
        int px = Mth.clamp(popup.originX + 18, frameX + 4, frameX + frameW - popW - 4);
        int py = Mth.clamp(popup.originY - popH / 2, frameY + TAB_H + 4, frameY + frameH - popH - 4);

        // Shadow
        gfx.fill(px + 5, py + 5, px + popW + 5, py + popH + 5, 0x22000000);
        // Background
        gfx.fill(px, py, px + popW, py + popH, C_POPUP_BG);
        drawBorder(gfx, px, py, popW, popH, 2, C_POPUP_BORDER);
        drawCornerAccents(gfx, px, py, popW, popH);

        // Title bar
        gfx.fill(px, py, px + popW, py + 20, C_BORDER);
        gfx.fill(px, py + 20, px + popW, py + 21, C_ACCENT);

        if (w.iconW > 0 && w.iconH > 0) {
            RenderSystem.setShaderTexture(0, w.iconTexture);
            RenderSystem.enableBlend();
            gfx.blit(w.iconTexture, px + 4, py + 4, 0, 0, 12, 12, 12, 12);
            RenderSystem.disableBlend();
        }
        gfx.drawString(font, w.label, px + 20, py + 6, C_ACCENT, false);

        // Close ×
        boolean closeHov = mx >= px + popW - 14 && mx < px + popW && my >= py && my < py + 20;
        gfx.drawString(font, "×", px + popW - 10, py + 6, closeHov ? 0x000000 : C_TEXT, false);

        // Body — either normal content or a "missing deps" explanation
        int bx = px + 6, by = py + 24, bw = popW - 12, bh = popH - 50;
        int lh = font.lineHeight + 2;

        if (!depsOk) {
            // Show what's still needed
            gfx.drawString(font, "§cRequires:", bx, by, C_TEXT, false);
            int cy = by + lh;
            for (String dep : missing) {
                if (cy + lh > by + bh) break;
                gfx.fill(bx, cy + lh / 2 - 1, bx + 3, cy + lh / 2 + 2, 0xFFCC4444);
                gfx.drawString(font, dep, bx + 8, cy, 0xFFCC4444, false);
                cy += lh;
            }
        } else {
            switch (w.popupType) {
                case INFO   -> renderBodyInfo(gfx, w.popupData, bx, by, bw, bh, lh);
                case LIST   -> renderBodyList(gfx, w.popupData, bx, by, bw, bh, lh);
                case IMAGE  -> renderBodyImage(gfx, w.popupData, bx, by, bw, bh);
                case CUSTOM -> gfx.drawString(font, "[custom:" + w.popupData + "]", bx, by, C_TEXT_DIM, false);
            }
        }

        // ── ACTIVATE / LOCKED button ──────────────────────────────────────────
        int btnW = 80, btnH = 16;
        int btnX = px + (popW - btnW) / 2;
        int btnY = py + popH - 22;

        boolean btnHov = mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH;

        String btnText;
        int    btnCol;

        if (isUnlocked) {
            btnText = "UNLOCKED";
            btnCol  = 0xFF55FF55;
        } else if (!depsOk) {
            btnText = "LOCKED";
            btnCol  = 0xFF666666;   // grey — not clickable
        } else {
            btnText = "ACTIVATE";
            btnCol  = btnHov ? 0xFFc4a080 : C_BORDER;
        }

        gfx.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnCol);
        drawBorder(gfx, btnX, btnY, btnW, btnH, 1, C_ACCENT);
        int textW = font.width(btnText);
        gfx.drawString(font, btnText, btnX + (btnW - textW) / 2, btnY + 4, C_ACCENT, false);
    }

    // ── Popup body renderers ──────────────────────────────────────────────────

    private void renderBodyInfo(GuiGraphics gfx, String text,
                                int x, int y, int maxW, int maxH, int lh) {
        int cy = y;
        for (String raw : text.split("\n")) {
            for (var seq : font.split(Component.literal(raw), maxW)) {
                if (cy + lh > y + maxH) return;
                gfx.drawString(font, seq, x, cy, C_TEXT, false);
                cy += lh;
            }
        }
    }

    private void renderBodyList(GuiGraphics gfx, String data,
                                int x, int y, int maxW, int maxH, int lh) {
        int cy = y;
        for (String entry : data.split("\n")) {
            if (cy + lh > y + maxH) break;
            gfx.fill(x, cy + lh / 2 - 1, x + 3, cy + lh / 2 + 2, C_ACCENT);
            gfx.drawString(font, entry, x + 8, cy, C_TEXT, false);
            cy += lh;
        }
    }

    private void renderBodyImage(GuiGraphics gfx, String texStr,
                                 int x, int y, int maxW, int maxH) {
        try {
            ResourceLocation rl = new ResourceLocation(texStr);
            int sz = Math.min(maxW, maxH);
            RenderSystem.setShaderTexture(0, rl);
            RenderSystem.enableBlend();
            gfx.blit(rl, x + (maxW - sz) / 2, y, 0, 0, sz, sz, sz, sz);
            RenderSystem.disableBlend();
        } catch (Exception e) {
            gfx.drawString(font, "img: " + texStr, x, y, C_TEXT_DIM, false);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INPUT
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {

        // ── Popup is open ─────────────────────────────────────────────────────
        if (popup != null) {
            int popW = 210, popH = 160;
            int px = Mth.clamp(popup.originX + 18, frameX + 4, frameX + frameW - popW - 4);
            int py = Mth.clamp(popup.originY - popH / 2, frameY + TAB_H + 4, frameY + frameH - popH - 4);

            int btnW = 80, btnH = 16;
            int btnX = px + (popW - btnW) / 2;
            int btnY = py + popH - 22;

            if (mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH) {
                String id        = popup.widget.id;
                boolean unlocked = ClientKnowledge.isUnlocked(id);
                boolean depsOk   = ClientKnowledge.areDependenciesMet(id);

                if (!unlocked && depsOk) {
                    // Client already verified deps — server will double-check
                    ModPackets.CHANNEL.sendToServer(new ActivateWidgetPacket(id));
                }
                // Clicking UNLOCKED or LOCKED does nothing
                return true;
            }

            boolean inside = mx >= px && mx < px + popW && my >= py && my < py + popH;
            boolean onX    = mx >= px + popW - 14 && mx < px + popW && my >= py && my < py + 20;
            if (!inside || onX) { popup = null; }
            return true;
        }

        // ── Tab bar ───────────────────────────────────────────────────────────
        int tabW = 24, tabH = 24, spacing = 2;
        for (int i = 0; i < tabs.size(); i++) {
            int tx = frameX + frameW;
            int ty = frameY + (i * (tabH + spacing));
            if (mx >= tx && mx < tx + tabW && my >= ty && my < ty + tabH) {
                if (i != activeTab) {
                    activeTab = i;
                    scrollX = scrollY = 0;
                    zoom = 1f;
                    popup = null;
                }
                return true;
            }
        }

        // ── Drag start ────────────────────────────────────────────────────────
        if ((btn == GLFW.GLFW_MOUSE_BUTTON_RIGHT || btn == GLFW.GLFW_MOUSE_BUTTON_MIDDLE)
                && isInCanvas((int)mx, (int)my)) {
            dragging = true; dragLastX = mx; dragLastY = my;
            return true;
        }

        // ── Widget click (left) ───────────────────────────────────────────────
        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT && isInCanvas((int)mx, (int)my)) {
            if (!tabs.isEmpty()) {
                for (WidgetDefinition w : tabs.get(activeTab).widgets) {
                    int wx = canvasToScreenX(w.canvasX), wy = canvasToScreenY(w.canvasY);
                    int ww = Math.max(1, (int)(w.iconW * zoom)), wh = Math.max(1, (int)(w.iconH * zoom));
                    int pad = 4;
                    if (mx >= wx - pad && mx < wx + ww + pad && my >= wy - pad && my < wy + wh + pad) {
                        // Open popup regardless of dep state — the popup itself
                        // explains what's missing when deps aren't met.
                        popup = new PopupState(w, (int)mx, (int)my);
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn == GLFW.GLFW_MOUSE_BUTTON_RIGHT || btn == GLFW.GLFW_MOUSE_BUTTON_MIDDLE)
            dragging = false;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragging) {
            scrollX -= (float)(dx / zoom);
            scrollY -= (float)(dy / zoom);
            clampScroll();
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!isInCanvas((int)mx, (int)my) || popup != null) return super.mouseScrolled(mx, my, delta);

        float oldZoom = zoom;
        zoom = Mth.clamp(zoom + (float)(delta * ZOOM_STEP), ZOOM_MIN, ZOOM_MAX);

        float cpx = (float)(mx - canvasX) / oldZoom + scrollX;
        float cpy = (float)(my - canvasY) / oldZoom + scrollY;
        scrollX = cpx - (float)(mx - canvasX) / zoom;
        scrollY = cpy - (float)(my - canvasY) / zoom;
        clampScroll();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (popup != null) { popup = null; return true; }
            onClose(); return true;
        }
        if (popup != null) return super.keyPressed(key, scan, mods);

        float step = 40 / zoom;
        return switch (key) {
            case GLFW.GLFW_KEY_LEFT  -> { scrollX -= step; clampScroll(); yield true; }
            case GLFW.GLFW_KEY_RIGHT -> { scrollX += step; clampScroll(); yield true; }
            case GLFW.GLFW_KEY_UP    -> { scrollY -= step; clampScroll(); yield true; }
            case GLFW.GLFW_KEY_DOWN  -> { scrollY += step; clampScroll(); yield true; }
            case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD ->
            { zoom = Mth.clamp(zoom + ZOOM_STEP * 3, ZOOM_MIN, ZOOM_MAX); yield true; }
            case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT ->
            { zoom = Mth.clamp(zoom - ZOOM_STEP * 3, ZOOM_MIN, ZOOM_MAX); yield true; }
            default -> super.keyPressed(key, scan, mods);
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private int canvasToScreenX(int cx) { return canvasX + (int)((cx - scrollX) * zoom); }
    private int canvasToScreenY(int cy) { return canvasY + (int)((cy - scrollY) * zoom); }

    private boolean isInCanvas(int sx, int sy) {
        return sx >= canvasX && sx < canvasX + canvasW
                && sy >= canvasY && sy < canvasY + canvasH;
    }

    private void clampScroll() {
        if (tabs.isEmpty()) return;
        TabDefinition t = tabs.get(activeTab);
        scrollX = Mth.clamp(scrollX, 0, Math.max(0, t.canvasWidth  - canvasW / zoom));
        scrollY = Mth.clamp(scrollY, 0, Math.max(0, t.canvasHeight - canvasH / zoom));
    }

    private void drawBorder(GuiGraphics gfx, int x, int y, int w, int h, int t, int col) {
        gfx.fill(x,         y,         x + w,     y + t,     col);
        gfx.fill(x,         y + h - t, x + w,     y + h,     col);
        gfx.fill(x,         y,         x + t,     y + h,     col);
        gfx.fill(x + w - t, y,         x + w,     y + h,     col);
    }

    private void drawCornerAccents(GuiGraphics gfx, int x, int y, int w, int h) {
        int s = 6, col = C_ACCENT;
        gfx.fill(x,         y,         x + s, y + 1, col); gfx.fill(x,         y, x + 1, y + s,     col);
        gfx.fill(x + w - s, y,         x + w, y + 1, col); gfx.fill(x + w - 1, y, x + w, y + s,     col);
        gfx.fill(x,         y + h - 1, x + s, y + h, col); gfx.fill(x,         y + h - s, x + 1, y + h, col);
        gfx.fill(x + w - s, y + h - 1, x + w, y + h, col); gfx.fill(x + w - 1, y + h - s, x + w, y + h, col);
    }

    private void drawTooltip(GuiGraphics gfx, String text, int mx, int my) {
        int tw = font.width(text) + 8, th = font.lineHeight + 6;
        int tx = Mth.clamp(mx + 12, frameX, frameX + frameW - tw);
        int ty = Mth.clamp(my - th - 4, frameY + TAB_H, frameY + frameH - th);
        drawBorder(gfx, tx, ty, tw, th, 1, C_ACCENT);
        gfx.drawString(font, text, tx + 4, ty + 3, C_TEXT, false);
    }

    private void enableScissor(int x, int y, int w, int h) {
        double s = minecraft.getWindow().getGuiScale();
        RenderSystem.enableScissor(
                (int)(x * s), (int)((height - y - h) * s),
                (int)(w * s), (int)(h * s));
    }

    private void disableScissor() { RenderSystem.disableScissor(); }

    @Override public boolean isPauseScreen() { return false; }
}