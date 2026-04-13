package net.tntim1.psychic.widget.pml;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Walks a parsed PML tree and draws each node into a {@link GuiGraphics} context.
 *
 * <h3>Coordinate contract</h3>
 * Every render method receives the current Y cursor and returns the new Y cursor
 * after the element has been fully drawn. Callers handle clipping; this renderer
 * simply stops early when {@code cy >= maxY}.
 *
 * <h3>Usage</h3>
 * <pre>
 *   List&lt;PmlNode&gt; nodes = PmlParser.parse(pmlSource);
 *   int newY = PmlRenderer.render(gfx, font, nodes, x, y, maxW, maxY, partialTick);
 * </pre>
 */
public final class PmlRenderer {

    // ── Panel colour palette ──────────────────────────────────────────────────
    private static final int BG_DARK       = 0xCC1a1208;
    private static final int BG_ACCENT     = 0x44392420;
    private static final int BG_LIGHT      = 0x33d4a9a2;
    private static final int BORDER_ACCENT = 0xFF392420;
    private static final int BORDER_DIM    = 0x88392420;
    private static final int DIVIDER_COLOR = 0x88392420;

    // ── Private data holder for one inline text segment ───────────────────────
    private static final class Run {
        final String text; final PmlStyle style;
        Run(String text, PmlStyle style) { this.text = text; this.style = style; }
    }

    // =========================================================================
    // PUBLIC ENTRY POINT
    // =========================================================================

    /**
     * Renders all top-level nodes in order.
     *
     * @return Y cursor after the last rendered element
     */
    public static int render(GuiGraphics gfx, Font font,
                             List<PmlNode> nodes,
                             int x, int y, int maxW, int maxY,
                             float partialTick) {
        int cy = y;
        for (PmlNode node : nodes) {
            if (cy >= maxY) break;
            cy = renderNode(gfx, font, node, x, cy, maxW, maxY, PmlStyle.DEFAULT, partialTick);
        }
        return cy;
    }

    // =========================================================================
    // DISPATCH
    // =========================================================================

    private static int renderNode(GuiGraphics gfx, Font font, PmlNode node,
                                  int x, int y, int maxW, int maxY,
                                  PmlStyle parent, float pt) {
        if (node.isTextNode())
            return renderBareText(gfx, font, node.text, x, y, maxW, maxY, parent);

        return switch (node.tag) {
            case "text", "span" -> renderTextBlock(gfx, font, node, x, y, maxW, maxY, parent);
            case "list"         -> renderList(gfx, font, node, x, y, maxW, maxY, parent);
            case "render"       -> renderRender(gfx, font, node, x, y, maxW, maxY, pt);
            case "panel"        -> renderPanel(gfx, font, node, x, y, maxW, maxY, parent, pt);
            case "divider"      -> renderDivider(gfx, node, x, y, maxW, maxY);
            default             -> y; // unknown tag — skip silently
        };
    }

    // =========================================================================
    // <text>
    // =========================================================================

    private static int renderTextBlock(GuiGraphics gfx, Font font, PmlNode node,
                                       int x, int y, int maxW, int maxY,
                                       PmlStyle parent) {
        PmlStyle style = PmlStyle.fromNode(node, parent);
        int cy = y + style.marginTop;
        if (cy >= maxY) return cy;
        cy = renderRuns(gfx, font, collectRuns(node, style), x, cy, maxW, maxY, style.align);
        return cy + 2;
    }

    private static int renderBareText(GuiGraphics gfx, Font font,
                                      String text, int x, int y,
                                      int maxW, int maxY, PmlStyle style) {
        if (text == null || text.isEmpty()) return y;
        return renderRuns(gfx, font, List.of(new Run(text, style)),
                x, y, maxW, maxY, style.align);
    }

    // =========================================================================
    // INLINE RUN COLLECTION  (<text> children = text nodes + <span> elements)
    // =========================================================================

