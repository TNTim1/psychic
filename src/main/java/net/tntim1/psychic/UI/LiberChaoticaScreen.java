package net.tntim1.psychic.UI;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.tntim1.psychic.network.ActivateWidgetPacket;
import net.tntim1.psychic.network.DeactivateWidgetPacket;   // ← you must add this packet
import net.tntim1.psychic.network.ModPackets;
import net.tntim1.psychic.player_data.ClientKnowledge;
import net.tntim1.psychic.widget.*;
import net.tntim1.psychic.widget.pml.PmlRenderer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.NativeImage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LiberChaoticaScreen extends Screen {

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
    private double  dragLastX, dragLastY;

    // Task progress bar colours
    private static final int C_BAR_TRACK  = 0xFF1a1208;
    private static final int C_BAR_FILL   = 0xFFc4a080;
    private static final int C_BAR_DONE   = 0xFF44aa44;
    private static final int C_BAR_BORDER = 0xFF5a3a28;

    // ── Spell widget active tint (drawn over the pattern when the spell is active) ──
    private static final int C_SPELL_ACTIVE   = 0xFF392420;
    private static final int C_SPELL_INACTIVE = 0xFFCCCCCC;  // neutral grey

    // ── Spiral palettes ───────────────────────────────────────────────────────
    private static final int[][][] TAB_SPIRAL_STOPS = {
            { { 0xAAAA66, 0x4B0082,   0, 200 }, { 0x4B0082, 0x0066CC, 200, 400 },
                    { 0x0066CC, 0x00994D, 400, 550 }, { 0x00994D, 0xCC2200, 550, 700 },
                    { 0xCC2200, 0xFFCC44, 700, 850 }, { 0xFFCC44, 0xAAAA66, 850, 1000 } },

            { { 0x000810, 0x001830,   0, 250 }, { 0x001830, 0x003D6B, 250, 450 },
                    { 0x003D6B, 0x007A8A, 450, 620 }, { 0x007A8A, 0x00C8C8, 620, 800 },
                    { 0x00C8C8, 0x000810, 800, 1000 } },

            { { 0xAAAA66, 0x4B0082,   0, 200 }, { 0x4B0082, 0x0066CC, 200, 400 },
                    { 0x0066CC, 0x00994D, 400, 550 }, { 0x00994D, 0xCC2200, 550, 700 },
                    { 0xCC2200, 0xFFCC44, 700, 850 }, { 0xFFCC44, 0xAAAA66, 850, 1000 } },
    };

    private static final float ARM_COUNT   = 8.0f;
    private static final float GROWTH_RATE = 100.0f;

    // ── layout ────────────────────────────────────────────────────────────────
    private static final int TAB_H     = 26;
    private static final int FRAME_PAD = 6;
    private static final int HEADER_H  = 26;

    private static final int POPUP_W          = 220;
    private static final int POPUP_TASK_ROW_H = 22;

    private static final int RENDER_SZ = 32;

    // ── zoom ──────────────────────────────────────────────────────────────────
    private static final float ZOOM_MIN  = 0.001f;
    private static final float ZOOM_MAX  = 20.0f;
    private static final float ZOOM_STEP = 0.12f;

    // ── Spell orbit layout ────────────────────────────────────────────────────
    // Increased from 80→160 (radius step) and 0.5→0.9 (angle step) so spells
    // are spaced further apart and further from the centre.
    private static final float SPELL_ORBIT_RADIUS_START = 60f;
    private static final float SPELL_ORBIT_RADIUS_STEP  = 20f;
    private static final float SPELL_ANGLE_STEP         = 0.9f;

    // ── state ─────────────────────────────────────────────────────────────────
    private final List<TabDefinition> tabs = TabRegistry.getTabs();
    private int   activeTab = 0;
    private float scrollX = 0, scrollY = 0;
    private float zoom    = 1.0f;

    private boolean dragging;
    private int     hoveredWidget = -1;

    // ── popup state ───────────────────────────────────────────────────────────
    private PopupState popup = null;

    private static final class PopupState {
        final WidgetDefinition widget;
        int page = 0;
        boolean confirmed = false;
        int hoveredRenderIdx = -1;

        PopupState(WidgetDefinition widget) { this.widget = widget; }
    }

    // ── derived layout ────────────────────────────────────────────────────────
    private int frameX, frameY, frameW, frameH;
    private int canvasX, canvasY, canvasW, canvasH;

    // ── animation state ───────────────────────────────────────────────────────
    private final java.util.Set<String>  alreadySeenUnlocked = new java.util.HashSet<>();
    private final Map<String, Long>      unlockTicks         = new HashMap<>();
    private long openTime;

    public LiberChaoticaScreen() {
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
            gfx.fill(frameX + 5, lineY, frameX + frameW - 5, lineY + 1, C_TEXT);
        }
        renderTabBar(gfx, mx, my);
        renderCanvas(gfx, mx, my, pt);
        if (popup != null) renderPopup(gfx, mx, my, pt);
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
        drawSpiralBackground(gfx, pt);
        enableScissor(canvasX, canvasY, canvasW, canvasH);

        if (!tabs.isEmpty()) {
            TabDefinition tab = tabs.get(activeTab);
            Map<String, int[]> centres = buildCentreMap(tab);

            for (DecoraterDefinition d : tab.decorators) {
                float cx = canvasX + (canvasW / 2f) + (d.canvasX - scrollX) * zoom;
                float cy = canvasY + (canvasH / 2f) + (d.canvasY - scrollY) * zoom;
                float iw = Math.max(0.5f, d.iconW * zoom);
                float ih = Math.max(0.5f, d.iconH * zoom);
                float pad = Math.max(1.0f, 4.0f * zoom);
                int bw = Math.round(iw + pad * 2), bh = Math.round(ih + pad * 2);
                int bx = Math.round(cx - bw / 2f), by = Math.round(cy - bh / 2f);
                renderDecorator(gfx, d, bx, by, bw, bh);
            }

            renderDependencyLines(gfx, tab, centres, pt);
            gfx.flush();

            if (tab.name.equals("Psychic")) {
                List<String> history = ClientKnowledge.getSpellOrder();

                // We iterate by *slot index* so that a missing widget does not
                // collapse the spiral — the next valid widget occupies the slot
                // where the missing one would have been.
                int slotIndex = 0;
                for (String spellId : history) {
                    // Always compute position from slotIndex before deciding to draw.
                    double angle         = slotIndex * SPELL_ANGLE_STEP;
                    float  radius        = SPELL_ORBIT_RADIUS_START + (slotIndex * SPELL_ORBIT_RADIUS_STEP);
                    float  canvasXPos    = (float)(Math.cos(angle) * radius);
                    float  canvasYPos    = (float)(Math.sin(angle) * radius);


                    SpellWidgetDefinition s = TabRegistry.findSpellById(spellId);
                    if (s == null) continue;   // no widget registered — position was still consumed above

                    float cx = canvasX + (canvasW / 2f) + (canvasXPos - scrollX) * zoom;
                    float cy = canvasY + (canvasH / 2f) + (canvasYPos - scrollY) * zoom;

                    int drawSize = Math.round(s.size * zoom);
                    int xPos = Math.round(cx - drawSize / 2f);
                    int yPos = Math.round(cy - drawSize / 2f);

                    boolean isActive = ClientKnowledge.isUnlocked(spellId);

                    // Tinted backing square so the activation state is obvious even
                    // before entering the pattern rendering.
                    int backingAlpha = isActive ? 0x55 : 0x22;
                    int backingColor = isActive
                            ? (backingAlpha << 24 | 0x44FF88)   // green tint
                            : (backingAlpha << 24 | 0x888888);  // grey tint
                    gfx.fill(xPos - 2, yPos - 2, xPos + drawSize + 2, yPos + drawSize + 2, backingColor);

                    // Render the spell pattern, coloured by active state
                    int patternColor = isActive ? C_SPELL_ACTIVE : C_SPELL_INACTIVE;
                    SpellPatternRenderer.render(gfx, xPos, yPos, drawSize, s.pattern, s.label, s.iconTexture, font, patternColor);

                    slotIndex++;
                }
            }

            hoveredWidget = -1;
            for (int i = 0; i < tab.widgets.size(); i++) {
                WidgetDefinition w = tab.widgets.get(i);
                float cx = canvasX + (canvasW / 2f) + (w.canvasX - scrollX) * zoom;
                float cy = canvasY + (canvasH / 2f) + (w.canvasY - scrollY) * zoom;
                float iw = Math.max(0.5f, w.iconW * zoom);
                float ih = Math.max(0.5f, w.iconH * zoom);
                float pad = Math.max(1.0f, 4.0f * zoom);
                int bgW = Math.round(iw + pad * 2), bgH = Math.round(ih + pad * 2);
                int bgX = Math.round(cx - bgW / 2f), bgY = Math.round(cy - bgH / 2f);
                int drawX = Math.round(cx - iw / 2f), drawY = Math.round(cy - ih / 2f);
                int drawW = Math.round(iw), drawH = Math.round(ih);

                boolean depsOk = ClientKnowledge.areDependenciesMet(w.id);
                boolean hov    = popup == null && depsOk
                        && mx >= bgX && mx < bgX + bgW
                        && my >= bgY && my < bgY + bgH;
                if (hov) hoveredWidget = i;

                renderWidget(gfx, w, bgX, bgY, bgW, bgH, drawX, drawY, drawW, drawH, hov, depsOk);
            }
            if (hoveredWidget >= 0)
                drawTooltip(gfx, tab.widgets.get(hoveredWidget).label, mx, my);
        }
        disableScissor();
    }

    // ── Spiral background (unchanged) ─────────────────────────────────────────

    private void drawSpiralBackground(GuiGraphics gfx, float partialTick) {
        int texW = canvasW, texH = canvasH;
        if (texW < 1 || texH < 1) return;

        NativeImage img = new NativeImage(texW, texH, false);
        int[][] stops = TAB_SPIRAL_STOPS[activeTab % TAB_SPIRAL_STOPS.length];

        float twoPi = (float)(Math.PI * 2.0);
        float gameTime = (minecraft.level.getGameTime() + partialTick);
        float time = gameTime * 0.008f;
        float paletteRepeats = 1.0f;
        float maxPaletteValue = 0.0f;
        for (int[] s : stops) if (s[3] > maxPaletteValue) maxPaletteValue = s[3];
        maxPaletteValue = Math.max(1.0f, maxPaletteValue);

        float turbFreqScale = 1.0f / Math.max(0.01f, zoom);
        float noiseWeight   = Mth.clamp(zoom, 0.2f, 1.0f);

        for (int py = 0; py < texH; py++) {
            for (int px = 0; px < texW; px++) {
                float screenX = canvasX + (px + 0.5f);
                float screenY = canvasY + (py + 0.5f);
                float cx = (screenX - (canvasX + canvasW / 2f)) / zoom + scrollX;
                float cy = (screenY - (canvasY + canvasH / 2f)) / zoom + scrollY;

                float dist  = Mth.sqrt(cx * cx + cy * cy);
                float angle = (float)Math.atan2(cy, cx) + time;
                float distNorm = dist / (GROWTH_RATE * 6f);
                float turb = 0f;
                turb += Mth.sin(angle * 2.0f + dist * 0.08f * turbFreqScale + time * 1.3f) * 0.35f * noiseWeight;
                turb += Mth.sin(angle * 5.0f - dist * 0.15f * turbFreqScale + time * 0.7f) * 0.18f * noiseWeight;
                float warpedAngle = angle + (turb * Mth.clamp(distNorm, 0f, 1.8f));

                float idealTheta = dist / GROWTH_RATE;
                float angleMod   = ((warpedAngle % twoPi) + twoPi) % twoPi;
                float armPhase   = (angleMod * ARM_COUNT) % twoPi;
                float thetaMod   = (idealTheta * ARM_COUNT) % twoPi;
                float delta = thetaMod - armPhase;
                if (delta >  Math.PI) delta -= twoPi;
                if (delta < -Math.PI) delta += twoPi;
                float rawProx  = 1.0f - (float)Mth.smoothstep(Math.abs(delta) / (float)Math.PI);
                float power    = Mth.lerp(Mth.clamp(distNorm, 0f, 1f), 5.0f, 1.1f);
                float proximity = (float)Math.pow((double)rawProx, (double)power);

                float rawT = (dist / GROWTH_RATE) / twoPi;
                float normalizedT = (rawT * paletteRepeats * maxPaletteValue) % maxPaletteValue;

                int fromColor = 0, toColor = 0;
                float localFrac = 0f;
                boolean found = false;
                for (int[] s : stops) {
                    float t0 = s[2], t1 = s[3];
                    if (normalizedT >= t0 && normalizedT <= t1) {
                        fromColor = s[0]; toColor = s[1];
                        localFrac = (normalizedT - t0) / Math.max(0.0001f, t1 - t0);
                        found = true; break;
                    }
                }
                if (!found) {
                    int li = stops.length - 1;
                    fromColor = stops[li][1]; toColor = stops[0][0];
                    float gapStart = stops[li][3], gapEnd = stops[0][2];
                    localFrac = normalizedT < gapEnd
                            ? (normalizedT + (maxPaletteValue - gapStart)) / ((maxPaletteValue - gapStart) + gapEnd)
                            : (normalizedT - gapStart) / ((maxPaletteValue - gapStart) + gapEnd);
                }

                int fr = (int)(((fromColor>>16)&0xFF)*(1-localFrac)+((toColor>>16)&0xFF)*localFrac);
                int fg = (int)(((fromColor>>8)&0xFF)*(1-localFrac)+((toColor>>8)&0xFF)*localFrac);
                int fb = (int)((fromColor&0xFF)*(1-localFrac)+(toColor&0xFF)*localFrac);
                fr = (int)(fr * proximity); fg = (int)(fg * proximity); fb = (int)(fb * proximity);

                float coreGlow = Mth.clamp(1.0f - dist/(GROWTH_RATE*2f), 0f, 1f);
                coreGlow *= coreGlow;
                int gc = stops[0][0];
                fr = Math.min(255, fr+(int)(((gc>>16)&0xFF)*coreGlow*0.8f));
                fg = Math.min(255, fg+(int)(((gc>>8)&0xFF)*coreGlow*0.8f));
                fb = Math.min(255, fb+(int)((gc&0xFF)*coreGlow*0.8f));

                float nx2 = cx*0.015f, ny2 = cy*0.015f, nt = gameTime*0.01f;
                float noise = Mth.sin(nx2+Mth.cos(ny2+nt))*0.5f
                        + Mth.sin(nx2*2.3f-nt*1.2f)*Mth.cos(ny2*1.8f+nt)*0.25f
                        + Mth.sin(nx2*4.5f+ny2*3.2f+nt*2.0f)*0.125f;
                float mask = (noise+1.0f)*0.5f;
                float lightStrength = (float)Math.pow(Math.max(0,(mask-0.25f)/0.75f), 5.0);
                float currentMute = 1.0f - lightStrength;
                fr = (int)Mth.lerp(currentMute, fr, (int)((fr*0.299f)*0.15f));
                fg = (int)Mth.lerp(currentMute, fg, (int)((fg*0.587f)*0.15f));
                fb = (int)Mth.lerp(currentMute, fb, (int)((fb*0.114f)*0.15f));

                img.setPixelRGBA(px, py, (0xFF<<24)|(Mth.clamp(fb,0,255)<<16)|(Mth.clamp(fg,0,255)<<8)|Mth.clamp(fr,0,255));
            }
        }

        DynamicTexture tex = new DynamicTexture(img);
        ResourceLocation loc = minecraft.getTextureManager().register("spiral_bg_frame", tex);
        RenderSystem.setShaderTexture(0, loc);
        RenderSystem.texParameter(3553, 10241, 9729);
        RenderSystem.texParameter(3553, 10240, 9729);
        gfx.blit(loc, canvasX, canvasY, canvasW, canvasH, 0, 0, texW, texH, texW, texH);
        tex.close(); img.close();
    }

    // ── Canvas coordinate helpers ─────────────────────────────────────────────

    private int canvasToScreenX(int cx) { return canvasX + (canvasW/2) + (int)((cx-scrollX)*zoom); }
    private int canvasToScreenY(int cy) { return canvasY + (canvasH/2) + (int)((cy-scrollY)*zoom); }

    // ── Dependency lines ──────────────────────────────────────────────────────

    private Map<String, int[]> buildCentreMap(TabDefinition tab) {
        Map<String, int[]> map = new HashMap<>();
        for (WidgetDefinition w : tab.widgets)
            map.put(w.id, new int[]{ canvasToScreenX(w.canvasX), canvasToScreenY(w.canvasY) });
        return map;
    }

    private void renderDependencyLines(GuiGraphics gfx, TabDefinition tab,
                                       Map<String, int[]> centres, float partialTick) {
        float animationDuration = 50f;
        long  gameTime = minecraft.level.getGameTime();

        for (WidgetDefinition w : tab.widgets) {
            if (!w.hasDependencies()) continue;
            int[] to = centres.get(w.id); if (to == null) continue;
            for (String depId : w.dependencies) {
                int[] from = centres.get(depId); if (from == null) continue;
                drawGradientLine(gfx, from[0], from[1], to[0], to[1],
                        0xFF444444, 0xFF444444, 1.0f, 0.0f, 0.3f, 0f, 20, 1.0f, partialTick);
            }
        }

        for (WidgetDefinition w : tab.widgets) {
            if (!w.hasDependencies()) continue;
            int[] to = centres.get(w.id); if (to == null) continue;
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
                float dist = Mth.sqrt(Mth.square(to[0]-from[0]) + Mth.square(to[1]-from[1]));
                int segments = Math.max(12, (int)(dist/6f));
                drawGradientLine(gfx, from[0], from[1], to[0], to[1],
                        0xAACC5500, 0xAAFFD700, 2.2f, 0.0f, 1.0f, 1.7f, segments, eased, partialTick);
                drawGradientLine(gfx, from[0], from[1], to[0], to[1],
                        0xFFFFFFFF, 0xAAFFD700, 0.6f, 0.0f, 0.8f, 0.7f, segments, eased, partialTick);
            }
        }
    }

    private void drawGradientLine(GuiGraphics gfx, float x1, float y1, float x2, float y2,
                                  int color1, int color2, float thicknessMult, float z,
                                  float alphaMult, float jitter, int segments,
                                  float maxProgress, float partialTick) {
        if (maxProgress <= 0.001f) return;

        Matrix4f matrix = gfx.pose().last().pose();
        var consumer = gfx.bufferSource().getBuffer(RenderType.gui());

        float a1 = ((color1 >> 24) & 0xFF) / 255f * alphaMult, r1 = ((color1 >> 16) & 0xFF) / 255f, g1 = ((color1 >> 8) & 0xFF) / 255f, b1 = (color1 & 0xFF) / 255f;
        float a2 = ((color2 >> 24) & 0xFF) / 255f * alphaMult, r2 = ((color2 >> 16) & 0xFF) / 255f, g2 = ((color2 >> 8) & 0xFF) / 255f, b2 = (color2 & 0xFF) / 255f;

        float cp1x = x1 + (x2 - x1) * 0.5f, cp1y = y1;
        float cp2x = x1 + (x2 - x1) * 0.5f, cp2y = y2;

        float baseThickness = thicknessMult * zoom;
        float flowTime = (minecraft.level.getGameTime() + partialTick) * 0.15f;
        int drawCount = Math.max(1, (int) (segments * maxProgress));

        float lastCleanX = x1, lastCleanY = y1;
        float lastJitterX = x1, lastJitterY = y1;
        float frequency = 8.0f;

        for (int i = 1; i <= drawCount; i++) {
            float t = (float) i / segments;
            float invT = 1f - t;

            float nextCleanX = invT*invT*invT*x1 + 3*invT*invT*t*cp1x + 3*invT*t*t*cp2x + t*t*t*x2;
            float nextCleanY = invT*invT*invT*y1 + 3*invT*invT*t*cp1y + 3*invT*t*t*cp2y + t*t*t*y2;

            float dx = nextCleanX - lastCleanX;
            float dy = nextCleanY - lastCleanY;
            float len = Mth.sqrt(dx * dx + dy * dy);
            if (len < 0.001f) continue;

            float nx = -dy / len;
            float ny = dx / len;

            float waveInput = (flowTime * 0.5f) + (t * frequency);
            float triangle = Math.abs((waveInput % 1.0f) - 0.5f) * 4.0f - 1.0f;
            float noise = (float)(Math.sin(t * 100f + flowTime * 2f) * 0.2f);
            float disp = (triangle + noise) * jitter * zoom;

            float nextJitterX = nextCleanX + (nx * disp);
            float nextJitterY = nextCleanY + (ny * disp);

            float alpha = Mth.lerp(t, a1, a2);
            if (maxProgress < 0.99f && i >= drawCount - 2) alpha *= (1f - (float)(i - (drawCount - 2)) / 3f);

            float cr = Mth.lerp(t, r1, r2), cg = Mth.lerp(t, g1, g2), cb = Mth.lerp(t, b1, b2);
            float halfWidth = baseThickness / 2f;

            consumer.vertex(matrix, lastJitterX - nx*halfWidth, lastJitterY - ny*halfWidth, z).color(cr, cg, cb, alpha).endVertex();
            consumer.vertex(matrix, lastJitterX + nx*halfWidth, lastJitterY + ny*halfWidth, z).color(cr, cg, cb, alpha).endVertex();
            consumer.vertex(matrix, nextJitterX + nx*halfWidth, nextJitterY + ny*halfWidth, z).color(cr, cg, cb, alpha).endVertex();
            consumer.vertex(matrix, nextJitterX - nx*halfWidth, nextJitterY - ny*halfWidth, z).color(cr, cg, cb, alpha).endVertex();

            lastCleanX = nextCleanX; lastCleanY = nextCleanY;
            lastJitterX = nextJitterX; lastJitterY = nextJitterY;
        }
    }

    // ── Widget rendering ──────────────────────────────────────────────────────

    private void renderWidget(GuiGraphics gfx, WidgetDefinition w,
                              int bgX, int bgY, int bgW, int bgH,
                              int ix, int iy, int iw, int ih,
                              boolean hovered, boolean depsOk) {
        boolean isUnlocked = ClientKnowledge.isUnlocked(w.id);
        ResourceLocation bgTex;
        if (!depsOk)         bgTex = WIDGET_BG_LOCKED;
        else if (isUnlocked) bgTex = hovered ? WIDGET_BG_ACTIVE_SELECTED : WIDGET_BG_ACTIVE;
        else                 bgTex = hovered ? WIDGET_BG_INACTIVE_SELECTED : WIDGET_BG_INACTIVE;
        RenderSystem.enableBlend();
        gfx.blit(bgTex, bgX, bgY, 0, 0, bgW, bgH, bgW, bgH);
        gfx.blit(w.iconTexture, ix, iy, 0, 0, iw, ih, iw, ih);
        RenderSystem.disableBlend();
    }

    private void renderDecorator(GuiGraphics gfx, DecoraterDefinition d,
                                 int bgX, int bgY, int bgW, int bgH) {
        gfx.blit(d.iconTexture, bgX, bgY, 0, 0, bgW, bgH, bgW, bgH);
        RenderSystem.disableBlend();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POPUP
    // ─────────────────────────────────────────────────────────────────────────

    private void renderPopup(GuiGraphics gfx, int mx, int my, float pt) {
        if (popup == null) return;
        WidgetDefinition w      = popup.widget;
        boolean isSpellPopup    = (w instanceof SpellWidgetDefinition);

        // 1. Gatekeeping Logic
        // Spells always bypass the "Locked" view so players can read descriptions.
        boolean isUnlocked      = ClientKnowledge.isUnlocked(w.id);
        boolean depsOk          = isSpellPopup || ClientKnowledge.areDependenciesMet(w.id);
        List<String> missing    = isSpellPopup ? List.of() : ClientKnowledge.getMissingDependencyLabels(w.id);

        PopupContent content    = w.popupContent;
        int pageCount           = content.pageCount();

        int popW = POPUP_W;
        int popH = canvasH;
        int px   = canvasX + canvasW - popW;
        int py   = canvasY;

        // Background, Shadow, and Border
        gfx.fill(px+5, py+5, px+popW+5, py+popH+5, 0x22000000);
        gfx.fill(px, py, px+popW, py+popH, C_POPUP_BG);
        drawBorder(gfx, px, py, popW, popH, 2, C_POPUP_BORDER);
        drawCornerAccents(gfx, px, py, popW, popH);

        // ── Header bar ────────────────────────────────────────────────────────
        gfx.fill(px, py, px+popW, py+20, C_BORDER);
        gfx.fill(px, py+20, px+popW, py+21, C_ACCENT);
        RenderSystem.enableBlend();
        gfx.blit(w.iconTexture, px+4, py+4, 0, 0, 12, 12, 12, 12);
        RenderSystem.disableBlend();
        gfx.drawString(font, w.label, px+20, py+6, C_ACCENT, false);

        boolean closeHov = mx >= px+popW-14 && mx < px+popW && my >= py && my < py+20;
        gfx.drawString(font, "×", px+popW-10, py+6, closeHov ? 0x000000 : C_TEXT, false);

        // Page Navigation
        if (pageCount > 1) {
            int navY = py + 21;
            gfx.fill(px, navY, px+popW, navY+14, 0x33000000);
            String pageLabel = (popup.page+1) + " / " + pageCount;
            int plw = font.width(pageLabel);
            boolean prevHov = popup.page > 0 && mx >= px+4 && mx < px+14 && my >= navY+2 && my < navY+12;
            gfx.drawString(font, "<", px+6, navY+3, (popup.page > 0) ? (prevHov ? 0xFFFFAA44 : C_ACCENT) : 0xFF888888, false);
            gfx.drawString(font, pageLabel, px+(popW-plw)/2, navY+3, C_ACCENT, false);
            boolean nextHov = popup.page < pageCount-1 && mx >= px+popW-14 && mx < px+popW-4 && my >= navY+2 && my < navY+12;
            gfx.drawString(font, ">", px+popW-12, navY+3, (popup.page < pageCount-1) ? (nextHov ? 0xFFFFAA44 : C_ACCENT) : 0xFF888888, false);
        }

        // ── Content Sizing ───────────────────────────────────────────────────
        int headerTotalH = 21 + (pageCount > 1 ? 14 : 0);
        int contentY     = py + headerTotalH + 4;
        boolean isLastPage = (popup.page == pageCount - 1);

        // Spells ignore Task Zones and Bottom Buttons entirely
        boolean hasTasks    = !isSpellPopup && isLastPage && depsOk && w.hasTasks() && !isUnlocked;
        int taskZoneH       = hasTasks ? (w.taskRequirements.size() * POPUP_TASK_ROW_H + 4) : 0;

        boolean needsConfirm = !isSpellPopup && hasTasks && w.requiresConfirmation && ClientKnowledge.areTasksMet(w.id);
        int confirmRowH      = needsConfirm ? 20 : 0;

        // Bottom buttons only exist for non-spell research entries
        int bottomButtonH    = (isLastPage && !isSpellPopup) ? 22 : 0;

        int bodyH = popH - headerTotalH - 4 - taskZoneH - confirmRowH - bottomButtonH - 4;
        int bodyW = popW - 12;
        int bx    = px + 6;

        // ── Body Rendering ───────────────────────────────────────────────────
        if (!depsOk) {
            // Only non-spell widgets will ever hit this "Locked" state
            int cy = contentY;
            int lh = font.lineHeight + 2;
            gfx.drawString(font, "§cRequires:", bx, cy, C_TEXT, false);
            cy += lh;
            for (String dep : missing) {
                if (cy + lh > contentY + bodyH) break;
                gfx.fill(bx, cy+lh/2-1, bx+3, cy+lh/2+2, 0xFFCC4444);
                gfx.drawString(font, dep, bx+8, cy, 0xFFCC4444, false);
                cy += lh;
            }
        } else {
            PopupContent.Page currentPage = content.pages.get(popup.page);
            popup.hoveredRenderIdx = -1;
            int cy = contentY;
            int lh = font.lineHeight + 2;

            for (int bi = 0; bi < currentPage.blocks.size(); bi++) {
                PopupContent.Block block = currentPage.blocks.get(bi);
                switch (block.type) {
                    case TEXT -> cy = renderBlockText(gfx, block.data, bx, cy, bodyW, contentY + bodyH, lh);
                    case LIST -> cy = renderBlockList(gfx, block.data, bx, cy, bodyW, contentY + bodyH, lh);
                    case IMAGE -> cy = renderBlockImage(gfx, block.data, bx, cy, bodyW, contentY + bodyH);
                    case PML_SOURCE -> cy = PmlRenderer.render(gfx, font, block.pmlNodes(), bx, cy, bodyW, contentY + bodyH, pt);
                    case ENTITY_RENDER -> {
                        int sz = RENDER_SZ;
                        if (cy + sz + 2 > contentY + bodyH) break;
                        boolean renderHov = mx >= bx && mx < bx+sz && my >= cy && my < cy+sz;
                        if (renderHov) popup.hoveredRenderIdx = bi;
                        gfx.fill(bx-2, cy-2, bx+sz+2, cy+sz+2, 0x33000000);
                        drawBorder(gfx, bx-2, cy-2, sz+4, sz+4, 1, C_POPUP_BORDER);
                        if (block.isEntity) renderEntityThumbnail(gfx, block.resourceId, bx, cy, sz, pt);
                        else                renderItemThumbnail(gfx, block.resourceId, bx, cy, sz);
                        if (renderHov && block.data != null && !block.data.isEmpty())
                            drawTooltip(gfx, block.data, mx, my);
                        cy += sz + 4;
                    }
                }
                if (cy >= contentY + bodyH) break;
            }
        }

        // ── Footer UI (Research Entries Only) ────────────────────────────────
        // Spells exit here to avoid rendering progress bars or activation buttons
        if (isSpellPopup) return;

        // Task progress bars
        if (isLastPage && hasTasks) {
            int taskY = py + popH - bottomButtonH - confirmRowH - 4 - taskZoneH;
            gfx.fill(px+6, taskY-2, px+popW-6, taskY-1, C_ACCENT);
            List<TaskRequirement> reqs = w.taskRequirements;
            for (int t = 0; t < reqs.size(); t++) {
                TaskRequirement req = reqs.get(t);
                int current = ClientKnowledge.getTaskProgress(w.id, t);
                boolean done = current >= req.required;
                int rowY = taskY + t * POPUP_TASK_ROW_H;
                int barX = bx, barW = bodyW;
                int barY = rowY + font.lineHeight + 2, barH = 5;
                String countStr   = done ? "✔" : current + " / " + req.required;
                int    countColor = done ? 0xFF44aa44 : 0xFF7a5a3a;
                gfx.drawString(font, req.label, barX, rowY, C_ACCENT, false);
                int cw = font.width(countStr);
                gfx.drawString(font, countStr, barX+barW-cw, rowY, countColor, false);
                gfx.fill(barX, barY, barX+barW, barY+barH, C_BAR_TRACK);
                drawBorder(gfx, barX, barY, barW, barH, 1, C_BAR_BORDER);
                int fillW = done ? barW-2 : (int)((float)current/req.required*(barW-2));
                if (fillW > 0)
                    gfx.fill(barX+1, barY+1, barX+1+fillW, barY+barH-1, done ? C_BAR_DONE : C_BAR_FILL);
            }
        }

        // Confirmation checkbox
        if (isLastPage && needsConfirm) {
            int checkY = py + popH - bottomButtonH - 4 - confirmRowH;
            gfx.fill(px+6, checkY-1, px+popW-6, checkY, C_ACCENT);
            boolean checkHov = mx >= bx && mx < bx+16 && my >= checkY+2 && my < checkY+18;
            ResourceLocation checkTex = popup.confirmed
                    ? WidgetDefinition.CHECKBOX_CHECKED
                    : (checkHov ? WidgetDefinition.CHECKBOX_HOVERED : WidgetDefinition.CHECKBOX_UNCHECKED);
            RenderSystem.enableBlend();
            gfx.blit(checkTex, bx, checkY+2, 0, 0, 14, 14, 14, 14);
            RenderSystem.disableBlend();
            gfx.drawString(font, "Confirm activation", bx+18, checkY+5, C_ACCENT, false);
        }

        // Bottom action buttons
        if (isLastPage) {
            int btnW = 90, btnH = 16;
            int btnX = px + (popW - btnW) / 2;
            int btnY = py + popH - 20;

            if (isUnlocked) {
                renderBottomButton(gfx, "ACTIVE ✔", 0xFF44aa44, false, btnX, btnY, btnW, btnH, mx, my);
            } else if (!depsOk) {
                renderBottomButton(gfx, "LOCKED", 0xFF555555, false, btnX, btnY, btnW, btnH, mx, my);
            } else if (w.hasTasks()) {
                boolean allDone = ClientKnowledge.areTasksMet(w.id);
                if (allDone && (!w.requiresConfirmation || popup.confirmed)) {
                    boolean hov = mx >= btnX && mx < btnX+btnW && my >= btnY && my < btnY+btnH;
                    renderBottomButton(gfx, "ACTIVATE ✔", hov ? 0xFFc4a080 : C_BORDER, true, btnX, btnY, btnW, btnH, mx, my);
                } else {
                    renderBottomButton(gfx, allDone ? "CONFIRM FIRST" : "IN PROGRESS", 0xFF7a5a3a, false, btnX, btnY, btnW, btnH, mx, my);
                }
            } else {
                boolean hov = mx >= btnX && mx < btnX+btnW && my >= btnY && my < btnY+btnH;
                renderBottomButton(gfx, "ACTIVATE", hov ? 0xFFc4a080 : C_BORDER, true, btnX, btnY, btnW, btnH, mx, my);
            }
        }
    }

    // ── Block renderers ───────────────────────────────────────────────────────

    private int renderBlockText(GuiGraphics gfx, String text, int x, int y, int maxW, int maxY, int lh) {
        int cy = y;
        for (String raw : text.split("\n"))
            for (var seq : font.split(Component.literal(raw), maxW)) {
                if (cy + lh > maxY) return cy;
                gfx.drawString(font, seq, x, cy, C_TEXT, false);
                cy += lh;
            }
        return cy + 2;
    }

    private int renderBlockList(GuiGraphics gfx, String data, int x, int y, int maxW, int maxY, int lh) {
        int cy = y;
        for (String entry : data.split("\n")) {
            if (cy + lh > maxY) break;
            gfx.fill(x, cy+lh/2-1, x+3, cy+lh/2+2, C_ACCENT);
            gfx.drawString(font, entry, x+8, cy, C_TEXT, false);
            cy += lh;
        }
        return cy + 2;
    }

    private int renderBlockImage(GuiGraphics gfx, String texStr, int x, int y, int maxW, int maxY) {
        try {
            ResourceLocation rl = new ResourceLocation(texStr);
            int sz = Math.min(maxW, maxY - y);
            RenderSystem.enableBlend();
            gfx.blit(rl, x + (maxW-sz)/2, y, 0, 0, sz, sz, sz, sz);
            RenderSystem.disableBlend();
            return y + sz + 4;
        } catch (Exception e) {
            gfx.drawString(font, "img: " + texStr, x, y, C_TEXT_DIM, false);
            return y + font.lineHeight + 2;
        }
    }

    private void renderEntityThumbnail(GuiGraphics gfx, String entityId, int x, int y, int sz, float partialTick) {
        try {
            ResourceLocation rl = new ResourceLocation(entityId);
            Optional<EntityType<?>> typeOpt = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(rl);
            if (typeOpt.isEmpty()) { renderMissingPlaceholder(gfx, x, y, sz); return; }
            net.minecraft.world.entity.Entity entity = typeOpt.get().create(minecraft.level);
            if (!(entity instanceof LivingEntity living)) { renderMissingPlaceholder(gfx, x, y, sz); return; }
            float yRot  = (minecraft.level.getGameTime() + partialTick) * 2.0f;
            float scale = sz * 0.35f;
            com.mojang.blaze3d.vertex.PoseStack poseStack = gfx.pose();
            poseStack.pushPose();
            poseStack.translate(x + sz/2.0, y + sz*0.85, 50.0);
            poseStack.scale(scale, -scale, scale);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yRot));
            EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
            dispatcher.setRenderShadow(false);
            net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
            RenderSystem.enableDepthTest();
            dispatcher.render(living, 0.0, 0.0, 0.0, yRot, partialTick, poseStack, buffers,
                    net.minecraft.client.renderer.LightTexture.pack(15, 15));
            buffers.endBatch();
            dispatcher.setRenderShadow(true);
            RenderSystem.disableDepthTest();
            poseStack.popPose();
        } catch (Exception e) { renderMissingPlaceholder(gfx, x, y, sz); }
    }

    private void renderItemThumbnail(GuiGraphics gfx, String itemId, int x, int y, int sz) {
        try {
            ResourceLocation rl  = new ResourceLocation(itemId);
            Item             itm = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl);
            ItemStack        stk = new ItemStack(itm);
            if (stk.isEmpty()) { renderMissingPlaceholder(gfx, x, y, sz); return; }
            float scale = sz / 16.0f;
            gfx.pose().pushPose();
            gfx.pose().translate(x, y, 0);
            gfx.pose().scale(scale, scale, 1.0f);
            gfx.renderItem(stk, 0, 0);
            gfx.pose().popPose();
        } catch (Exception e) { renderMissingPlaceholder(gfx, x, y, sz); }
    }

    private void renderMissingPlaceholder(GuiGraphics gfx, int x, int y, int sz) {
        gfx.fill(x, y, x+sz, y+sz, 0xFF442222);
        gfx.drawString(font, "?", x+sz/2-3, y+sz/2-4, 0xFFCCCCCC, false);
    }

    // ── Bottom button helper ──────────────────────────────────────────────────

    private void renderBottomButton(GuiGraphics gfx, String text, int bgColor,
                                    boolean interactive, int x, int y, int w, int h, int mx, int my) {
        gfx.fill(x, y, x+w, y+h, bgColor);
        drawBorder(gfx, x, y, w, h, 1, C_ACCENT);
        int tw = font.width(text);
        int textColor = interactive ? C_ACCENT : 0xFFd4c8c0;
        gfx.drawString(font, text, x+(w-tw)/2, y+(h-font.lineHeight)/2, textColor, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INPUT
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (popup != null) {
            WidgetDefinition w          = popup.widget;
            boolean          depsOk    = ClientKnowledge.areDependenciesMet(w.id);
            boolean          isUnlocked = ClientKnowledge.isUnlocked(w.id);
            boolean          isSpellPopup = (w instanceof SpellWidgetDefinition);

            int popW = POPUP_W, popH = canvasH;
            int ppx = canvasX + canvasW - popW, ppy = canvasY;

            // Close button
            if (mx >= ppx+popW-14 && mx < ppx+popW && my >= ppy && my < ppy+20) {
                popup = null; return true;
            }

            boolean inside = mx >= ppx && mx < ppx+popW && my >= ppy && my < ppy+popH;
            if (!inside) { popup = null; return true; }

            int pageCount    = w.popupContent.pageCount();
            int headerTotalH = 21 + (pageCount > 1 ? 14 : 0);

            // Page navigation
            if (pageCount > 1) {
                int navY = ppy + 21;
                if (my >= navY && my < navY+14) {
                    if (mx >= ppx+4 && mx < ppx+14 && popup.page > 0)            { popup.page--; return true; }
                    if (mx >= ppx+popW-14 && mx < ppx+popW-4 && popup.page < pageCount-1) { popup.page++; return true; }
                }
            }

            // Confirmation checkbox
            boolean needsConfirm = depsOk && w.hasTasks() && !isUnlocked
                    && w.requiresConfirmation && ClientKnowledge.areTasksMet(w.id);
            if (needsConfirm) {
                int confirmRowH = 20;
                int btnH2       = isUnlocked && isSpellPopup ? 44 : 22;
                int checkY      = ppy + popH - btnH2 - 4 - confirmRowH;
                int cbx         = ppx + 6;
                if (mx >= cbx && mx < cbx+16 && my >= checkY+2 && my < checkY+18) {
                    popup.confirmed = !popup.confirmed; return true;
                }
            }

            boolean isLastPageClick = (popup.page == pageCount - 1);

            if (isLastPageClick) {
                int btnW = 90, btnH = 16;
                int btnX = ppx + (popW - btnW) / 2;

                // ── Deactivate button (spell only, when unlocked) ──────────────
                if (isUnlocked && isSpellPopup) {
                    int deactY = ppy + popH - 20;
                    if (mx >= btnX && mx < btnX+btnW && my >= deactY && my < deactY+btnH) {
                        ModPackets.sendToServer(new DeactivateWidgetPacket(w.id));
                        popup = null;
                        return true;
                    }
                }

                // ── Activate button (non-unlocked path) ───────────────────────
                if (!isUnlocked) {
                    int btnY = ppy + popH - 20;
                    if (mx >= btnX && mx < btnX+btnW && my >= btnY && my < btnY+btnH) {
                        boolean canActivate = depsOk
                                && (!w.hasTasks() || (ClientKnowledge.areTasksMet(w.id)
                                && (!w.requiresConfirmation || popup.confirmed)));
                        if (canActivate) ModPackets.sendToServer(new ActivateWidgetPacket(w.id));
                        return true;
                    }
                }
            }

            return true;
        }

        // ── Tab bar ───────────────────────────────────────────────────────────
        int tabW = 24, tabH = 24, spacing = 2;
        for (int i = 0; i < tabs.size(); i++) {
            int tx = frameX+frameW, ty = frameY+(i*(tabH+spacing));
            if (mx >= tx && mx < tx+tabW && my >= ty && my < ty+tabH) {
                if (i != activeTab) { activeTab = i; scrollX = scrollY = 0; zoom = 1f; popup = null; }
                return true;
            }
        }

        if ((btn == GLFW.GLFW_MOUSE_BUTTON_RIGHT || btn == GLFW.GLFW_MOUSE_BUTTON_MIDDLE)
                && isInCanvas((int)mx, (int)my)) {
            dragging = true; dragLastX = mx; dragLastY = my; return true;
        }

        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT && isInCanvas((int)mx, (int)my)) {
            if (!tabs.isEmpty()) {
                TabDefinition tab = tabs.get(activeTab);

                // ── Spell widgets ─────────────────────────────────────────────
                if (tab.name.equals("Psychic")) {
                    List<String> history = ClientKnowledge.getSpellOrder();

                    int slotIndex = 0; // Only increments when a valid widget is found
                    for (String spellId : history) {
                        SpellWidgetDefinition s = TabRegistry.findSpellById(spellId);

                        // Skip missing widgets: slotIndex does NOT increment.
                        // This keeps the hitbox aligned with the visible compact spiral.
                        if (s == null) continue;

                        double angle      = slotIndex * SPELL_ANGLE_STEP;
                        float  radius     = SPELL_ORBIT_RADIUS_START + (slotIndex * SPELL_ORBIT_RADIUS_STEP);
                        float  canvasXPos = (float)(Math.cos(angle) * radius);
                        float  canvasYPos = (float)(Math.sin(angle) * radius);

                        float cx = canvasX + (canvasW / 2f) + (canvasXPos - scrollX) * zoom;
                        float cy = canvasY + (canvasH / 2f) + (canvasYPos - scrollY) * zoom;

                        int drawSize = Math.round(s.size * zoom);
                        float xPos   = cx - drawSize / 2f;
                        float yPos   = cy - drawSize / 2f;

                        // Hitbox check
                        if (mx >= xPos && mx < xPos + drawSize && my >= yPos && my < yPos + drawSize) {
                            popup = new PopupState(s);
                            return true;
                        }

                        // Only advance to the next spiral position after a valid widget is processed
                        slotIndex++;
                    }
                }

                // ── Normal widgets ────────────────────────────────────────────
                for (WidgetDefinition w : tab.widgets) {
                    float centerX = canvasX + (canvasW/2f) + (w.canvasX-scrollX)*zoom;
                    float centerY = canvasY + (canvasH/2f) + (w.canvasY-scrollY)*zoom;
                    float iconW = w.iconW*zoom, iconH = w.iconH*zoom;
                    float padding = Math.max(0.5f, 4.0f*zoom);
                    float bgW = iconW+padding*2f, bgH = iconH+padding*2f;
                    float hitX = centerX-bgW/2f, hitY = centerY-bgH/2f;
                    if (mx >= hitX && mx < hitX+bgW && my >= hitY && my < hitY+bgH) {
                        popup = new PopupState(w); return true;
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
        if (dragging) { scrollX -= (float)(dx/zoom); scrollY -= (float)(dy/zoom); return true; }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!isInCanvas((int)mx, (int)my) || popup != null) return false;
        float oldZoom = zoom;
        zoom = Mth.clamp(zoom * (delta > 0 ? 1.1f : 0.9f), ZOOM_MIN, ZOOM_MAX);
        float dx = (float)(mx - (canvasX+canvasW/2f)), dy = (float)(my - (canvasY+canvasH/2f));
        scrollX += dx/oldZoom - dx/zoom;
        scrollY += dy/oldZoom - dy/zoom;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (popup != null) { popup = null; return true; }
            onClose(); return true;
        }
        if (popup != null) {
            int pc = popup.widget.popupContent.pageCount();
            if (key == GLFW.GLFW_KEY_LEFT  && popup.page > 0)    { popup.page--; return true; }
            if (key == GLFW.GLFW_KEY_RIGHT && popup.page < pc-1) { popup.page++; return true; }
            return super.keyPressed(key, scan, mods);
        }
        float step = 40/zoom;
        return switch (key) {
            case GLFW.GLFW_KEY_LEFT  -> { scrollX -= step; clampScroll(); yield true; }
            case GLFW.GLFW_KEY_RIGHT -> { scrollX += step; clampScroll(); yield true; }
            case GLFW.GLFW_KEY_UP    -> { scrollY -= step; clampScroll(); yield true; }
            case GLFW.GLFW_KEY_DOWN  -> { scrollY += step; clampScroll(); yield true; }
            case GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD ->
            { zoom = Mth.clamp(zoom+ZOOM_STEP*3, ZOOM_MIN, ZOOM_MAX); yield true; }
            case GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT ->
            { zoom = Mth.clamp(zoom-ZOOM_STEP*3, ZOOM_MIN, ZOOM_MAX); yield true; }
            default -> super.keyPressed(key, scan, mods);
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isInCanvas(int sx, int sy) {
        return sx >= canvasX && sx < canvasX+canvasW && sy >= canvasY && sy < canvasY+canvasH;
    }

    private void clampScroll() {
        if (tabs.isEmpty()) return;
        TabDefinition t = tabs.get(activeTab);
        scrollX = Mth.clamp(scrollX, 0, Math.max(0, t.canvasWidth  - canvasW/zoom));
        scrollY = Mth.clamp(scrollY, 0, Math.max(0, t.canvasHeight - canvasH/zoom));
    }

    private void drawBorder(GuiGraphics gfx, int x, int y, int w, int h, int t, int col) {
        gfx.fill(x,     y,     x+w,   y+t,   col);
        gfx.fill(x,     y+h-t, x+w,   y+h,   col);
        gfx.fill(x,     y,     x+t,   y+h,   col);
        gfx.fill(x+w-t, y,     x+w,   y+h,   col);
    }

    private void drawCornerAccents(GuiGraphics gfx, int x, int y, int w, int h) {
        int s = 6, col = C_ACCENT;
        gfx.fill(x,     y,     x+s,   y+1,   col); gfx.fill(x,     y,     x+1,   y+s,   col);
        gfx.fill(x+w-s, y,     x+w,   y+1,   col); gfx.fill(x+w-1, y,     x+w,   y+s,   col);
        gfx.fill(x,     y+h-1, x+s,   y+h,   col); gfx.fill(x,     y+h-s, x+1,   y+h,   col);
        gfx.fill(x+w-s, y+h-1, x+w,   y+h,   col); gfx.fill(x+w-1, y+h-s, x+w,   y+h,   col);
    }

    private void drawTooltip(GuiGraphics gfx, String text, int mx, int my) {
        int tw = font.width(text) + 8, th = font.lineHeight + 6;
        int tx = Mth.clamp(mx+12, frameX, frameX+frameW-tw);
        int ty = Mth.clamp(my-th-4, frameY+TAB_H, frameY+frameH-th);
        gfx.fill(tx, ty, tx+tw, ty+th, C_POPUP_BG);
        drawBorder(gfx, tx, ty, tw, th, 1, C_ACCENT);
        gfx.drawString(font, text, tx+4, ty+3, C_TEXT, false);
    }

    private void enableScissor(int x, int y, int w, int h) {
        double s = minecraft.getWindow().getGuiScale();
        RenderSystem.enableScissor((int)(x*s), (int)((height-y-h)*s), (int)(w*s), (int)(h*s));
    }

    private void disableScissor() { RenderSystem.disableScissor(); }

    @Override public boolean isPauseScreen() { return false; }
}