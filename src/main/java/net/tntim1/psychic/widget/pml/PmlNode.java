package net.tntim1.psychic.widget.pml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A single node in a parsed PML tree.
 *
 * <p>Each node is one of:
 * <ul>
 *   <li><b>Element</b>  — {@code tag != null}. Has attributes and zero or more children.</li>
 *   <li><b>Text run</b> — {@code tag == null}. Raw text content between tags.</li>
 * </ul>
 *
 * <p>Children are ordered and may freely mix element nodes and text-run nodes,
 * which lets the renderer handle inline {@code <span>} correctly inside a
 * {@code <text>} parent.
 */
public final class PmlNode {

    // ── Identity ──────────────────────────────────────────────────────────────

    /** Tag name in lower-case, e.g. {@code "text"}, {@code "panel"}, {@code "render"}.
     *  {@code null} for a raw-text node. */
    public final String tag;

    /** Raw text content. Non-null only for raw-text nodes ({@code tag == null}). */
    public final String text;

    // ── Attributes ────────────────────────────────────────────────────────────

    private final Map<String, String> attrs;

    // ── Children ──────────────────────────────────────────────────────────────

    private final List<PmlNode> children;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Element node. */
    PmlNode(String tag, Map<String, String> attrs, List<PmlNode> children) {
        this.tag      = tag;
        this.text     = null;
        this.attrs    = Collections.unmodifiableMap(attrs);
        this.children = Collections.unmodifiableList(children);
    }

    /** Raw text node. */
    PmlNode(String text) {
        this.tag      = null;
        this.text     = text;
        this.attrs    = Collections.emptyMap();
        this.children = Collections.emptyList();
    }

    // ── Attribute helpers ─────────────────────────────────────────────────────

    public boolean isElement()  { return tag  != null; }
    public boolean isTextNode() { return text != null; }

    /** Returns the attribute value, or {@code defaultValue} if absent. */
    public String attr(String name, String defaultValue) {
        return attrs.getOrDefault(name, defaultValue);
    }

    /** Returns {@code true} when a flag attribute is present (value is irrelevant). */
    public boolean flag(String name) {
        return attrs.containsKey(name);
    }

    /** Returns the attribute as an int, or {@code defaultValue} if absent / not parseable. */
    public int attrInt(String name, int defaultValue) {
        String v = attrs.get(name);
        if (v == null) return defaultValue;
        try { return Integer.parseInt(v.trim()); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    public List<PmlNode> children() { return children; }

    /** Convenience: collect only immediate element children with a specific tag. */
    public List<PmlNode> childrenOfTag(String tagName) {
        List<PmlNode> out = new ArrayList<>();
        for (PmlNode c : children) if (tagName.equals(c.tag)) out.add(c);
        return out;
    }

    @Override
    public String toString() {
        if (tag == null) return "[text: \"" + text + "\"]";
        return "<" + tag + " " + attrs + ">";
    }
}