    private static List<Run> collectRuns(PmlNode node, PmlStyle blockStyle) {
        List<Run> out = new ArrayList<>();
        for (PmlNode child : node.children()) {
            if (child.isTextNode()) {
                if (!child.text.isEmpty()) out.add(new Run(child.text, blockStyle));
            } else if ("span".equals(child.tag)) {
                PmlStyle spanStyle = PmlStyle.fromInlineNode(child, blockStyle);
                for (PmlNode sc : child.children())
                    if (sc.isTextNode() && !sc.text.isEmpty())
                        out.add(new Run(sc.text, spanStyle));
            }
        }
        return out;
    }

    // =========================================================================
    // WORD-WRAP + DRAW
    // =========================================================================

    /**
     * Word-wraps a flat list of runs and emits each line via {@link #emitLine}.
     *
     * <p>Mixed-style runs (e.g. a gold word in the middle of a sentence) are
     * handled because the line accumulator tracks per-word style separately.
     */
    private static int renderRuns(GuiGraphics gfx, Font font,
                                  List<Run> runs,
                                  int x, int y, int maxW, int maxY,
                                  PmlStyle.Align align) {
        if (runs.isEmpty()) return y;

        // Explode each run into per-word fragments
        List<String>   wText  = new ArrayList<>();
        List<PmlStyle> wStyle = new ArrayList<>();
        for (Run r : runs) {
            // Split on whitespace, keeping the whitespace as separate tokens
            String[] parts = r.text.split("(?<=\\s)|(?=\\s)");
            for (String p : parts) { wText.add(p); wStyle.add(r.style); }
        }

        // Accumulate words into lines
        List<String>   lineT = new ArrayList<>();
        List<PmlStyle> lineS = new ArrayList<>();
        int lineUsedW = 0, lineH = 0;
        int cy = y;

        for (int wi = 0; wi < wText.size(); wi++) {
            String   word  = wText.get(wi);
            PmlStyle st    = wStyle.get(wi);
            boolean  space = word.isBlank();

            if (space && lineUsedW == 0) continue; // skip leading space on new line

            int ww = scaledWidth(font, word, st);
            int lh = st.lineHeight();

            if (lineUsedW + ww > maxW && lineUsedW > 0) {
                if (cy < maxY) cy = emitLine(gfx, font, lineT, lineS, x, cy, maxW, align, lineH);
                lineT.clear(); lineS.clear(); lineUsedW = 0; lineH = 0;
                if (space) continue;
            }

            lineT.add(word); lineS.add(st);
            lineUsedW += ww;
            lineH = Math.max(lineH, lh);
        }

        if (!lineT.isEmpty() && cy < maxY)
            cy = emitLine(gfx, font, lineT, lineS, x, cy, maxW, align, lineH);

        return cy;
    }

    private static int scaledWidth(Font font, String text, PmlStyle st) {
        return Math.round(font.width(text) * st.scaleFactor());
    }

    /** Draws one accumulated line of segments with horizontal alignment. */
    private static int emitLine(GuiGraphics gfx, Font font,
                                List<String> texts, List<PmlStyle> styles,
                                int x, int y, int maxW,
                                PmlStyle.Align align, int lineH) {
        int totalW = 0;
        for (int i = 0; i < texts.size(); i++)
            totalW += scaledWidth(font, texts.get(i), styles.get(i));

        int drawX = switch (align) {
            case CENTER -> x + Math.max(0, (maxW - totalW) / 2);
            case RIGHT  -> x + Math.max(0, maxW - totalW);
            default     -> x;
        };

        for (int i = 0; i < texts.size(); i++) {
            PmlStyle st = styles.get(i);
            String seg  = texts.get(i);
            float scale = st.scaleFactor();
            int   sw    = scaledWidth(font, seg, st);

            gfx.pose().pushPose();
            gfx.pose().translate(drawX, y, 0);
            gfx.pose().scale(scale, scale, 1.0f);
            gfx.drawString(font, seg, 0, 0, st.color, false);
            gfx.pose().popPose();

            drawX += sw;
        }

        return y + lineH + 1;
    }

    // =========================================================================
    // <list>
    // =========================================================================

