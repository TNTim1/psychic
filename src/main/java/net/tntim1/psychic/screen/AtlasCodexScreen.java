package net.tntim1.psychic.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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

    // ── palette ───────────────────────────────────────────────────────────────
    private static final int C_BG             = 0xFF0D0D1A;
    private static final int C_FRAME          = 0xFF12122A;
    private static final int C_BORDER         = 0xFF2A1A5E;
    private static final int C_ACCENT         = 0xFFAA44FF;
    private static final int C_ACCENT2        = 0xFF6622CC;
    private static final int C_TAB_ACTIVE     = 0xFF2A1A5E;
    private static final int C_TAB_INACTIVE   = 0xFF161630;
    private static final int C_TAB_HOVER      = 0xFF1E1848;
    private static final int C_TAB_TEXT       = 0xFFD0C0FF;
    private static final int C_CANVAS_BG      = 0xFF080812;
    private static final int C_GRID           = 0x1AFFFFFF;
    private static final int C_GRID_MAJOR     = 0x33AA44FF;
    private static final int C_WIDGET_BG      = 0xCC12122A;
    private static final int C_WIDGET_BORDER  = 0xFF2A1A5E;
    private static final int C_WIDGET_HOVER   = 0xFFAA44FF;
    private static final int C_POPUP_BG       = 0xF00D0D1A;
    private static final int C_POPUP_BORDER   = 0xFFAA44FF;
    private static final int C_TEXT           = 0xFFD0C0FF;
    private static final int C_TEXT_DIM       = 0xFF6655AA;
    private static final int C_SCROLLBAR      = 0xFF161630;
    private static final int C_SCROLLTHUMB    = 0xFF6622CC;

    // ── layout ───────────────────────────────────────────────────────────────
    private static final int TAB_H      = 26;
    private static final int FRAME_PAD  = 6;
    private static final int CLOSE_W    = 20;
    private static final int SB_THICK   = 5;

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
        frameX  = m;
        frameY  = m;
        frameW  = width  - m * 2 - TAB_H;
        frameH  = height - m * 2;
        canvasX = frameX  + FRAME_PAD;
        canvasY = frameY  + FRAME_PAD;
        canvasW = frameW  - FRAME_PAD * 2;
        canvasH = frameH  - FRAME_PAD * 2;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RENDER
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float pt) {
        recalcLayout();
        renderBackground(gfx);

        // Outer frame + corner accents
        gfx.fill(frameX, frameY, frameX + frameW, frameY + frameH, C_BORDER);
        drawBorder(gfx, frameX, frameY, frameW, frameH, 2, C_ACCENT);
        //drawCornerAccents(gfx, frameX, frameY, frameW, frameH);

        renderTabBar(gfx, mx, my);
        renderCanvas(gfx, mx, my);
        if (popup != null) renderPopup(gfx, mx, my);



        super.render(gfx, mx, my, pt);
    }

    // ── Tab bar ───────────────────────────────────────────────────────────────



    // ── Canvas ────────────────────────────────────────────────────────────────

    private void renderCanvas(GuiGraphics gfx, int mx, int my) {
        gfx.fill(canvasX, canvasY, canvasX + canvasW, canvasY + canvasH, C_CANVAS_BG);

        enableScissor(canvasX, canvasY, canvasW, canvasH);

        //drawGrid(gfx);

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
        //renderScrollBars(gfx);
    }

    private void renderTabBar(GuiGraphics gfx, int mx, int my) {
        int tabCount = tabs.size();
        int tabW = 24;
        int tabH = 24;
        int spacing = 2;

        for (int i = 0; i < tabCount; i++) {
            // Position tabs to the RIGHT of the main frame
            int tx = frameX + frameW;
            int ty = frameY + (i * (tabH + spacing));

            boolean active  = i == activeTab;
            boolean hovered = !active && mx >= tx && mx < tx + tabW && my >= ty && my < ty + tabH;

            int bg = active ? C_TAB_ACTIVE : (hovered ? C_TAB_HOVER : C_TAB_INACTIVE);



            // Use C_ACCENT for active tab, C_BORDER for others\
            gfx.fill(tx , ty, tx + tabW, ty + tabH, bg);
            drawBorder(gfx, tx, ty, tabW, tabH, active ? 2 : 1, active ? C_ACCENT : C_BORDER);


            if (active) {
                gfx.fill(tx -5, ty+2, tx + tabW-2, ty + tabH-2, bg);
            }

            String label = tabs.get(i).name.substring(0, 1).toUpperCase();
            gfx.drawString(font, label,
                    tx + (tabW - font.width(label)) / 2,
                    ty + (tabH - font.lineHeight) / 2,
                    C_TAB_TEXT, false);
        }
    }



    private void renderWidget(GuiGraphics gfx, WidgetDefinition w,
                              int wx, int wy, int ww, int wh, boolean hovered) {
        int pad = 4;
        // glow shadow when hovered
        if (hovered) {
            gfx.fill(wx - pad - 2, wy - pad - 2, wx + ww + pad + 2, wy + wh + pad + 2, 0x44AA44FF);
        }
        gfx.fill(wx - pad, wy - pad, wx + ww + pad, wy + wh + pad, C_WIDGET_BG);
        drawBorder(gfx, wx - pad, wy - pad, ww + pad * 2, wh + pad * 2, 1,
                hovered ? C_WIDGET_HOVER : C_WIDGET_BORDER);

        // Icon
        RenderSystem.setShaderTexture(0, w.iconTexture);
        RenderSystem.enableBlend();
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
        gfx.fill(px + 5, py + 5, px + popW + 5, py + popH + 5, 0xAA000000);
        // Background
        gfx.fill(px, py, px + popW, py + popH, C_POPUP_BG);
        drawBorder(gfx, px, py, popW, popH, 2, C_POPUP_BORDER);
        drawCornerAccents(gfx, px, py, popW, popH);

        // Title bar
        gfx.fill(px, py, px + popW, py + 20, C_TAB_ACTIVE);
        gfx.fill(px, py + 20, px + popW, py + 21, C_ACCENT);

        // Icon in title
        if (w.iconW > 0 && w.iconH > 0) {
            RenderSystem.setShaderTexture(0, w.iconTexture);
            RenderSystem.enableBlend();
            gfx.blit(w.iconTexture, px + 4, py + 4, 0, 0, 12, 12, 12, 12);
            RenderSystem.disableBlend();
        }
        gfx.drawString(font, w.label, px + 20, py + 6, C_ACCENT, false);

        // Close ×
        boolean closeHov = mx >= px + popW - 14 && mx < px + popW && my >= py && my < py + 20;
        gfx.drawString(font, "×", px + popW - 10, py + 6,
                closeHov ? 0xFFFFFFFF : 0xFFCC4488, false);

        // Body
        int bx = px + 6, by = py + 24, bw = popW - 12, bh = popH - 28;
        int lh = font.lineHeight + 2;
        switch (w.popupType) {
            case INFO   -> renderBodyInfo(gfx, w.popupData, bx, by, bw, bh, lh);
            case LIST   -> renderBodyList(gfx, w.popupData, bx, by, bw, bh, lh);
            case IMAGE  -> renderBodyImage(gfx, w.popupData, bx, by, bw, bh);
            case CUSTOM -> gfx.drawString(font, "[custom:" + w.popupData + "]", bx, by, C_TEXT_DIM, false);
        }
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
            // bullet dot
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
        // Popup-open: only let clicks through to close or interact with popup
        if (popup != null) {
            int popW = 210, popH = 150;
            int px = Mth.clamp(popup.originX + 18, frameX + 4, frameX + frameW - popW - 4);
            int py = Mth.clamp(popup.originY - popH / 2, frameY + TAB_H + 4, frameY + frameH - popH - 4);
            boolean inside = mx >= px && mx < px + popW && my >= py && my < py + popH;
            boolean onX    = mx >= px + popW - 14 && mx < px + popW && my >= py && my < py + 20;
            if (!inside || onX) { popup = null; }
            return true;
        }

        // Tab bar (Right Side Vertical)
        int tabW = 24;
        int tabH = 24;
        int spacing = 2;

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

        // Drag start (right / middle)
        if ((btn == GLFW.GLFW_MOUSE_BUTTON_RIGHT || btn == GLFW.GLFW_MOUSE_BUTTON_MIDDLE)
                && isInCanvas((int)mx, (int)my)) {
            dragging = true; dragLastX = mx; dragLastY = my;
            return true;
        }

        // Widget click (left)
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

        // keep cursor-point fixed on canvas
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

    /** Small purple corner marks — give the frame a "psychic rune" feel. */
    private void drawCornerAccents(GuiGraphics gfx, int x, int y, int w, int h) {
        int s = 6;
        int col = C_ACCENT;
        // TL
        gfx.fill(x, y, x + s, y + 1, col); gfx.fill(x, y, x + 1, y + s, col);
        // TR
        gfx.fill(x + w - s, y, x + w, y + 1, col); gfx.fill(x + w - 1, y, x + w, y + s, col);
        // BL
        gfx.fill(x, y + h - 1, x + s, y + h, col); gfx.fill(x, y + h - s, x + 1, y + h, col);
        // BR
        gfx.fill(x + w - s, y + h - 1, x + w, y + h, col); gfx.fill(x + w - 1, y + h - s, x + w, y + h, col);
    }

    private void drawTooltip(GuiGraphics gfx, String text, int mx, int my) {
        int tw = font.width(text) + 8, th = font.lineHeight + 6;
        int tx = Mth.clamp(mx + 12, frameX, frameX + frameW - tw);
        int ty = Mth.clamp(my - th - 4, frameY + TAB_H, frameY + frameH - th);
        gfx.fill(tx, ty, tx + tw, ty + th, 0xEE080812);
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
