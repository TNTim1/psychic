package net.tntim1.psychic.widget;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Describes the full content of a widget's popup.
 *
 * <p>A popup is made up of one or more {@link Page}s. Each page holds an
 * ordered list of {@link Block}s — self-contained content units that the
 * popup renderer draws top-to-bottom. Pages are navigated with Prev / Next
 * arrows in the popup header.
 *
 * <h3>Quick-build examples</h3>
 * <pre>
 *   // Single info page
 *   PopupContent.page(
 *       PopupContent.text("Fireball launches a...\nCost: 30 mana"),
 *       PopupContent.entityRender("minecraft:blaze", "Blaze")
 *   )
 *
 *   // Two pages
 *   PopupContent.pages(
 *       List.of(PopupContent.text("Overview text")),
 *       List.of(PopupContent.text("Details"), PopupContent.image("psychic:textures/..."))
 *   )
 * </pre>
 */
public class PopupContent {

    // ─────────────────────────────────────────────────────────────────────────
    // Block — one unit of rendered content inside a page
    // ─────────────────────────────────────────────────────────────────────────

    public enum BlockType {
        /** Scrollable plain text. {@code data} = raw string, "\n" = line break. */
        TEXT,
        /** Bulleted list. {@code data} = newline-separated entries. */
        LIST,
        /**
         * A rendered item/mob sprite with an optional label shown on hover.
         * {@code resourceId} = registry name, e.g. "minecraft:blaze".
         * {@code data} = display name shown on hover.
         * {@code isEntity} = true → mob model, false → item sprite.
         */
        ENTITY_RENDER,
        /** A full-width texture image. {@code data} = ResourceLocation string. */
        IMAGE,
    }

    public static final class Block {
        public final BlockType type;
        /** Primary string payload — see {@link BlockType} for per-type meaning. */
        public final String data;
        /** Registry ID used for ENTITY_RENDER. */
        public final String resourceId;
        /** True → render as entity model; false → render as item sprite. */
        public final boolean isEntity;

        private Block(BlockType type, String data, String resourceId, boolean isEntity) {
            this.type       = type;
            this.data       = data;
            this.resourceId = resourceId;
            this.isEntity   = isEntity;
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
     * Entity/mob render block. Shows a spinning entity model.
     *
     * @param entityId registry name, e.g. {@code "minecraft:blaze"}
     * @param label    hover tooltip text, e.g. {@code "Blaze"}
     */
    public static Block entityRender(String entityId, String label) {
        return new Block(BlockType.ENTITY_RENDER, label, entityId, true);
    }

    /**
     * Item sprite render block. Shows the item's 2-D sprite.
     *
     * @param itemId registry name, e.g. {@code "minecraft:blaze_rod"}
     * @param label  hover tooltip text, e.g. {@code "Blaze Rod"}
     */
    public static Block itemRender(String itemId, String label) {
        return new Block(BlockType.ENTITY_RENDER, label, itemId, false);
    }

    /** Full-width image block. */
    public static Block image(ResourceLocation texture) {
        return new Block(BlockType.IMAGE, texture.toString(), null, false);
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