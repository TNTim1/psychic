package net.tntim1.psychic.widget.pml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hand-rolled recursive-descent parser for PML (Popup Markup Language).
 *
 * <p>PML is a small XML-like language:
 * <pre>
 *   &lt;tag attr="value" flag&gt;content or child tags&lt;/tag&gt;
 *   &lt;self-closing attr="val"/&gt;
 * </pre>
 *
 * <p>Differences from full XML:
 * <ul>
 *   <li>No processing instructions, DTDs, or namespaces.</li>
 *   <li>Attribute values may use single {@code '} or double {@code "} quotes.</li>
 *   <li>Flag attributes (no {@code =value}) are recorded with an empty string value.</li>
 *   <li>Text nodes are trimmed but internal whitespace is collapsed to a single space
 *       to ease multi-line Java string literals.</li>
 *   <li>HTML entities {@code &amp;} {@code &lt;} {@code &gt;} {@code &quot;} {@code &#NNN;}
 *       are decoded.</li>
 *   <li>Malformed input produces a best-effort result rather than throwing; a
 *       single ERROR text node is emitted when the parser gives up.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 *   List&lt;PmlNode&gt; roots = PmlParser.parse(pmlString);
 * </pre>
 */
public final class PmlParser {

    // ── Public entry point ────────────────────────────────────────────────────

    /**
     * Parses {@code source} and returns a list of top-level {@link PmlNode}s.
     * Never returns {@code null}; returns an empty list for blank input.
     */
    public static List<PmlNode> parse(String source) {
        if (source == null || source.isBlank()) return List.of();
        try {
            return new PmlParser(source).parseChildren(null);
        } catch (Exception e) {
            // Graceful degradation: surface the error as visible text in the popup
            List<PmlNode> err = new ArrayList<>();
            err.add(new PmlNode("[PML error: " + e.getMessage() + "]"));
            return err;
        }
    }

    // ── Parser state ──────────────────────────────────────────────────────────

    private final String src;
    private int pos;

    private PmlParser(String src) {
        this.src = src;
        this.pos = 0;
    }

    // ── Core: parse children until closing tag or EOF ─────────────────────────

    /**
     * Reads nodes until {@code </parentTag>} or end of input.
     * Pass {@code null} as {@code parentTag} to parse at the root level.
     */
    private List<PmlNode> parseChildren(String parentTag) {
        List<PmlNode> out = new ArrayList<>();
        while (pos < src.length()) {
            skipWhitespace();
            if (pos >= src.length()) break;

            if (src.charAt(pos) == '<') {
                if (lookingAt("<!--")) {
                    skipComment();
                    continue;
                }
                if (lookingAt("</")) {
                    // Closing tag — consume and return
                    pos += 2; // skip </
                    String closingTag = readName();
                    skipWhitespace();
                    if (pos < src.length() && src.charAt(pos) == '>') pos++;
                    if (parentTag != null && !closingTag.equals(parentTag)) {
                        // Mismatched tag — put the position back and let the caller handle it
                        // by returning what we have; the parent will see the mismatch.
                    }
                    return out;
                }
                // Opening tag
                PmlNode node = parseElement();
                if (node != null) out.add(node);
            } else {
                // Raw text content
                String text = readText();
                if (!text.isEmpty()) out.add(new PmlNode(text));
            }
        }
        return out;
    }

    // ── Element parsing ───────────────────────────────────────────────────────

    private PmlNode parseElement() {
        if (pos >= src.length() || src.charAt(pos) != '<') return null;
        pos++; // skip <

        String tag = readName().toLowerCase();
        if (tag.isEmpty()) return null;

        Map<String, String> attrs = new HashMap<>();
        skipWhitespace();

        // Attributes
        while (pos < src.length() && src.charAt(pos) != '>' && src.charAt(pos) != '/') {
            String name = readName().toLowerCase();
            if (name.isEmpty()) { pos++; continue; } // skip garbage char
            skipWhitespace();
            if (pos < src.length() && src.charAt(pos) == '=') {
                pos++; // skip =
                skipWhitespace();
                String value = readAttributeValue();
                attrs.put(name, value);
            } else {
                // Flag attribute (no value)
                attrs.put(name, "");
            }
            skipWhitespace();
        }

        // Self-closing?
        if (pos < src.length() && src.charAt(pos) == '/') {
            pos++; // skip /
            if (pos < src.length() && src.charAt(pos) == '>') pos++; // skip >
            return new PmlNode(tag, attrs, List.of());
        }

        if (pos < src.length() && src.charAt(pos) == '>') pos++; // skip >

        // Children
        List<PmlNode> children = parseChildren(tag);
        return new PmlNode(tag, attrs, children);
    }

    // ── Attribute value ───────────────────────────────────────────────────────

    private String readAttributeValue() {
        if (pos >= src.length()) return "";
        char quote = src.charAt(pos);
        if (quote == '"' || quote == '\'') {
            pos++;
            StringBuilder sb = new StringBuilder();
            while (pos < src.length() && src.charAt(pos) != quote) {
                sb.append(src.charAt(pos++));
            }
            if (pos < src.length()) pos++; // closing quote
            return decodeEntities(sb.toString());
        }
        // Unquoted — read until whitespace or >
        StringBuilder sb = new StringBuilder();
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isWhitespace(c) || c == '>' || c == '/') break;
            sb.append(c); pos++;
        }
        return decodeEntities(sb.toString());
    }

    // ── Text content ──────────────────────────────────────────────────────────

    private String readText() {
        StringBuilder sb = new StringBuilder();
        while (pos < src.length() && src.charAt(pos) != '<') {
            sb.append(src.charAt(pos++));
        }
        // Collapse internal whitespace, trim leading/trailing
        String raw = sb.toString();
        // Collapse runs of whitespace (including newlines) to single space
        String collapsed = raw.replaceAll("[ \t\r\n]+", " ").trim();
        return decodeEntities(collapsed);
    }

    // ── Name (tag / attribute name) ───────────────────────────────────────────

    private String readName() {
        StringBuilder sb = new StringBuilder();
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == ':') {
                sb.append(c); pos++;
            } else break;
        }
        return sb.toString();
    }

    // ── Comment ───────────────────────────────────────────────────────────────

    private void skipComment() {
        // consume <!-- ... -->
        pos += 4; // skip <!--
        while (pos + 2 < src.length()) {
            if (src.charAt(pos) == '-' && src.charAt(pos+1) == '-' && src.charAt(pos+2) == '>') {
                pos += 3; return;
            }
            pos++;
        }
        pos = src.length(); // malformed — skip to end
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
    }

    private boolean lookingAt(String prefix) {
        return src.startsWith(prefix, pos);
    }

    // ── Entity decoding ───────────────────────────────────────────────────────

    private static String decodeEntities(String s) {
        if (!s.contains("&")) return s;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            if (s.charAt(i) == '&') {
                int semi = s.indexOf(';', i);
                if (semi < 0) { sb.append('&'); i++; continue; }
                String entity = s.substring(i+1, semi);
                switch (entity) {
                    case "amp"  -> sb.append('&');
                    case "lt"   -> sb.append('<');
                    case "gt"   -> sb.append('>');
                    case "quot" -> sb.append('"');
                    case "apos" -> sb.append('\'');
                    case "nbsp" -> sb.append('\u00a0');
                    default -> {
                        if (entity.startsWith("#x") || entity.startsWith("#X")) {
                            try { sb.append((char) Integer.parseInt(entity.substring(2), 16)); }
                            catch (NumberFormatException e) { sb.append('&').append(entity).append(';'); }
                        } else if (entity.startsWith("#")) {
                            try { sb.append((char) Integer.parseInt(entity.substring(1))); }
                            catch (NumberFormatException e) { sb.append('&').append(entity).append(';'); }
                        } else {
                            sb.append('&').append(entity).append(';');
                        }
                    }
                }
                i = semi + 1;
            } else {
                sb.append(s.charAt(i++));
            }
        }
        return sb.toString();
    }
}