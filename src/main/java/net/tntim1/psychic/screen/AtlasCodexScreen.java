package net.tntim1.psychic.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.tntim1.psychic.network.ActivateWidgetPacket;
import net.tntim1.psychic.network.ModPackets;
import net.tntim1.psychic.player_data.ClientKnowledge;
import net.tntim1.psychic.widget.PopupType;
import net.tntim1.psychic.widget.TabDefinition;
import net.tntim1.psychic.widget.TabRegistry;
import net.tntim1.psychic.widget.WidgetDefinition;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The Atlas Codex fullscreen GUI.
 *
 * <p>Layout:
 * <pre>
 *  ┌─────────────────────────────────────────────────────┐
 *  │  [Tab A]  [Tab B]  [Tab C]          [×]  (top bar) │
 *  ├─────────────────────────────────────────────────────┤
 *  │                                                     │
 *  │        scrollable + zoomable canvas                 │
 *  │                                                     │
 *  └─────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>Controls:
 * <ul>
 *   <li>Right-drag or Middle-drag → pan</li>
 *   <li>Scroll wheel              → zoom (centres on cursor)</li>
 *   <li>+ / - keys               → zoom</li>
 *   <li>Arrow keys               → pan</li>
 *   <li>Left-click widget        → open popup</li>
 *   <li>Left-click outside popup → close popup</li>
 *   <li>Esc                      → close popup then close GUI</li>
 * </ul>
 */
public class AtlasCodexScreen extends Screen {
    private static final ResourceLocation WIDGET_BG_INACTIVE          = new ResourceLocation("psychic", "textures/gui/widgets/widget_background_inactive.png");
    private static final ResourceLocation WIDGET_BG_INACTIVE_SELECTED = new ResourceLocation("psychic", "textures/gui/widgets/widget_background_inactive_selected.png");
    private static final ResourceLocation WIDGET_BG_ACTIVE            = new ResourceLocation("psychic", "textures/gui/widgets/widget_background_active.png");
    private static final ResourceLocation WIDGET_BG_ACTIVE_SELECTED   = new ResourceLocation("psychic", "textures/gui/widgets/widget_background_active_selected.png");

    // ── palette ───────────────────────────────────────────────────────────────
    private static final int C_BORDER        = 0xFFd4a9a2;
    private static final int C_ACCENT        = 0xFF392420;
    private static final int C_TAB_INACTIVE  = 0xFF000000;
    private static final int C_TAB_HOVER     = 0xFFd18a7d;
    private static final int C_CANVAS_BG     = 0xFFFFFFFF;
    private static final int C_POPUP_BG      = 0xFFd4a9a2;
    private static final int C_POPUP_BORDER  = 0xFF392420;
    private static final int C_TEXT          = 0xFF000000;
    private static final int C_TEXT_DIM      = 0xFF392420;

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

    // ── derived layout (recalculated each frame) ──────────────────────────────
    private int frameX, frameY, frameW, frameH;
    private int canvasX, canvasY, canvasW, canvasH;

    // ── popup record ──────────────────────────────────────────────────────────
    private record PopupState(WidgetDefinition widget, int originX, int originY) {}

    // ─────────────────────────────────────────────────────────────────────────

    public AtlasCodexScreen() {
        super(Component.literal("Atlas Codex"));
    }

    @Override
    protected void init() {
        super.init();
        recalcLayout();
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

            if (active) {
                gfx.fill(tx - 5, ty + 2, tx + tabW - 2, ty + tabH - 2, bg);
            }

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

        hoveredWidget = -1;
        if (!tabs.isEmpty()) {
            TabDefinition tab = tabs.get(activeTab);
            for (int i = 0; i < tab.widgets.size(); i++) {
                WidgetDefinition w = tab.widgets.get(i);
                int wx = canvasToScreenX(w.canvasX);
                int wy = canvasToScreenY(w.canvasY);
                int ww = Math.max(1, (int)(w.iconW * zoom));
                int wh = Math.max(1, (int)(w.iconH * zoom));
                int pad = 4;
                boolean hov = popup == null
                        && mx >= wx - pad && mx < wx + ww + pad
                        && my >= wy - pad && my < wy + wh + pad;
                if (hov) hoveredWidget = i;
                renderWidget(gfx, w, wx, wy, ww, wh, hov);
            }

            if (hoveredWidget >= 0) {
                drawTooltip(gfx, tab.widgets.get(hoveredWidget).label, mx, my);
            }
        }

        disableScissor();
    }