    private static int renderList(GuiGraphics gfx, Font font, PmlNode node,
                                  int x, int y, int maxW, int maxY, PmlStyle parent) {
        PmlStyle style = PmlStyle.fromNode(node, parent);
        int cy         = y + style.marginTop;
        String bullet  = node.attr("bullet", "dot");
        int indent     = node.attrInt("indent", 8);
        int gap        = node.attrInt("gap", 2);
        int itemNum    = 1;

        for (PmlNode child : node.children()) {
            if (!"item".equals(child.tag)) continue;
            if (cy >= maxY) break;

            PmlStyle itemStyle = PmlStyle.fromInlineNode(child, style);
            List<Run> runs     = collectRuns(child, itemStyle);

            String glyph = switch (bullet) {
                case "dash"   -> "–";
                case "number" -> (itemNum++) + ".";
                case "none"   -> "";
                default       -> "•";
            };
            if (!glyph.isEmpty())
                gfx.drawString(font, glyph, x, cy, style.color, false);

            int afterItem = renderRuns(gfx, font, runs,
                    x + indent, cy, maxW - indent, maxY, itemStyle.align);
            cy = afterItem + gap;
        }
        return cy + 2;
    }

    // =========================================================================
    // <render>
    // =========================================================================

    private static int renderRender(GuiGraphics gfx, Font font, PmlNode node,
                                    int x, int y, int maxW, int maxY, float pt) {
        int cy = y + node.attrInt("margin-top", 4);
        if (cy >= maxY) return cy;

        String type      = node.attr("type",       "item");
        String id        = node.attr("id",         "");
        int    sz        = node.attrInt("size",     32);
        String label     = node.attr("label",       null);
        String labelSide = node.attr("label-side", "right");
        String bgKey     = node.attr("bg",         "none");
        String borderKey = node.attr("border",     "none");
        String alignStr  = node.attr("align",      "left");

        int rx = switch (alignStr) {
            case "center" -> x + (maxW - sz) / 2;
            case "right"  -> x + maxW - sz;
            default       -> x;
        };

        // Background
        if (!"none".equals(bgKey)) {
            int bgCol = resolveBg(bgKey);
            gfx.fill(rx - 3, cy - 3, rx + sz + 3, cy + sz + 3, bgCol);
        }
        // Border
        if (!"none".equals(borderKey)) {
            int bc = "dim".equals(borderKey) ? BORDER_DIM : BORDER_ACCENT;
            drawBorder(gfx, rx - 3, cy - 3, sz + 6, sz + 6, 1, bc);
        }

        // Content
        try {
            switch (type) {
                case "entity" -> drawEntity(gfx, id, rx, cy, sz, pt);
                case "item"   -> drawItem(gfx, id, rx, cy, sz);
                case "image"  -> drawImage(gfx, id, rx, cy, sz);
                default       -> drawMissing(gfx, font, rx, cy, sz);
            }
        } catch (Exception e) { drawMissing(gfx, font, rx, cy, sz); }

        // Label
        int labelColor = PmlStyle.resolveColor(node.attr("label-color", "text"), 0xFF000000);
        if (label != null && !label.isEmpty()) {
            int lh = font.lineHeight;
            if ("below".equals(labelSide)) {
                gfx.drawString(font, label, rx, cy + sz + 3, labelColor, false);
                cy += sz + lh + 6;
            } else {
                int lx = rx + sz + 6;
                int ly = cy + (sz - lh) / 2;
                int lw = maxW - (lx - x);
                if (lw > 10) {
                    for (var seq : font.split(Component.literal(label), lw)) {
                        gfx.drawString(font, seq, lx, ly, labelColor, false);
                        ly += lh + 1;
                    }
                }
                cy += sz + 4;
            }
        } else {
            cy += sz + 4;
        }

        return cy;
    }

    // =========================================================================
    // <panel>
    // =========================================================================

