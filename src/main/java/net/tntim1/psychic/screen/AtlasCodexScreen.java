package net.tntim1.psychic.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.tntim1.psychic.network.ActivateWidgetPacket;
import net.tntim1.psychic.network.ModPackets;
import net.tntim1.psychic.player_data.ClientKnowledge;
import net.tntim1.psychic.widget.TabDefinition;
import net.tntim1.psychic.widget.TabRegistry;
import net.tntim1.psychic.widget.TaskRequirement;
import net.tntim1.psychic.widget.WidgetDefinition;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AtlasCodexScreen extends Screen {

    // ── textures ──────────────────────────────────────────────────────────────
    private static final ResourceLocation WIDGET_BG_INACTIVE          = new ResourceLocation("psychic", "textures/gui/widgets/widget_background_inactive.png");
    private static final ResourceLocation WIDGET_BG_INACTIVE_SELECTED = new ResourceLocation("psychic", "textures/gui/widgets/widget_background_inactive_selected.png");
    private static final ResourceLocation WIDGET_BG_ACTIVE            = new ResourceLocation("psychic", "textures/gui/widgets/widget_background_active.png");
    private static final ResourceLocation WIDGET_BG_ACTIVE_SELECTED   = new ResourceLocation("psychic", "textures/gui/widgets/widget_background_active_selected.png");
    private static final ResourceLocation WIDGET_BG_LOCKED            = new ResourceLocation("psychic", "textures/gui/widgets/widget_background_locked.png");

    // ── palette ───────────────────────────────────────────────────────────────
    private static final int C_BORDER       = 0xFFd4a9a2;
    private static final int C_ACCENT       = 0xFF392420;
    private static final int C_TAB_INACTIVE = 0xFF000000;
    private static final int C_TAB_HOVER    = 0xFFd18a7d;
    private static final int C_CANVAS_BG    = 0xFF333333;
    private static final int C_POPUP_BG     = 0xFFd4a9a2;
    private static final int C_POPUP_BORDER = 0xFF392420;
    private static final int C_TEXT         = 0xFF000000;
    private static final int C_TEXT_DIM     = 0xFF000000;

    // Task progress bar colours
    private static final int C_BAR_TRACK    = 0xFF1a1208;   // dark track
    private static final int C_BAR_FILL     = 0xFFc4a080;   // warm amber — in progress
    private static final int C_BAR_DONE     = 0xFF44aa44;   // green — complete
    private static final int C_BAR_BORDER   = 0xFF5a3a28;

    // ── layout ────────────────────────────────────────────────────────────────
    private static final int TAB_H     = 26;
    private static final int FRAME_PAD = 6;
    private static final int HEADER_H  = 26;

    // Popup grows taller when tasks are present
    private static final int POPUP_W          = 210;
    private static final int POPUP_H_BASE     = 160;   // no tasks / locked
    private static final int POPUP_TASK_ROW_H = 22;    // height per task row

    // ── zoom ──────────────────────────────────────────────────────────────────
    private static final float ZOOM_MIN  = 0.02f;
    private static final float ZOOM_MAX  = 5.0f;
    private static final float ZOOM_STEP = 0.12f;

    // ── state ─────────────────────────────────────────────────────────────────
    private final List<TabDefinition> tabs = TabRegistry.getTabs();
    private int   activeTab = 0;
    private float scrollX = 0, scrollY = 0;
    private float zoom    = 1.0f;

    private boolean dragging;
    private double  dragLastX, dragLastY;
    private int     hoveredWidget = -1;
    private PopupState popup = null;

    // ── derived layout ────────────────────────────────────────────────────────
    private int frameX, frameY, frameW, frameH;
    private int canvasX, canvasY, canvasW, canvasH;

    private record PopupState(WidgetDefinition widget, int originX, int originY) {}

    // ── animation state ───────────────────────────────────────────────────────
    private final java.util.Set<String> alreadySeenUnlocked = new java.util.HashSet<>();
    private final Map<String, Long> unlockTicks = new HashMap<>();
    private long openTime;

    // ─────────────────────────────────────────────────────────────────────────

    public AtlasCodexScreen() {
        super(Component.literal("Atlas Codex"));
    }

    @Override
    protected void init() {
        super.init();
        recalcLayout();
        this.openTime = System.currentTimeMillis();
        this.alreadySeenUnlocked.clear();
        for (TabDefinition tab : tabs)
            for (WidgetDefinition w : tab.widgets)
                if (ClientKnowledge.isUnlocked(w.id))
                    alreadySeenUnlocked.add(w.id);
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

        gfx.fill(frameX, frameY, frameX + frameW, frameY + frameH, C_BORDER);
        drawBorder(gfx, frameX, frameY, frameW, frameH, 2, C_ACCENT);

        if (!tabs.isEmpty()) {
            String title = tabs.get(activeTab).name.toUpperCase();
            gfx.drawString(font, title, frameX + 12, frameY + 12, C_ACCENT, false);
            int lineY = frameY + HEADER_H + 2;
            gfx.fill(frameX + 10, lineY, frameX + frameW - 10, lineY + 1, C_TEXT);
        }

        renderTabBar(gfx, mx, my);
        renderCanvas(gfx, mx, my, pt);
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

            RenderSystem.enableBlend();
            gfx.blit(tab.icon, tx + 4, ty + 4, 0, 0, 16, 16, 16, 16);
            RenderSystem.disableBlend();
        }
    }

    // ── Canvas ────────────────────────────────────────────────────────────────

    private void renderCanvas(GuiGraphics gfx, int mx, int my, float pt) {
        gfx.fill(canvasX, canvasY, canvasX + canvasW, canvasY + canvasH, C_CANVAS_BG);
        enableScissor(canvasX, canvasY, canvasW, canvasH);

        if (!tabs.isEmpty()) {
            TabDefinition tab = tabs.get(activeTab);
            Map<String, int[]> centres = buildCentreMap(tab);

            renderDependencyLines(gfx, tab, centres, pt);
            gfx.flush();

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

            if (hoveredWidget >= 0)
                drawTooltip(gfx, tab.widgets.get(hoveredWidget).label, mx, my);
        }

        disableScissor();
    }

    private Map<String, int[]> buildCentreMap(TabDefinition tab) {
        Map<String, int[]> map = new HashMap<>();
        for (WidgetDefinition w : tab.widgets) {
            int ww = Math.max(1, (int)(w.iconW * zoom));
            int wh = Math.max(1, (int)(w.iconH * zoom));
            map.put(w.id, new int[]{
                    canvasToScreenX(w.canvasX) + ww / 2,
                    canvasToScreenY(w.canvasY) + wh / 2
            });
        }
        return map;
    }

    private void renderDependencyLines(GuiGraphics gfx, TabDefinition tab,
                                       Map<String, int[]> centres, float partialTick) {
        float animationDuration = 50f;
        long  gameTime = minecraft.level.getGameTime();

        // Pass 1: grey background lines
        for (WidgetDefinition w : tab.widgets) {
            if (!w.hasDependencies()) continue;
            int[] to = centres.get(w.id);
            if (to == null) continue;
            for (String depId : w.dependencies) {
                int[] from = centres.get(depId);
                if (from == null) continue;
                drawGradientLine(gfx, from[0], from[1], to[0], to[1],
                        0xFF444444, 0xFF444444, 1.0f, 0.0f, 0.3f, 0f, 20, 1.0f, partialTick);
            }
        }

        // Pass 2: animated active lines
        for (WidgetDefinition w : tab.widgets) {
            if (!w.hasDependencies()) continue;
            int[] to = centres.get(w.id);
            if (to == null) continue;
            for (String depId : w.dependencies) {
                int[] from = centres.get(depId);
                if (from == null || !ClientKnowledge.isUnlocked(depId)) continue;

                long startTick = unlockTicks.computeIfAbsent(depId, k ->
                        alreadySeenUnlocked.contains(depId) ? -1L : gameTime);

                float progress = 1.0f;
                if (startTick != -1L) {
                    float elapsed = (gameTime - startTick) + partialTick;
                    progress = Mth.clamp(elapsed / animationDuration, 0f, 1f);
                }
                float eased = 1f - (1f - progress) * (1f - progress);

                float dist = Mth.sqrt(Mth.square(to[0] - from[0]) + Mth.square(to[1] - from[1]));
                int segments = Math.max(12, (int)(dist / 6f));

                drawGradientLine(gfx, from[0], from[1], to[0], to[1],
                        0xAACC5500, 0xAAFFD700, 2.2f, 0.0f, 1.0f, 1.2f, segments, eased, partialTick);
                drawGradientLine(gfx, from[0], from[1], to[0], to[1],
                        0xFFFFFFFF, 0xAAFFD700, 0.6f, 0.0f, 0.8f, 0.5f, segments, eased, partialTick);
            }
        }
    }

    private void drawGradientLine(GuiGraphics gfx, float x1, float y1, float x2, float y2,
                                  int color1, int color2, float thicknessMult, float z,
                                  float alphaMult, float jitter, int segments,
                                  float maxProgress, float partialTick) {
        if (maxProgress <= 0.001f) return;

        Matrix4f matrix   = gfx.pose().last().pose();
        var      consumer = gfx.bufferSource().getBuffer(RenderType.gui());

        float a1 = ((color1 >> 24) & 0xFF) / 255f * alphaMult;
        float r1 = ((color1 >> 16) & 0xFF) / 255f;
        float g1 = ((color1 >>  8) & 0xFF) / 255f;
        float b1 =  (color1        & 0xFF) / 255f;
        float a2 = ((color2 >> 24) & 0xFF) / 255f * alphaMult;
        float r2 = ((color2 >> 16) & 0xFF) / 255f;
        float g2 = ((color2 >>  8) & 0xFF) / 255f;
        float b2 =  (color2        & 0xFF) / 255f;

        float cp1x = x1 + (x2 - x1) * 0.5f, cp1y = y1;
        float cp2x = x1 + (x2 - x1) * 0.5f, cp2y = y2;

        float lastX = x1, lastY = y1;
        float baseThickness = thicknessMult * zoom;
        float flowTime = (minecraft.level.getGameTime() + partialTick) * 0.15f;
        int animSegs = Math.max(1, (int)(segments * maxProgress));

        for (int i = 1; i <= animSegs; i++) {
            float t    = (float) i / segments;
            float invT = 1f - t;
            float nextX = invT*invT*invT*x1 + 3*invT*invT*t*cp1x + 3*invT*t*t*cp2x + t*t*t*x2;
            float nextY = invT*invT*invT*y1 + 3*invT*invT*t*cp1y + 3*invT*t*t*cp2y + t*t*t*y2;

            float dx = nextX - lastX, dy = nextY - lastY;
            float len = Mth.sqrt(dx*dx + dy*dy);
            if (len < 0.01f) continue;
            float nx = -dy / len, ny = dx / len;

            float disp = Mth.sin(flowTime + i * 0.4f) * jitter * zoom;
            nextX += nx * disp; nextY += ny * disp;

            float alpha = Mth.lerp(t, a1, a2);
            if (maxProgress < 0.95f && i >= animSegs - 2)
                alpha *= 1f - (float)(i - (animSegs - 2)) / 3f;

            float cr = Mth.lerp(t, r1, r2);
            float cg = Mth.lerp(t, g1, g2);
            float cb = Mth.lerp(t, b1, b2);

            float ox1 = nx * (baseThickness / 2f), oy1 = ny * (baseThickness / 2f);
            consumer.vertex(matrix, lastX - ox1, lastY - oy1, z).color(cr, cg, cb, alpha).endVertex();
            consumer.vertex(matrix, lastX + ox1, lastY + oy1, z).color(cr, cg, cb, alpha).endVertex();
            consumer.vertex(matrix, nextX + ox1, nextY + oy1, z).color(cr, cg, cb, alpha).endVertex();
            consumer.vertex(matrix, nextX - ox1, nextY - oy1, z).color(cr, cg, cb, alpha).endVertex();

            lastX = nextX; lastY = nextY;
        }
    }

    private void renderWidget(GuiGraphics gfx, WidgetDefinition w,
                              int wx, int wy, int ww, int wh, boolean hovered, boolean depsOk) {
        int pad = (int)(4 * zoom);
        int bgX = wx - pad, bgY = wy - pad;
        int bgW = ww + pad * 2, bgH = wh + pad * 2;

        boolean isUnlocked = ClientKnowledge.isUnlocked(w.id);

        ResourceLocation bgTex;
        if (!depsOk)       bgTex = WIDGET_BG_LOCKED;
        else if (isUnlocked) bgTex = hovered ? WIDGET_BG_ACTIVE_SELECTED : WIDGET_BG_ACTIVE;
        else               bgTex = hovered ? WIDGET_BG_INACTIVE_SELECTED : WIDGET_BG_INACTIVE;

        RenderSystem.enableBlend();
        gfx.blit(bgTex, bgX, bgY, 0, 0, bgW, bgH, bgW, bgH);
        gfx.blit(w.iconTexture, wx, wy, 0, 0, ww, wh, ww, wh);
        RenderSystem.disableBlend();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POPUP — the main change
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Calculates how tall the popup needs to be for this widget.
     * Grows by POPUP_TASK_ROW_H for each task row when the widget has tasks
     * and the widget dependencies are met (so we actually show the task section).
     */
    private int popupHeight(WidgetDefinition w, boolean depsOk) {
        if (depsOk && w.hasTasks() && !ClientKnowledge.isUnlocked(w.id)) {
            // title bar (20) + separator (1) + body area + task rows + bottom padding
            return POPUP_H_BASE + w.taskRequirements.size() * POPUP_TASK_ROW_H;
        }
        return POPUP_H_BASE;
    }

    private void renderPopup(GuiGraphics gfx, int mx, int my) {
        if (popup == null) return;
        WidgetDefinition w = popup.widget;

        boolean isUnlocked = ClientKnowledge.isUnlocked(w.id);
        boolean depsOk     = ClientKnowledge.areDependenciesMet(w.id);
        List<String> missing = ClientKnowledge.getMissingDependencyLabels(w.id);

        int popW = POPUP_W;
        int popH = popupHeight(w, depsOk);

        int px = Mth.clamp(popup.originX + 18, frameX + 4, frameX + frameW - popW - 4);
        int py = Mth.clamp(popup.originY - popH / 2, frameY + TAB_H + 4, frameY + frameH - popH - 4);

        // Shadow + background
        gfx.fill(px + 5, py + 5, px + popW + 5, py + popH + 5, 0x22000000);
        gfx.fill(px, py, px + popW, py + popH, C_POPUP_BG);
        drawBorder(gfx, px, py, popW, popH, 2, C_POPUP_BORDER);
        drawCornerAccents(gfx, px, py, popW, popH);

        // Title bar
        gfx.fill(px, py, px + popW, py + 20, C_BORDER);
        gfx.fill(px, py + 20, px + popW, py + 21, C_ACCENT);

        RenderSystem.enableBlend();
        gfx.blit(w.iconTexture, px + 4, py + 4, 0, 0, 12, 12, 12, 12);
        RenderSystem.disableBlend();
        gfx.drawString(font, w.label, px + 20, py + 6, C_ACCENT, false);

        // Close ×
        boolean closeHov = mx >= px + popW - 14 && mx < px + popW && my >= py && my < py + 20;
        gfx.drawString(font, "×", px + popW - 10, py + 6, closeHov ? 0x000000 : C_TEXT, false);

        // ── Body area ─────────────────────────────────────────────────────────
        // When tasks are present the body is shorter to leave room for the task
        // section below it.  When there are no tasks, the body fills as before.
        int taskSectionH = (depsOk && w.hasTasks() && !isUnlocked)
                ? w.taskRequirements.size() * POPUP_TASK_ROW_H + 4
                : 0;

        int bx  = px + 6;
        int by  = py + 24;
        int bw  = popW - 12;
        // Reserve space at the bottom: button + task section + a little padding
        int bh  = popH - 24 - 26 - taskSectionH - 6;
        int lh  = font.lineHeight + 2;

        if (!depsOk) {
            // Widget deps not met — show which widgets are still needed
            gfx.drawString(font, "§cRequires:", bx, by, C_TEXT, false);
            int cy = by + lh;
            for (String dep : missing) {
                if (cy + lh > by + bh + taskSectionH) break;   // use full height
                gfx.fill(bx, cy + lh / 2 - 1, bx + 3, cy + lh / 2 + 2, 0xFFCC4444);
                gfx.drawString(font, dep, bx + 8, cy, 0xFFCC4444, false);
                cy += lh;
            }
        } else {
            // Normal body content (spell description / list / image)
            switch (w.popupType) {
                case INFO   -> renderBodyInfo(gfx, w.popupData, bx, by, bw, bh, lh);
                case LIST   -> renderBodyList(gfx, w.popupData, bx, by, bw, bh, lh);
                case IMAGE  -> renderBodyImage(gfx, w.popupData, bx, by, bw, bh);
                case CUSTOM -> gfx.drawString(font, "[custom:" + w.popupData + "]", bx, by, C_TEXT_DIM, false);
            }
        }

        // ── Task progress section ─────────────────────────────────────────────
        // Shown only when: deps met AND widget has tasks AND not yet unlocked.
        if (depsOk && w.hasTasks() && !isUnlocked) {
            int taskY = py + 24 + bh + 4;   // sits directly below the body text

            // Thin separator above the task block
            gfx.fill(px + 6, taskY - 2, px + popW - 6, taskY - 1, C_ACCENT);

            List<TaskRequirement> reqs = w.taskRequirements;
            for (int t = 0; t < reqs.size(); t++) {
                TaskRequirement req = reqs.get(t);
                int current = ClientKnowledge.getTaskProgress(w.id, t);
                boolean done = current >= req.required;

                int rowY  = taskY + t * POPUP_TASK_ROW_H;
                int barX  = bx;
                int barW  = bw;
                int barY  = rowY + font.lineHeight + 2;
                int barH  = 5;

                // Label row: "Blazes slain" on the left, "3 / 5" or "✔" on the right
                String countStr   = done ? "✔" : current + " / " + req.required;
                int    countColor = done ? 0xFF44aa44 : 0xFF7a5a3a;
                gfx.drawString(font, req.label, barX, rowY, C_ACCENT, false);
                int cw = font.width(countStr);
                gfx.drawString(font, countStr, barX + barW - cw, rowY, countColor, false);

                // Progress bar
                gfx.fill(barX,     barY, barX + barW,     barY + barH, C_BAR_TRACK);
                drawBorder(gfx, barX, barY, barW, barH, 1, C_BAR_BORDER);
                int fillW = done ? barW - 2
                        : (int)((float)current / req.required * (barW - 2));
                if (fillW > 0) {
                    int fillColor = done ? C_BAR_DONE : C_BAR_FILL;
                    gfx.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + barH - 1, fillColor);
                }
            }
        }

        // ── Bottom button ─────────────────────────────────────────────────────
        int btnW = 80, btnH = 16;
        int btnX = px + (popW - btnW) / 2;
        int btnY = py + popH - 20;

        if (isUnlocked) {
            // Already done — green UNLOCKED label, not clickable
            renderBottomButton(gfx, "UNLOCKED", 0xFF44aa44, false, btnX, btnY, btnW, btnH, mx, my);

        } else if (!depsOk) {
            // Widget deps missing — grey LOCKED label, not clickable
            renderBottomButton(gfx, "LOCKED", 0xFF555555, false, btnX, btnY, btnW, btnH, mx, my);

        } else if (w.hasTasks()) {
            // Task-gated — button shows overall progress, not clickable manually.
            // The widget auto-activates on the server when all tasks are done.
            boolean allDone = ClientKnowledge.areTasksMet(w.id);
            String label  = allDone ? "COMPLETE ✔" : "IN PROGRESS";
            int    colour = allDone ? 0xFF44aa44    : 0xFF7a5a3a;
            renderBottomButton(gfx, label, colour, false, btnX, btnY, btnW, btnH, mx, my);

        } else {
            // Normal widget — clickable ACTIVATE button
            boolean hov = mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH;
            renderBottomButton(gfx, "ACTIVATE", hov ? 0xFFc4a080 : C_BORDER, true, btnX, btnY, btnW, btnH, mx, my);
        }
    }

    /** Draws the standard bottom pill-button used across all popup states. */
    private void renderBottomButton(GuiGraphics gfx, String text, int bgColor,
                                    boolean interactive,
                                    int x, int y, int w, int h, int mx, int my) {
        gfx.fill(x, y, x + w, y + h, bgColor);
        drawBorder(gfx, x, y, w, h, 1, C_ACCENT);
        int tw = font.width(text);
        int textColor = interactive ? C_ACCENT : 0xFFd4c8c0;
        gfx.drawString(font, text, x + (w - tw) / 2, y + (h - font.lineHeight) / 2, textColor, false);
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

        // ── Popup open ────────────────────────────────────────────────────────
        if (popup != null) {
            WidgetDefinition w = popup.widget;
            boolean depsOk     = ClientKnowledge.areDependenciesMet(w.id);
            boolean isUnlocked = ClientKnowledge.isUnlocked(w.id);

            int popH = popupHeight(w, depsOk);
            int px = Mth.clamp(popup.originX + 18, frameX + 4, frameX + frameW - POPUP_W - 4);
            int py = Mth.clamp(popup.originY - popH / 2, frameY + TAB_H + 4, frameY + frameH - popH - 4);

            int btnW = 80, btnH = 16;
            int btnX = px + (POPUP_W - btnW) / 2;
            int btnY = py + popH - 20;

            // Only fire ACTIVATE for normal (non-task) widgets
            if (mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH) {
                if (!isUnlocked && depsOk && !w.hasTasks()) {
                    ModPackets.sendToServer(new ActivateWidgetPacket(w.id));
                }
                return true;
            }

            boolean inside = mx >= px && mx < px + POPUP_W && my >= py && my < py + popH;
            boolean onX    = mx >= px + POPUP_W - 14 && mx < px + POPUP_W && my >= py && my < py + 20;
            if (!inside || onX) popup = null;
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
                    int wx  = canvasToScreenX(w.canvasX), wy = canvasToScreenY(w.canvasY);
                    int ww  = Math.max(1, (int)(w.iconW * zoom));
                    int wh  = Math.max(1, (int)(w.iconH * zoom));
                    int pad = 4;
                    if (mx >= wx - pad && mx < wx + ww + pad && my >= wy - pad && my < wy + wh + pad) {
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
        RenderSystem.enableScissor((int)(x*s), (int)((height-y-h)*s), (int)(w*s), (int)(h*s));
    }

    private void disableScissor() { RenderSystem.disableScissor(); }

    @Override public boolean isPauseScreen() { return false; }
}