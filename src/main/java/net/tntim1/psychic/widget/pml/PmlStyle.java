package net.tntim1.psychic.widget.pml;

import net.minecraft.util.Mth;

/**
 * Immutable resolved style applied to a text run or block element.
 *
 * <p>Build via {@link #DEFAULT} and the {@code with*()} builder methods.
 */
public final class PmlStyle {

    // ── Font size constants ───────────────────────────────────────────────────

    /** Font size tokens. The renderer maps these to actual pixel heights. */
    public enum Size { SM, MD, LG, XL }

    // ── Named palette ─────────────────────────────────────────────────────────

    /**
     * Resolves a named palette key or raw {@code #RRGGBB} / {@code #AARRGGBB}
     * colour string to an ARGB int.  Falls back to {@code fallback} on failure.
     */
    public static int resolveColor(String key, int fallback) {
        if (key == null || key.isBlank()) return fallback;
        return switch (key.toLowerCase().trim()) {
            case "accent"  -> 0xFF392420;
            case "border"  -> 0xFFd4a9a2;
            case "dim"     -> 0xFF888888;
            case "text"    -> 0xFF000000;
            case "mana"    -> 0xFF4466CC;
            case "red"     -> 0xFFCC3333;
            case "gold"    -> 0xFFFFCC44;
            case "green"   -> 0xFF44AA44;
            case "dark"    -> 0xFF1a1208;
            case "white"   -> 0xFFFFFFFF;
            case "purple"  -> 0xFF9933CC;
            case "orange"  -> 0xFFDD6622;
            case "cyan"    -> 0xFF22BBCC;
            default        -> parseHex(key, fallback);
        };
    }

    private static int parseHex(String key, int fallback) {
        String s = key.startsWith("#") ? key.substring(1) : key;
        try {
            long v = Long.parseLong(s, 16);
            return (int)(s.length() <= 6 ? (0xFF000000L | v) : v);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    public final Size    size;
    public final int     color;       // ARGB
    public final boolean bold;
    public final boolean italic;
    public final Align   align;
    public final int     marginTop;   // pixels above this block

    public enum Align { LEFT, CENTER, RIGHT }

    // ── Singleton default ─────────────────────────────────────────────────────

    public static final PmlStyle DEFAULT = new PmlStyle(
            Size.MD, 0xFF000000, false, false, Align.LEFT, 2
    );

    // ── Constructor ───────────────────────────────────────────────────────────

    public PmlStyle(Size size, int color, boolean bold, boolean italic,
                    Align align, int marginTop) {
        this.size      = size;
        this.color     = color;
        this.bold      = bold;
        this.italic    = italic;
        this.align     = align;
        this.marginTop = marginTop;
    }

    // ── Builder helpers ───────────────────────────────────────────────────────

    /** Parse all style attributes from a {@link PmlNode} starting from DEFAULT. */
    public static PmlStyle fromNode(PmlNode node) {
        return fromNode(node, DEFAULT);
    }

    /** Parse style attributes from a node, inheriting from {@code parent}. */
    public static PmlStyle fromNode(PmlNode node, PmlStyle parent) {
        Size    size      = parseSize(node.attr("size", null), parent.size);
        int     color     = resolveColor(node.attr("color", null), parent.color);
        boolean bold      = node.flag("bold")   || parent.bold;
        boolean italic    = node.flag("italic") || parent.italic;
        Align   align     = parseAlign(node.attr("align", null), parent.align);
        int     marginTop = node.attrInt("margin-top", parent.marginTop);
        return new PmlStyle(size, color, bold, italic, align, marginTop);
    }

    /** Parse a child/inline node that should NOT inherit margin-top from parent. */
    public static PmlStyle fromInlineNode(PmlNode node, PmlStyle parent) {
        Size    size   = parseSize(node.attr("size", null), parent.size);
        int     color  = resolveColor(node.attr("color", null), parent.color);
        boolean bold   = node.flag("bold")   || parent.bold;
        boolean italic = node.flag("italic") || parent.italic;
        Align   align  = parseAlign(node.attr("align", null), parent.align);
        return new PmlStyle(size, color, bold, italic, align, 0);
    }

    // ── Font size px mapping ──────────────────────────────────────────────────

    /**
     * Minecraft's font renders at a fixed internal size. We fake size variation
     * by scaling the GuiGraphics pose matrix.  Values here are scale multipliers
     * relative to the base 1× scale (which renders at ~8px cap-height).
     */
    public float scaleFactor() {
        return switch (size) {
            case SM -> 0.75f;
            case MD -> 1.0f;
            case LG -> 1.5f;
            case XL -> 2.0f;
        };
    }

    /** Line height in pixels at this scale (Minecraft font is 9px per line). */
    public int lineHeight() {
        return Math.round(9 * scaleFactor());
    }

    // ── Parsing helpers ───────────────────────────────────────────────────────

    private static Size parseSize(String v, Size fallback) {
        if (v == null) return fallback;
        return switch (v.toLowerCase().trim()) {
            case "sm", "small"  -> Size.SM;
            case "md", "medium" -> Size.MD;
            case "lg", "large"  -> Size.LG;
            case "xl"           -> Size.XL;
            default             -> {
                // Numeric px values: map to nearest token
                try {
                    int px = Integer.parseInt(v.trim());
                    yield px < 8 ? Size.SM : px < 12 ? Size.MD : px < 18 ? Size.LG : Size.XL;
                } catch (NumberFormatException e) { yield fallback; }
            }
        };
    }

    private static Align parseAlign(String v, Align fallback) {
        if (v == null) return fallback;
        return switch (v.toLowerCase().trim()) {
            case "center", "centre" -> Align.CENTER;
            case "right"            -> Align.RIGHT;
            case "left"             -> Align.LEFT;
            default                 -> fallback;
        };
    }
}