    private static int renderPanel(GuiGraphics gfx, Font font, PmlNode node,
                                   int x, int y, int maxW, int maxY,
                                   PmlStyle parent, float pt) {
        PmlStyle style    = PmlStyle.fromNode(node, parent);
        int cy            = y + style.marginTop;
        if (cy >= maxY) return cy;

        int     pad       = node.attrInt("padding", 6);
        int     gap       = node.attrInt("gap",     8);
        String  bgKey     = node.attr("bg",         "none");
        String  borderKey = node.attr("border",     "none");
        boolean horiz     = "horizontal".equals(node.attr("direction", "vertical"));
        boolean sharp     = "sharp".equals(node.attr("corner", "round"));

        int innerX = x   + pad;
        int innerW = maxW - pad * 2;
        int innerY = cy  + pad;

        if (horiz) {
            // ── Horizontal child layout ───────────────────────────────────────
            List<PmlNode> kids = node.children();
            int count = Math.max(1, kids.size());
            int share = (innerW - gap * Math.max(0, count - 1)) / count;

            // Probe height for background sizing
            int estH = 0;
            for (PmlNode kid : kids) estH = Math.max(estH, estimateHeight(kid, style));
            drawPanelBg(gfx, x, cy, maxW, estH + pad * 2, bgKey, borderKey, sharp);

            int childX = innerX, realMaxH = 0;
            for (int ki = 0; ki < kids.size(); ki++) {
                int endY = renderNode(gfx, font, kids.get(ki), childX, innerY,
                        share, maxY, style, pt);
                realMaxH = Math.max(realMaxH, endY - innerY);
                childX += share + gap;
            }
            cy = innerY + realMaxH + pad;

        } else {
            // ── Vertical child layout ─────────────────────────────────────────
            // Pass 1: probe height for background
            int estH = 0;
            for (PmlNode kid : node.children()) estH += estimateHeight(kid, style);
            drawPanelBg(gfx, x, cy, maxW, estH + pad * 2, bgKey, borderKey, sharp);

            // Pass 2: real draw
            int ry = innerY;
            for (PmlNode kid : node.children()) {
                if (ry >= maxY) break;
                ry = renderNode(gfx, font, kid, innerX, ry, innerW, maxY, style, pt);
            }
            cy = ry + pad;
        }

        return cy + 2;
    }

    private static void drawPanelBg(GuiGraphics gfx,
                                    int x, int y, int w, int h,
                                    String bgKey, String borderKey, boolean sharp) {
        if (!"none".equals(bgKey))
            gfx.fill(x, y, x + w, y + h, resolveBg(bgKey));
        if (!"none".equals(borderKey)) {
            int bc = "dim".equals(borderKey) ? BORDER_DIM : BORDER_ACCENT;
            if (sharp) drawBorder(gfx, x, y, w, h, 1, bc);
            else       drawRoundedBorder(gfx, x, y, w, h, bc);
        }
    }

    private static int resolveBg(String key) {
        return switch (key) {
            case "dark"   -> BG_DARK;
            case "accent" -> BG_ACCENT;
            default       -> BG_LIGHT;
        };
    }

    // =========================================================================
    // <divider>
    // =========================================================================

    private static int renderDivider(GuiGraphics gfx, PmlNode node,
                                     int x, int y, int maxW, int maxY) {
        int cy  = y + node.attrInt("margin-top", 6);
        if (cy >= maxY) return cy;
        int col = PmlStyle.resolveColor(node.attr("color", "dim"), DIVIDER_COLOR);
        gfx.fill(x, cy, x + maxW, cy + 1, col);
        return cy + 4;
    }

    // =========================================================================
    // HEIGHT ESTIMATION (probe pass — no drawing)
    // =========================================================================

    private static int estimateHeight(PmlNode node, PmlStyle parent) {
        if (node.isTextNode()) return parent.lineHeight() + 3;
        return switch (node.tag == null ? "" : node.tag) {
            case "text", "span" -> {
                PmlStyle s = PmlStyle.fromNode(node, parent);
                yield s.marginTop + s.lineHeight() * 3 + 4;
            }
            case "list" -> {
                int items = node.childrenOfTag("item").size();
                yield node.attrInt("margin-top", 4)
                        + items * (parent.lineHeight() + node.attrInt("gap", 2)) + 6;
            }
            case "render"  -> node.attrInt("margin-top", 4) + node.attrInt("size", 32) + 4;
            case "divider" -> node.attrInt("margin-top", 6) + 4;
            case "panel"   -> {
                int sub = 0;
                for (PmlNode k : node.children()) sub += estimateHeight(k, parent);
                yield node.attrInt("margin-top", 6) + node.attrInt("padding", 6) * 2 + sub + 4;
            }
            default -> 10;
        };
    }

