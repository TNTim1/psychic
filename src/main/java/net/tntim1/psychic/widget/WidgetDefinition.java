package net.tntim1.psychic.widget;

import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Defines a single interactive widget placed on a tab's scrollable canvas.
 *
 * <h3>Dependencies</h3>
 * A widget can declare a list of other widget IDs it requires to be unlocked
 * before it can be opened or activated. If any dependency is not unlocked the
 * widget is "locked" — its popup cannot be opened and the ACTIVATE button is
 * disabled.
 *
 * <h3>Task confirmation</h3>
 * When {@link #requiresConfirmation} is {@code true}, a completed task set
 * shows a checkbox the player must tick before the ACTIVATE button becomes
 * available. The checkbox uses the textures defined in
 * {@link #CHECKBOX_UNCHECKED} / {@link #CHECKBOX_HOVERED} / {@link #CHECKBOX_CHECKED}.
 *
 * <h3>Quick-add helpers</h3>
 * <pre>
 *   WidgetDefinition.simple("fireball", icon, 200, 150, "Fireball",
 *       PopupContent.page(
 *           PopupContent.text("Launches a fireball.\nCost: 30 mana"),
 *           PopupContent.entityRender("minecraft:blaze", "Blaze")
 *       ),
 *       null,
 *       TaskRequirement.kill("minecraft:blaze", 5, "Blazes slain")
 *   );
 * </pre>
 */
public class WidgetDefinition {

    // ── Checkbox textures ─────────────────────────────────────────────────────
    // Define your actual resource locations here.
    public static final ResourceLocation CHECKBOX_UNCHECKED =
            new ResourceLocation("psychic", "textures/gui/widgets/checkbox_unchecked.png");
    public static final ResourceLocation CHECKBOX_HOVERED =
            new ResourceLocation("psychic", "textures/gui/widgets/checkbox_hovered.png");
    public static final ResourceLocation CHECKBOX_CHECKED =
            new ResourceLocation("psychic", "textures/gui/widgets/checkbox_checked.png");

    // ── Fields ────────────────────────────────────────────────────────────────

    /** Unique identifier for this widget. Used for unlock tracking and dependency references. */
    public final String id;

    /** Texture rendered as the widget's icon on the canvas. */
    public final ResourceLocation iconTexture;

    /** Position on the scrollable canvas (canvas-pixels, origin centred). */
    public final int canvasX, canvasY;

    /** Icon render size in screen-pixels before zoom is applied. */
    public final int iconW, iconH;

    /** Short label shown as a tooltip on hover and in the popup title. */
    public final String label;

    /** Full popup content (pages + blocks). Never null. */
    public final PopupContent popupContent;

    /**
     * IDs of widgets that must ALL be unlocked before this widget can be
     * opened or activated.
     */
    public final List<String> dependencies;

    /** Task requirements that gate this widget's activation. */
    public final List<TaskRequirement> taskRequirements;

    /**
     * When {@code true} the player must manually tick a confirmation checkbox
     * after all tasks are complete before the ACTIVATE button is enabled.
     */
    public final boolean requiresConfirmation;

    // ── Constructor ───────────────────────────────────────────────────────────

    public WidgetDefinition(String id,
                            ResourceLocation iconTexture,
                            int canvasX, int canvasY,
                            int iconW, int iconH,
                            String label,
                            PopupContent popupContent,
                            List<String> dependencies,
                            List<TaskRequirement> taskRequirements,
                            boolean requiresConfirmation) {
        this.id                  = id;
        this.iconTexture         = iconTexture;
        this.canvasX             = canvasX;
        this.canvasY             = canvasY;
        this.iconW               = iconW;
        this.iconH               = iconH;
        this.label               = label;
        this.popupContent        = popupContent;
        this.dependencies        = Collections.unmodifiableList(dependencies);
        this.taskRequirements    = Collections.unmodifiableList(taskRequirements);
        this.requiresConfirmation = requiresConfirmation;
    }

    // ── Convenience predicates ────────────────────────────────────────────────

    public boolean hasDependencies()  { return !dependencies.isEmpty(); }
    public boolean hasTasks()         { return !taskRequirements.isEmpty(); }

    // ── Primary factory — everything explicit ─────────────────────────────────

    /**
     * Full-control factory.
     *
     * @param requiresConfirmation if true, tasks need a manual checkbox tick before activation
     */
    public static WidgetDefinition simple(String id, ResourceLocation icon,
                                          int x, int y,
                                          String label,
                                          PopupContent content,
                                          String[] depIds,
                                          boolean requiresConfirmation,
                                          TaskRequirement... tasks) {
        return new WidgetDefinition(id, icon, x, y, 24, 24, label, content,
                depIds == null ? Collections.emptyList() : Arrays.asList(depIds),
                Arrays.asList(tasks), requiresConfirmation);
    }

    /** Same as {@link #simple} with {@code requiresConfirmation = false}. */
    public static WidgetDefinition simple(String id, ResourceLocation icon,
                                          int x, int y,
                                          String label,
                                          PopupContent content,
                                          String[] depIds,
                                          TaskRequirement... tasks) {
        return simple(id, icon, x, y, label, content, depIds, false, tasks);
    }

    /** Custom icon size variant. */
    public static WidgetDefinition simple(String id, ResourceLocation icon,
                                          int x, int y,
                                          int iconW, int iconH,
                                          String label,
                                          PopupContent content,
                                          String[] depIds,
                                          boolean requiresConfirmation,
                                          TaskRequirement... tasks) {
        return new WidgetDefinition(id, icon, x, y, iconW, iconH, label, content,
                depIds == null ? Collections.emptyList() : Arrays.asList(depIds),
                Arrays.asList(tasks), requiresConfirmation);
    }

    // ── Legacy-compatible factories (old API, now delegate to new system) ──────

    /** INFO popup (plain text), no deps, no tasks. */
    public static WidgetDefinition info(String id, ResourceLocation icon,
                                        int x, int y,
                                        String label, String text) {
        return simple(id, icon, x, y, label,
                PopupContent.page(PopupContent.text(text)), null);
    }

    /** INFO popup with custom icon size, no deps, no tasks. */
    public static WidgetDefinition info(String id, ResourceLocation icon,
                                        int x, int y,
                                        String label, String text,
                                        int iconW, int iconH) {
        return new WidgetDefinition(id, icon, x, y, iconW, iconH, label,
                PopupContent.page(PopupContent.text(text)),
                Collections.emptyList(), Collections.emptyList(), false);
    }

    /** INFO popup with dependency IDs, no tasks. */
    public static WidgetDefinition info(String id, ResourceLocation icon,
                                        int x, int y,
                                        String label, String text,
                                        String... depIds) {
        return simple(id, icon, x, y, label,
                PopupContent.page(PopupContent.text(text)), depIds);
    }

    /** LIST popup (bulleted), no deps, no tasks. */
    public static WidgetDefinition list(String id, ResourceLocation icon,
                                        int x, int y,
                                        String label, String... entries) {
        return simple(id, icon, x, y, label,
                PopupContent.page(PopupContent.list(entries)), null);
    }

    /** LIST popup with dep IDs, no tasks. */
    public static WidgetDefinition list_dependencies(String id, ResourceLocation icon,
                                                     int x, int y,
                                                     String label,
                                                     String[] depIds,
                                                     String... entries) {
        return simple(id, icon, x, y, label,
                PopupContent.page(PopupContent.list(entries)), depIds);
    }

    /** IMAGE popup, no deps, no tasks. */
    public static WidgetDefinition image(String id, ResourceLocation icon,
                                         int x, int y,
                                         String label, ResourceLocation img) {
        return simple(id, icon, x, y, label,
                PopupContent.page(PopupContent.image(img)), null);
    }

    /**
     * INFO popup with tasks.
     * Auto-generates an ENTITY_RENDER block for each task requirement,
     * appended after the text block.
     */
    public static WidgetDefinition infoWithTasks(String id, ResourceLocation icon,
                                                 int x, int y,
                                                 String label, String text,
                                                 String[] depIds,
                                                 TaskRequirement... tasks) {
        // Build blocks: text + one render block per task
        java.util.List<PopupContent.Block> blocks = new java.util.ArrayList<>();
        blocks.add(PopupContent.text(text));
        for (TaskRequirement t : tasks) blocks.add(PopupContent.renderForTask(t));

        return new WidgetDefinition(id, icon, x, y, 24, 24, label,
                new PopupContent(List.of(new PopupContent.Page(null, blocks))),
                depIds == null ? Collections.emptyList() : Arrays.asList(depIds),
                Arrays.asList(tasks), false);
    }

    /**
     * LIST popup with tasks.
     * Auto-generates render blocks for each task, placed after the list.
     */
    public static WidgetDefinition listWithTasks(String id, ResourceLocation icon,
                                                 int x, int y,
                                                 String label,
                                                 String[] depIds,
                                                 TaskRequirement[] tasks,
                                                 String... entries) {
        java.util.List<PopupContent.Block> blocks = new java.util.ArrayList<>();
        blocks.add(PopupContent.list(entries));
        for (TaskRequirement t : tasks) blocks.add(PopupContent.renderForTask(t));

        return new WidgetDefinition(id, icon, x, y, 24, 24, label,
                new PopupContent(List.of(new PopupContent.Page(null, blocks))),
                depIds == null ? Collections.emptyList() : Arrays.asList(depIds),
                Arrays.asList(tasks), false);
    }
}