    private void renderWidget(GuiGraphics gfx, WidgetDefinition w,
                              int wx, int wy, int ww, int wh, boolean hovered) {
        int pad = (int)(4 * zoom);
        int bgX = wx - pad, bgY = wy - pad;
        int bgW = ww + (pad * 2), bgH = wh + (pad * 2);

        boolean isUnlocked = ClientKnowledge.isUnlocked(w.id);
        ResourceLocation bgTexture = isUnlocked
                ? (hovered ? WIDGET_BG_ACTIVE_SELECTED   : WIDGET_BG_ACTIVE)
                : (hovered ? WIDGET_BG_INACTIVE_SELECTED : WIDGET_BG_INACTIVE);

        RenderSystem.setShaderTexture(0, bgTexture);
        RenderSystem.enableBlend();
        gfx.blit(bgTexture, bgX, bgY, 0, 0, bgW, bgH, bgW, bgH);

        RenderSystem.setShaderTexture(0, w.iconTexture);
        gfx.blit(w.iconTexture, wx, wy, 0, 0, ww, wh, ww, wh);

        RenderSystem.disableBlend();
    }

    // ── Popup ─────────────────────────────────────────────────────────────────

    private void renderPopup(GuiGraphics gfx, int mx, int my) {
        if (popup == null) return;
        WidgetDefinition w = popup.widget;

        int popW = 210, popH = 150;
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

        // Body
        int bx = px + 6, by = py + 24, bw = popW - 12, bh = popH - 28;
        int lh = font.lineHeight + 2;
        switch (w.popupType) {
            case INFO   -> renderBodyInfo(gfx, w.popupData, bx, by, bw, bh, lh);
            case LIST   -> renderBodyList(gfx, w.popupData, bx, by, bw, bh, lh);
            case IMAGE  -> renderBodyImage(gfx, w.popupData, bx, by, bw, bh);
            case CUSTOM -> gfx.drawString(font, "[custom:" + w.popupData + "]", bx, by, C_TEXT_DIM, false);
        }

        // ── ACTIVATE button ───────────────────────────────────────────────────
        int btnW = 80, btnH = 16;
        int btnX = px + (popW - btnW) / 2;
        int btnY = py + popH - 22;

        boolean isUnlocked = ClientKnowledge.isUnlocked(w.id);
        boolean btnHov     = mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH;

        int btnCol = isUnlocked ? 0xFF55FF55 : (btnHov ? 0xFFc4a080 : C_BORDER);
        gfx.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnCol);
        drawBorder(gfx, btnX, btnY, btnW, btnH, 1, C_ACCENT);

        String btnText  = isUnlocked ? "UNLOCKED" : "ACTIVATE";
        int    textW    = font.width(btnText);
        gfx.drawString(font, btnText, btnX + (btnW - textW) / 2, btnY + 4, C_ACCENT, false);
    }

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

        // ── Popup is open → handle interactions inside it ─────────────────────
        if (popup != null) {
            int popW = 210, popH = 150;
            int px = Mth.clamp(popup.originX + 18, frameX + 4, frameX + frameW - popW - 4);
            int py = Mth.clamp(popup.originY - popH / 2, frameY + TAB_H + 4, frameY + frameH - popH - 4);

            int btnW = 80, btnH = 16;
            int btnX = px + (popW - btnW) / 2;
            int btnY = py + popH - 22;

            if (mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH) {
                // Only send the packet if not already unlocked
                if (!ClientKnowledge.isUnlocked(popup.widget.id)) {
                    ModPackets.CHANNEL.sendToServer(new ActivateWidgetPacket(popup.widget.id));
                }
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

        // ── Drag start (right / middle) ───────────────────────────────────────
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