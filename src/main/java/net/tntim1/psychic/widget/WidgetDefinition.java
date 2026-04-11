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
 * before it can be opened or activated. If any dependency is not unlocked, the
 * widget is "locked" — its popup cannot be opened and the ACTIVATE button is
 * disabled. When a dependency is re-locked (cascaded from above), this widget
 * is automatically locked too.
 *
 * <h3>Quick-add helpers</h3>
 * <pre>
 *   // No dependencies
 *   WidgetDefinition.info(id, icon, x, y, "Label", "Body text.");
 *
 *   // With dependencies
 *   WidgetDefinition.info(id, icon, x, y, "Label", "Body text.", "dep_id_1", "dep_id_2");
 *
 *   // Bulleted list popup
 *   WidgetDefinition.list(id, icon, x, y, "Label", new String[]{"dep1"}, "Line 1", "Line 2");
 * </pre>
 */
public class WidgetDefinition {

    /** Unique identifier for this widget. Used for unlock tracking and dependency references. */
    public final String id;

    /** Texture rendered as the widget's icon on the canvas. */
    public final ResourceLocation iconTexture;

    /** Position on the scrollable canvas (canvas-pixels, origin top-left). */
    public final int canvasX, canvasY;

    /** Icon render size in screen-pixels before zoom is applied. */
    public final int iconW, iconH;

    /** Short label shown as a tooltip on hover and in the popup title. */
    public final String label;

    /** Which popup style to open on click. */
    public final PopupType popupType;

    /**
     * Data passed to the popup renderer. Meaning depends on {@link #popupType}:
     * <ul>
     *   <li>INFO   – plain text body (use \n for line breaks)</li>
     *   <li>LIST   – newline-separated bullet entries</li>
     *   <li>IMAGE  – ResourceLocation string, e.g. "psychic:textures/gui/map.png"</li>
     *   <li>CUSTOM – identifier string for a custom renderer</li>
     * </ul>
     */
    public final String popupData;

    /**
     * IDs of widgets that must ALL be unlocked before this widget can be
     * opened or activated. An empty list means no prerequisites.
     */
    public final List<String> dependencies;

    // ── full constructor ──────────────────────────────────────────────────────

    public WidgetDefinition(String id,
                            ResourceLocation iconTexture,
                            int canvasX, int canvasY,
                            int iconW, int iconH,
                            String label,
                            PopupType popupType,
                            String popupData,
                            List<String> dependencies) {
        this.id           = id;
        this.iconTexture  = iconTexture;
        this.canvasX      = canvasX;
        this.canvasY      = canvasY;
        this.iconW        = iconW;
        this.iconH        = iconH;
        this.label        = label;
        this.popupType    = popupType;
        this.popupData    = popupData;
        this.dependencies = Collections.unmodifiableList(dependencies);
    }

    // ── convenience: check if deps list is empty ──────────────────────────────

    public boolean hasDependencies() {
        return !dependencies.isEmpty();
    }

    // ── factory helpers ───────────────────────────────────────────────────────

    /**
     * INFO popup, no dependencies.
     */
    public static WidgetDefinition info(String id, ResourceLocation icon,
                                        int x, int y,
                                        String label, String text) {
        return new WidgetDefinition(id, icon, x, y, 24, 24,
                label, PopupType.INFO, text,
                Collections.emptyList());
    }

    /**
     * INFO popup with dependencies.
     *
     * @param depIds IDs of widgets that must be unlocked first
     */
    public static WidgetDefinition info(String id, ResourceLocation icon,
                                        int x, int y,
                                        String label, String text,
                                        String... depIds) {
        return new WidgetDefinition(id, icon, x, y, 24, 24,
                label, PopupType.INFO, text,
                Arrays.asList(depIds));
    }

    /**
     * LIST popup, no dependencies.
     */
    public static WidgetDefinition list(String id, ResourceLocation icon,
                                        int x, int y,
                                        String label, String... entries) {
        return new WidgetDefinition(id, icon, x, y, 24, 24,
                label, PopupType.LIST, String.join("\n", entries),
                Collections.emptyList());
    }

    /**
     * LIST popup with dependencies.
     *
     * @param depIds  IDs of widgets that must be unlocked first (null = none)
     * @param entries bullet lines for the popup body
     */
    public static WidgetDefinition list_dependencies(String id, ResourceLocation icon,
                                        int x, int y,
                                        String label,
                                        String[] depIds,
                                        String... entries) {
        return new WidgetDefinition(id, icon, x, y, 24, 24,
                label, PopupType.LIST, String.join("\n", entries),
                depIds == null ? Collections.emptyList() : Arrays.asList(depIds));
    }

    /**
     * IMAGE popup, no dependencies.
     */
    public static WidgetDefinition image(String id, ResourceLocation icon,
                                         int x, int y,
                                         String label, ResourceLocation img) {
        return new WidgetDefinition(id, icon, x, y, 24, 24,
                label, PopupType.IMAGE, img.toString(),
                Collections.emptyList());
    }

    /**
     * IMAGE popup with dependencies.
     */
    public static WidgetDefinition image(String id, ResourceLocation icon,
                                         int x, int y,
                                         String label, ResourceLocation img,
                                         String... depIds) {
        return new WidgetDefinition(id, icon, x, y, 24, 24,
                label, PopupType.IMAGE, img.toString(),
                Arrays.asList(depIds));
    }
}