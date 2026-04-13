package net.tntim1.psychic.widget;

import net.minecraft.resources.ResourceLocation;
import net.tntim1.psychic.widget.pml.PmlParser;
import net.tntim1.psychic.widget.pml.PmlNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Describes the full content of a widget's popup.
 *
 * <p>This is the updated version of PopupContent.  The only addition is the
 * {@link BlockType#PML_SOURCE} variant and the {@link #pml(String)} factory.
 * All existing factories and types are unchanged.
 *
 * <h3>PML quick-start</h3>
 * <pre>
 *   PopupContent.page(
 *       PopupContent.pml("""
 *           &lt;text size="lg" bold color="gold"&gt;Fireball&lt;/text&gt;
 *           &lt;text margin-top="4"&gt;
 *             Launches a concentrated fireball. Cost: &lt;span color="mana"&gt;30 mana&lt;/span&gt;
 *           &lt;/text&gt;
 *           &lt;panel bg="dark" border="accent" margin-top="8" padding="6"&gt;
 *             &lt;text size="sm" color="dim"&gt;Stats&lt;/text&gt;
 *             &lt;list bullet="dash"&gt;
 *               &lt;item&gt;Damage: &lt;span color="red" bold&gt;8 hearts&lt;/span&gt;&lt;/item&gt;
 *               &lt;item&gt;Cooldown: 2s&lt;/item&gt;
 *             &lt;/list&gt;
 *           &lt;/panel&gt;
 *           &lt;render type="entity" id="minecraft:blaze" size="48"
 *                   label="Blaze — slay 5" label-side="right"
 *                   bg="dark" border="accent" margin-top="10"/&gt;
 *       """)
 *   )
 * </pre>
 */
public class PopupContent {

    // ─────────────────────────────────────────────────────────────────────────
    // Block
    // ─────────────────────────────────────────────────────────────────────────

    public enum BlockType {
        /** Scrollable plain text. {@code data} = raw string, "\n" = line break. */
        TEXT,
        /** Bulleted list. {@code data} = newline-separated entries. */
        LIST,
        /**
         * A rendered item/mob sprite with an optional label shown on hover.
         * {@code resourceId} = registry name.  {@code isEntity} selects model vs sprite.
         */
        ENTITY_RENDER,
        /** A full-width texture image. {@code data} = ResourceLocation string. */
        IMAGE,
        /**
         * A PML markup source string.  The renderer parses and draws it via
         * {@link net.tntim1.psychic.widget.pml.PmlRenderer}.
         * {@code data} = raw PML text.
         */
        PML_SOURCE,
    }

    public static final class Block {
        public final BlockType type;
        /** Primary string payload — see {@link BlockType} for per-type meaning. */
        public final String data;
        /** Registry ID used for ENTITY_RENDER. */
        public final String resourceId;
        /** True → render as entity model; false → render as item sprite. */
        public final boolean isEntity;

        /**
         * Cached parse result for PML_SOURCE blocks.  Populated lazily on first
         * render call; {@code null} until then.
         */
        private volatile List<PmlNode> _pmlCache;

        private Block(BlockType type, String data, String resourceId, boolean isEntity) {
            this.type       = type;
            this.data       = data;
            this.resourceId = resourceId;
            this.isEntity   = isEntity;
        }

        /**
         * Returns the cached (or freshly parsed) PML node tree for a
         * {@link BlockType#PML_SOURCE} block.  Calling this on any other block
         * type returns an empty list.
         */
        public List<PmlNode> pmlNodes() {
            if (type != BlockType.PML_SOURCE) return List.of();
            if (_pmlCache == null) {
                synchronized (this) {
                    if (_pmlCache == null) _pmlCache = PmlParser.parse(data);
                }
            }
            return _pmlCache;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Block factories
    // ─────────────────────────────────────────────────────────────────────────

    /** Plain text block. Use "\n" for explicit line breaks. */
    public static Block text(String content) {
        return new Block(BlockType.TEXT, content, null, false);
    }

    /** Bulleted list block. Each entry is one bullet point. */
    public static Block list(String... entries) {
        return new Block(BlockType.LIST, String.join("\n", entries), null, false);
    }

    /**
     * Entity/mob render block.
     *
     * @param entityId registry name, e.g. {@code "minecraft:blaze"}
     * @param label    hover tooltip text
     */
    public static Block entityRender(String entityId, String label) {
        return new Block(BlockType.ENTITY_RENDER, label, entityId, true);
    }

    /**
     * Item sprite render block.
     *
     * @param itemId registry name, e.g. {@code "minecraft:blaze_rod"}
     * @param label  hover tooltip text
     */
    public static Block itemRender(String itemId, String label) {
        return new Block(BlockType.ENTITY_RENDER, label, itemId, false);
    }

    /** Full-width image block. */
    public static Block image(ResourceLocation texture) {
        return new Block(BlockType.IMAGE, texture.toString(), null, false);
    }

    /**
     * PML markup block.
     *
     * <p>The PML string is parsed lazily on first render.  Use Java text blocks
     * ({@code """..."""}) for multi-line PML to keep the source readable.
     *
     * @param pmlSource raw PML markup
     */
    public static Block pml(String pmlSource) {
        return new Block(BlockType.PML_SOURCE, pmlSource, null, false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Page
    // ─────────────────────────────────────────────────────────────────────────

    public static final class Page {
        /** Optional short page title shown in the popup header (may be null). */
        public final String title;
        public final List<Block> blocks;

        public Page(String title, List<Block> blocks) {
            this.title  = title;
            this.blocks = Collections.unmodifiableList(blocks);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PopupContent itself
    // ─────────────────────────────────────────────────────────────────────────

    public final List<Page> pages;

    PopupContent(List<Page> pages) {
        this.pages = Collections.unmodifiableList(pages);
    }

    public int pageCount() { return pages.size(); }

    // ─────────────────────────────────────────────────────────────────────────
    // Top-level factories
    // ─────────────────────────────────────────────────────────────────────────

    /** Single untitled page with one or more blocks. */
    public static PopupContent page(Block... blocks) {
        return new PopupContent(List.of(new Page(null, Arrays.asList(blocks))));
    }

    /** Single page with an explicit title and one or more blocks. */
    public static PopupContent titledPage(String title, Block... blocks) {
        return new PopupContent(List.of(new Page(title, Arrays.asList(blocks))));
    }

    /** Multiple pages, each supplied as a {@code List<Block>}. */
    @SafeVarargs
    public static PopupContent pages(List<Block>... pageBlocks) {
        List<Page> ps = new ArrayList<>();
        for (List<Block> bl : pageBlocks) ps.add(new Page(null, bl));
        return new PopupContent(ps);
    }

    /** Multiple titled pages. Titles and block-lists must correspond 1-to-1. */
    public static PopupContent titledPages(String[] titles, List<Block>[] pageBlocks) {
        if (titles.length != pageBlocks.length)
            throw new IllegalArgumentException("title/page count mismatch");
        List<Page> ps = new ArrayList<>();
        for (int i = 0; i < titles.length; i++)
            ps.add(new Page(titles[i], pageBlocks[i]));
        return new PopupContent(ps);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Convenience: auto-build task renders
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates an ENTITY_RENDER or itemRender block for a {@link TaskRequirement}
     * automatically, using the task's {@code targetId} and {@code label}.
     */
    public static Block renderForTask(TaskRequirement req) {
        return new Block(
                BlockType.ENTITY_RENDER,
                req.label,
                req.targetId,
                req.type == TaskRequirement.Type.KILL
        );
    }
}