    // =========================================================================
    // ENTITY / ITEM / IMAGE
    // =========================================================================

    private static void drawEntity(GuiGraphics gfx, String entityId,
                                   int x, int y, int sz, float pt) {
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation rl = new ResourceLocation(entityId);
        Optional<EntityType<?>> opt = BuiltInRegistries.ENTITY_TYPE.getOptional(rl);
        if (opt.isEmpty()) { drawMissing(gfx, mc.font, x, y, sz); return; }

        net.minecraft.world.entity.Entity entity = opt.get().create(mc.level);
        if (!(entity instanceof LivingEntity living)) {
            drawMissing(gfx, mc.font, x, y, sz); return;
        }

        float yRot  = (mc.level.getGameTime() + pt) * 2.0f;
        float scale = sz * 0.35f;

        gfx.pose().pushPose();
        gfx.pose().translate(x + sz / 2.0, y + sz * 0.85, 50.0);
        gfx.pose().scale(scale, -scale, scale);
        gfx.pose().mulPose(com.mojang.math.Axis.YP.rotationDegrees(yRot));

        EntityRenderDispatcher disp = mc.getEntityRenderDispatcher();
        disp.setRenderShadow(false);
        net.minecraft.client.renderer.MultiBufferSource.BufferSource bufs =
                mc.renderBuffers().bufferSource();

        RenderSystem.enableDepthTest();
        disp.render(living, 0.0, 0.0, 0.0, yRot, pt, gfx.pose(), bufs,
                net.minecraft.client.renderer.LightTexture.pack(15, 15));
        bufs.endBatch();
        disp.setRenderShadow(true);
        RenderSystem.disableDepthTest();

        gfx.pose().popPose();
    }

    private static void drawItem(GuiGraphics gfx, String itemId, int x, int y, int sz) {
        Minecraft mc = Minecraft.getInstance();
        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(itemId));
        ItemStack stk = new ItemStack(item);
        if (stk.isEmpty()) { drawMissing(gfx, mc.font, x, y, sz); return; }

        float scale = sz / 16.0f;
        gfx.pose().pushPose();
        gfx.pose().translate(x, y, 0);
        gfx.pose().scale(scale, scale, 1.0f);
        gfx.renderItem(stk, 0, 0);
        gfx.pose().popPose();
    }

    private static void drawImage(GuiGraphics gfx, String texStr, int x, int y, int sz) {
        try {
            RenderSystem.enableBlend();
            gfx.blit(new ResourceLocation(texStr), x, y, 0, 0, sz, sz, sz, sz);
            RenderSystem.disableBlend();
        } catch (Exception e) {
            drawMissing(gfx, Minecraft.getInstance().font, x, y, sz);
        }
    }

    private static void drawMissing(GuiGraphics gfx, Font font, int x, int y, int sz) {
        gfx.fill(x, y, x + sz, y + sz, 0xFF442222);
        gfx.drawString(font, "?", x + sz / 2 - 3, y + sz / 2 - 4, 0xFFCCCCCC, false);
    }

    // =========================================================================
    // DRAWING HELPERS
    // =========================================================================

    private static void drawBorder(GuiGraphics gfx, int x, int y, int w, int h,
                                   int t, int col) {
        gfx.fill(x,     y,     x+w,   y+t,   col);
        gfx.fill(x,     y+h-t, x+w,   y+h,   col);
        gfx.fill(x,     y,     x+t,   y+h,   col);
        gfx.fill(x+w-t, y,     x+w,   y+h,   col);
    }

    private static void drawRoundedBorder(GuiGraphics gfx, int x, int y, int w, int h,
                                          int col) {
        int r = 3;
        gfx.fill(x+r,   y,     x+w-r, y+1,   col); // top
        gfx.fill(x+r,   y+h-1, x+w-r, y+h,   col); // bottom
        gfx.fill(x,     y+r,   x+1,   y+h-r, col); // left
        gfx.fill(x+w-1, y+r,   x+w,   y+h-r, col); // right
    }
}