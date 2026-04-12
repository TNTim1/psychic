package net.tntim1.psychic.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
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
import com.mojang.blaze3d.platform.NativeImage;
import org.lwjgl.opengl.GL11;

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
    private static final int C_BAR_TRACK  = 0xFF1a1208;
    private static final int C_BAR_FILL   = 0xFFc4a080;
    private static final int C_BAR_DONE   = 0xFF44aa44;
    private static final int C_BAR_BORDER = 0xFF5a3a28;

    // ── Spiral palettes — one per tab, cycles if more tabs than palettes ──────
    private static final int[][][] TAB_SPIRAL_STOPS = {
            // Tab 0 — void vortex
            {
                    { 0x0D0221, 0x4B0082,   0, 200 },
                    { 0x4B0082, 0x0066CC, 200, 400 },
                    { 0x0066CC, 0x00994D, 400, 550 },
                    { 0x00994D, 0xCC2200, 550, 700 },
                    { 0xCC2200, 0xFFCC44, 700, 850 },
                    { 0xFFCC44, 0xFFFFEE, 850, 1000 },
            },
            // Tab 1 — deep ocean
            {
                    { 0x000810, 0x001830,   0, 250 },
                    { 0x001830, 0x003D6B, 250, 450 },
                    { 0x003D6B, 0x007A8A, 450, 620 },
                    { 0x007A8A, 0x00C8C8, 620, 800 },
                    { 0x00C8C8, 0x88FFFF, 800, 1000 },
            },
            // Tab 2 — inferno
            {
                    { 0x0A0000, 0x3D0000,   0, 150 },
                    { 0x3D0000, 0x8B1A00, 150, 350 },
                    { 0x8B1A00, 0xCC4400, 350, 550 },
                    { 0xCC4400, 0xFF8800, 550, 750 },
                    { 0xFF8800, 0xFFDD44, 750, 900 },
                    { 0xFFDD44, 0xFFFFCC, 900, 1000 },
            },
            // Tab 3 — toxic
            {
                    { 0x000A00, 0x0A2200,   0, 200 },
                    { 0x0A2200, 0x1A5500, 200, 400 },
                    { 0x1A5500, 0x33AA00, 400, 600 },
                    { 0x33AA00, 0x88FF00, 600, 800 },
                    { 0x88FF00, 0xDDFFAA, 800, 1000 },
            },
            // Tab 4 — blood moon
            {
                    { 0x0A0005, 0x2D0010,   0, 200 },
                    { 0x2D0010, 0x6B0020, 200, 400 },
                    { 0x6B0020, 0xBB0033, 400, 600 },
                    { 0xBB0033, 0xFF3366, 600, 800 },
                    { 0xFF3366, 0xFFAACC, 800, 1000 },
            },
            // Tab 5 — arctic
            {
                    { 0x05050F, 0x10104A,   0, 200 },
                    { 0x10104A, 0x3A1A8A, 200, 400 },
                    { 0x3A1A8A, 0x7744CC, 400, 600 },
                    { 0x7744CC, 0xBB88FF, 600, 800 },
                    { 0xBB88FF, 0xEEDDFF, 800, 1000 },
            },
            // Tab 6 — gold rush
            {
                    { 0x0A0700, 0x2A1800,   0, 200 },
                    { 0x2A1800, 0x5C3200, 200, 400 },
                    { 0x5C3200, 0xAA6600, 400, 600 },
                    { 0xAA6600, 0xDDAA00, 600, 800 },
                    { 0xDDAA00, 0xFFEE55, 800, 1000 },
            },
            // Tab 7 — abyssal
            {
                    { 0x000808, 0x051818,   0, 200 },
                    { 0x051818, 0x1A3A3A, 200, 400 },
                    { 0x1A3A3A, 0x446666, 400, 600 },
                    { 0x446666, 0x88AAAA, 600, 800 },
                    { 0x88AAAA, 0xDDEEEE, 800, 1000 },
            },
    };

    private static final float ARM_COUNT   = 3.0f;
    private static final float GROWTH_RATE = 400.0f;

    // ── layout ────────────────────────────────────────────────────────────────
    private static final int TAB_H     = 26;
    private static final int FRAME_PAD = 6;
    private static final int HEADER_H  = 26;

    private static final int POPUP_W          = 210;
    private static final int POPUP_H_BASE     = 160;
    private static final int POPUP_TASK_ROW_H = 22;

    // ── zoom ──────────────────────────────────────────────────────────────────
    private static final float ZOOM_MIN  = 0.001f;
    private static final float ZOOM_MAX  = 20.0f;
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

    public AtlasCodexScreen() {
        super(Component.literal("Atlas Codex"));
    }

    @Override
    protected void init() {
        super.init();
        recalcLayout();
        if (this.openTime == 0) { this.scrollX = 0; this.scrollY = 0; }
        this.openTime = System.currentTimeMillis();
        this.alreadySeenUnlocked.clear();
        for (TabDefinition tab : tabs)
            for (WidgetDefinition w : tab.widgets)
                if (ClientKnowledge.isUnlocked(w.id))
                    alreadySeenUnlocked.add(w.id);
    }

    @Override
    public void onClose() { super.onClose(); }

    private void recalcLayout() {
        int m = 18;
        frameX = m; frameY = m;
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

    private void renderCanvas(GuiGraphics gfx, int mx, int my, float pt) {
        drawSpiralBackground(gfx, pt);
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

    // ── Spiral background ─────────────────────────────────────────────────────

    private void drawSpiralBackground(GuiGraphics gfx, float partialTick) {
        int texW = canvasW;
        int texH = canvasH;
        if (texW < 1 || texH < 1) return;

        NativeImage img = new NativeImage(texW, texH, false);

        int[][] stops = TAB_SPIRAL_STOPS[activeTab % TAB_SPIRAL_STOPS.length];

        float twoPi        = (float)(Math.PI * 2.0);
        float time         = (minecraft.level.getGameTime() + partialTick) * 0.008f;
        float paletteRepeats = 3.0f;

        // Turbulence is in canvas-space, so it must be independent of zoom.
        // We scale turbulence frequency by 1/zoom so it looks the same
        // regardless of how far in or out the player is.
        float turbFreqScale = 1.0f / Math.max(0.01f, zoom);

        for (int py = 0; py < texH; py++) {
            for (int px = 0; px < texW; px++) {

                // ── Canvas-space coordinate ──────────────────────────────
                float screenX = canvasX + (px + 0.5f);
                float screenY = canvasY + (py + 0.5f);
                float cx = (screenX - (canvasX + canvasW / 2f)) / zoom + scrollX;
                float cy = (screenY - (canvasY + canvasH / 2f)) / zoom + scrollY;

                float dist  = Mth.sqrt(cx * cx + cy * cy);
                float angle = (float)Math.atan2(cy, cx) + time;

                // ── Turbulence in canvas-space ───────────────────────────
                // All frequencies multiplied by turbFreqScale so the pattern
                // stays the same physical size in canvas units at any zoom.
                float distNorm = dist / (GROWTH_RATE * 6f);

                float turb = 0f;
                turb += Mth.sin(angle * 2.0f + dist * 0.08f * turbFreqScale + time * 1.3f) * 0.35f;
                turb += Mth.sin(angle * 5.0f - dist * 0.15f * turbFreqScale + time * 0.7f) * 0.18f;
                turb += Mth.sin(angle * 11.0f + dist * 0.31f * turbFreqScale - time * 1.9f) * 0.09f;
                turb += Mth.sin(dist  * 0.22f * turbFreqScale + time * 2.1f)                * 0.12f;
                turb += Mth.sin(angle * 3.0f  + dist * 0.05f * turbFreqScale - time * 0.4f) * 0.22f;

                // Scale turbulence amplitude by distance so center stays tidy
                turb *= Mth.clamp(distNorm, 0f, 1.8f);

                float warpedAngle = angle + turb;

                // ── Arm proximity ────────────────────────────────────────
                float idealTheta = dist / GROWTH_RATE;
                float angleMod   = ((warpedAngle % twoPi) + twoPi) % twoPi;
                float armPhase   = (angleMod * ARM_COUNT) % twoPi;
                float thetaMod   = (idealTheta * ARM_COUNT) % twoPi;

                float delta = thetaMod - armPhase;
                if (delta >  Math.PI) delta -= twoPi;
                if (delta < -Math.PI) delta += twoPi;

                float rawProx = 1.0f - Mth.clamp(Math.abs(delta) / (float)Math.PI, 0f, 1f);

                // ── Arms get fatter with distance ────────────────────────
                // At center: power ~6 (thin sharp arms)
                // At edge:   power ~1.2 (wide, filled arms)
                // distNorm is clamped 0..1 across the "natural" spiral extent
                float armWidthNorm = Mth.clamp(distNorm, 0f, 1f);
                float power = Mth.lerp(armWidthNorm, 5.0f, 1.1f);
                float proximity = (float)Math.pow(rawProx, power);

                // ── Infinite tiling color ────────────────────────────────
                float rawT   = (dist / GROWTH_RATE) / twoPi;
                float tiledT = (rawT * paletteRepeats) % 1.0f;

                int fromColor = stops[0][0];
                int toColor   = stops[0][1];
                float localFrac = 0f;
                for (int[] stop : stops) {
                    float t0 = stop[2] / 1000f;
                    float t1 = stop[3] / 1000f;
                    if (tiledT >= t0 && tiledT <= t1) {
                        fromColor = stop[0];
                        toColor   = stop[1];
                        localFrac = (tiledT - t0) / Math.max(0.0001f, t1 - t0);
                        break;
                    }
                }

                // ── Smooth gradient — no dithering ───────────────────────
                int fr = (int)(((fromColor >> 16) & 0xFF) * (1-localFrac) + ((toColor >> 16) & 0xFF) * localFrac);
                int fg = (int)(((fromColor >>  8) & 0xFF) * (1-localFrac) + ((toColor >>  8) & 0xFF) * localFrac);
                int fb = (int)(( fromColor        & 0xFF) * (1-localFrac) + ( toColor        & 0xFF) * localFrac);

                // Apply arm brightness
                fr = (int)(fr * proximity);
                fg = (int)(fg * proximity);
                fb = (int)(fb * proximity);

                // ── Core glow ────────────────────────────────────────────
                float coreGlow = Mth.clamp(1.0f - dist / (GROWTH_RATE * 2f), 0f, 1f);
                coreGlow = coreGlow * coreGlow;
                int gc = stops[0][0];
                fr = Math.min(255, fr + (int)(((gc >> 16) & 0xFF) * coreGlow * 0.8f));
                fg = Math.min(255, fg + (int)(((gc >>  8) & 0xFF) * coreGlow * 0.8f));
                fb = Math.min(255, fb + (int)(( gc        & 0xFF) * coreGlow * 0.8f));

                // Write ABGR
                img.setPixelRGBA(px, py,
                        (0xFF << 24) | (Mth.clamp(fb,0,255) << 16)
                                | (Mth.clamp(fg,0,255) <<  8)
                                |  Mth.clamp(fr,0,255));
            }
        }

        DynamicTexture tex = new DynamicTexture(img);
        ResourceLocation loc = minecraft.getTextureManager()
                .register("spiral_bg_frame", tex);

        RenderSystem.setShaderTexture(0, loc);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

        gfx.blit(loc, canvasX, canvasY, canvasW, canvasH,
                0, 0, texW, texH, texW, texH);

        tex.close();
    }

    // ── Canvas coordinate helpers ─────────────────────────────────────────────

    private int canvasToScreenX(int cx) {
        return canvasX + (canvasW / 2) + (int)((cx - scrollX) * zoom);
    }

    private int canvasToScreenY(int cy) {
        return canvasY + (canvasH / 2) + (int)((cy - scrollY) * zoom);
    }

    // ── Dependency lines ──────────────────────────────────────────────────────

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

    // ── Widget rendering ──────────────────────────────────────────────────────

    private void renderWidget(GuiGraphics gfx, WidgetDefinition w,
                              int wx, int wy, int ww, int wh, boolean hovered, boolean depsOk) {
        int pad = (int)(4 * zoom);
        int bgX = wx - pad, bgY = wy - pad;
        int bgW = ww + pad * 2, bgH = wh + pad * 2;
        boolean isUnlocked = ClientKnowledge.isUnlocked(w.id);
        ResourceLocation bgTex;
        if (!depsOk)         bgTex = WIDGET_BG_LOCKED;
        else if (isUnlocked) bgTex = hovered ? WIDGET_BG_ACTIVE_SELECTED : WIDGET_BG_ACTIVE;
        else                 bgTex = hovered ? WIDGET_BG_INACTIVE_SELECTED : WIDGET_BG_INACTIVE;
        RenderSystem.enableBlend();
        gfx.blit(bgTex, bgX, bgY, 0, 0, bgW, bgH, bgW, bgH);
        gfx.blit(w.iconTexture, wx, wy, 0, 0, ww, wh, ww, wh);
        RenderSystem.disableBlend();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POPUP
    // ─────────────────────────────────────────────────────────────────────────

    private int popupHeight(WidgetDefinition w, boolean depsOk) {
        return canvasH;
    }

    private void renderPopup(GuiGraphics gfx, int mx, int my) {
        if (popup == null) return;
        WidgetDefinition w = popup.widget;

        boolean isUnlocked = ClientKnowledge.isUnlocked(w.id);
        boolean depsOk     = ClientKnowledge.areDependenciesMet(w.id);
        List<String> missing = ClientKnowledge.getMissingDependencyLabels(w.id);

        int popW = POPUP_W;
        int popH = canvasH;
        int px   = canvasX + canvasW - popW;
        int py   = canvasY;

        gfx.fill(px + 5, py + 5, px + popW + 5, py + popH + 5, 0x22000000);
        gfx.fill(px, py, px + popW, py + popH, C_POPUP_BG);
        drawBorder(gfx, px, py, popW, popH, 2, C_POPUP_BORDER);
        drawCornerAccents(gfx, px, py, popW, popH);

        gfx.fill(px, py, px + popW, py + 20, C_BORDER);
        gfx.fill(px, py + 20, px + popW, py + 21, C_ACCENT);

        RenderSystem.enableBlend();
        gfx.blit(w.iconTexture, px + 4, py + 4, 0, 0, 12, 12, 12, 12);
        RenderSystem.disableBlend();
        gfx.drawString(font, w.label, px + 20, py + 6, C_ACCENT, false);

        boolean closeHov = mx >= px + popW - 14 && mx < px + popW && my >= py && my < py + 20;
        gfx.drawString(font, "×", px + popW - 10, py + 6, closeHov ? 0x000000 : C_TEXT, false);

        int taskSectionH = (depsOk && w.hasTasks() && !isUnlocked)
                ? w.taskRequirements.size() * POPUP_TASK_ROW_H + 4 : 0;

        int bx = px + 6;
        int by = py + 24;
        int bw = popW - 12;
        int bh = popH - 24 - taskSectionH - 36;
        int lh = font.lineHeight + 2;

        if (!depsOk) {
            gfx.drawString(font, "§cRequires:", bx, by, C_TEXT, false);
            int cy = by + lh;
            for (String dep : missing) {
                if (cy + lh > by + bh + taskSectionH) break;
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

        if (depsOk && w.hasTasks() && !isUnlocked) {
            int taskY = py + 24 + bh + 4;
            gfx.fill(px + 6, taskY - 2, px + popW - 6, taskY - 1, C_ACCENT);
            List<TaskRequirement> reqs = w.taskRequirements;
            for (int t = 0; t < reqs.size(); t++) {
                TaskRequirement req = reqs.get(t);
                int current = ClientKnowledge.getTaskProgress(w.id, t);
                boolean done = current >= req.required;
                int rowY = taskY + t * POPUP_TASK_ROW_H;
                int barX = bx, barW = bw;
                int barY = rowY + font.lineHeight + 2, barH = 5;
                String countStr   = done ? "✔" : current + " / " + req.required;
                int    countColor = done ? 0xFF44aa44 : 0xFF7a5a3a;
                gfx.drawString(font, req.label, barX, rowY, C_ACCENT, false);
                int cw = font.width(countStr);
                gfx.drawString(font, countStr, barX + barW - cw, rowY, countColor, false);
                gfx.fill(barX, barY, barX + barW, barY + barH, C_BAR_TRACK);
                drawBorder(gfx, barX, barY, barW, barH, 1, C_BAR_BORDER);
                int fillW = done ? barW - 2 : (int)((float)current / req.required * (barW - 2));
                if (fillW > 0)
                    gfx.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + barH - 1, done ? C_BAR_DONE : C_BAR_FILL);
            }
        }

        int btnW = 80, btnH = 16;
        int btnX = px + (popW - btnW) / 2;
        int btnY = py + popH - 20;

        if (isUnlocked) {
            renderBottomButton(gfx, "UNLOCKED", 0xFF44aa44, false, btnX, btnY, btnW, btnH, mx, my);
        } else if (!depsOk) {
            renderBottomButton(gfx, "LOCKED", 0xFF555555, false, btnX, btnY, btnW, btnH, mx, my);
        } else if (w.hasTasks()) {
            boolean allDone = ClientKnowledge.areTasksMet(w.id);
            renderBottomButton(gfx, allDone ? "COMPLETE ✔" : "IN PROGRESS",
                    allDone ? 0xFF44aa44 : 0xFF7a5a3a, false, btnX, btnY, btnW, btnH, mx, my);
        } else {
            boolean hov = mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH;
            renderBottomButton(gfx, "ACTIVATE", hov ? 0xFFc4a080 : C_BORDER, true, btnX, btnY, btnW, btnH, mx, my);
        }
    }

    private void renderBottomButton(GuiGraphics gfx, String text, int bgColor,
                                    boolean interactive, int x, int y, int w, int h, int mx, int my) {
        gfx.fill(x, y, x + w, y + h, bgColor);
        drawBorder(gfx, x, y, w, h, 1, C_ACCENT);
        int tw = font.width(text);
        int textColor = interactive ? C_ACCENT : 0xFFd4c8c0;
        gfx.drawString(font, text, x + (w - tw) / 2, y + (h - font.lineHeight) / 2, textColor, false);
    }

    private void renderBodyInfo(GuiGraphics gfx, String text, int x, int y, int maxW, int maxH, int lh) {
        int cy = y;
        for (String raw : text.split("\n"))
            for (var seq : font.split(Component.literal(raw), maxW)) {
                if (cy + lh > y + maxH) return;
                gfx.drawString(font, seq, x, cy, C_TEXT, false);
                cy += lh;
            }
    }

    private void renderBodyList(GuiGraphics gfx, String data, int x, int y, int maxW, int maxH, int lh) {
        int cy = y;
        for (String entry : data.split("\n")) {
            if (cy + lh > y + maxH) break;
            gfx.fill(x, cy + lh / 2 - 1, x + 3, cy + lh / 2 + 2, C_ACCENT);
            gfx.drawString(font, entry, x + 8, cy, C_TEXT, false);
            cy += lh;
        }
    }

    private void renderBodyImage(GuiGraphics gfx, String texStr, int x, int y, int maxW, int maxH) {
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
        if (popup != null) {
            WidgetDefinition w = popup.widget;
            boolean depsOk     = ClientKnowledge.areDependenciesMet(w.id);
            boolean isUnlocked = ClientKnowledge.isUnlocked(w.id);

            int popW = POPUP_W, popH = canvasH;
            int px = canvasX + canvasW - popW, py = canvasY;
            int btnW = 80, btnH = 16;
            int btnX = px + (popW - btnW) / 2;
            int btnY = py + popH - 20;

            if (mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH) {
                if (!isUnlocked && depsOk && !w.hasTasks())
                    ModPackets.sendToServer(new ActivateWidgetPacket(w.id));
                return true;
            }

            boolean inside = mx >= px && mx < px + popW && my >= py && my < py + popH;
            boolean onX    = mx >= px + popW - 14 && mx < px + popW && my >= py && my < py + 20;
            if (!inside || onX) popup = null;
            return true;
        }

        int tabW = 24, tabH = 24, spacing = 2;
        for (int i = 0; i < tabs.size(); i++) {
            int tx = frameX + frameW, ty = frameY + (i * (tabH + spacing));
            if (mx >= tx && mx < tx + tabW && my >= ty && my < ty + tabH) {
                if (i != activeTab) { activeTab = i; scrollX = scrollY = 0; zoom = 1f; popup = null; }
                return true;
            }
        }

        if ((btn == GLFW.GLFW_MOUSE_BUTTON_RIGHT || btn == GLFW.GLFW_MOUSE_BUTTON_MIDDLE)
                && isInCanvas((int)mx, (int)my)) {
            dragging = true; dragLastX = mx; dragLastY = my;
            return true;
        }

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
            this.scrollX -= (float)(dx / zoom);
            this.scrollY -= (float)(dy / zoom);
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!isInCanvas((int)mx, (int)my) || popup != null) return false;
        float oldZoom    = zoom;
        float zoomFactor = delta > 0 ? 1.1f : 0.9f;
        zoom = Mth.clamp(zoom * zoomFactor, ZOOM_MIN, ZOOM_MAX);
        float dx = (float)(mx - (canvasX + canvasW / 2));
        float dy = (float)(my - (canvasY + canvasH / 2));
        scrollX += dx / oldZoom - dx / zoom;
        scrollY += dy / oldZoom - dy / zoom;
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isInCanvas(int sx, int sy) {
        return sx >= canvasX && sx < canvasX + canvasW
                && sy >= canvasY && sy < canvasY + canvasH;
    }

    private boolean isInsideCanvas(float x, float y) {
        return x >= canvasX && x <= canvasX + canvasW
                && y >= canvasY && y <= canvasY + canvasH;
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
        gfx.fill(x,         y,         x + s, y + 1, col); gfx.fill(x,         y,         x + 1, y + s, col);
        gfx.fill(x + w - s, y,         x + w, y + 1, col); gfx.fill(x + w - 1, y,         x + w, y + s, col);
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