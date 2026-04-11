package net.tntim1.psychic.widget;

import net.minecraft.resources.ResourceLocation;

/**
 * Defines a single interactive widget placed on a tab's scrollable canvas.
 *
 * <h3>Quick-add helpers</h3>
 * <pre>
 *   // Text popup
 *   WidgetDefinition.info(icon, canvasX, canvasY, "Label", "Body text here.");
 *
 *   // Bulleted list popup
 *   WidgetDefinition.list(icon, canvasX, canvasY, "Label", "Line 1", "Line 2");
 *
 *   // Image popup
 *   WidgetDefinition.image(icon, canvasX, canvasY, "Label", imageResourceLocation);
 * </pre>
 *
 * <p>Or use the full constructor for fine-grained control over icon size and popup type.
 */
public class WidgetDefinition {

    /** Texture rendered as the widget's icon on the canvas. */
    public final ResourceLocation iconTexture;

    /** Position on the scrollable canvas (canvas-pixels, origin top-left). */
    public final int canvasX, canvasY;

    /** Icon render size in screen-pixels before zoom is applied. */
    public final int iconW, iconH;

    /** Short label shown as a tooltip on hover. */
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
    public final String id;


    public WidgetDefinition(String id, ResourceLocation iconTexture,
                            int canvasX, int canvasY,
                            int iconW, int iconH,
                            String label,
                            PopupType popupType,
                            String popupData) {
        this.id=id;
        this.iconTexture = iconTexture;
        this.canvasX    = canvasX;
        this.canvasY    = canvasY;
        this.iconW      = iconW;
        this.iconH      = iconH;
        this.label      = label;
        this.popupType  = popupType;
        this.popupData  = popupData;
    }

    // ── convenience factories ─────────────────────────────────────────────────

    public static WidgetDefinition info(String id, ResourceLocation icon, int x, int y, String label, String text) {
        return new WidgetDefinition(id, icon, x, y, 24, 24, label, PopupType.INFO, text);
    }

    public static WidgetDefinition list(String id,ResourceLocation icon, int x, int y, String label, String... entries) {
        return new WidgetDefinition(id, icon, x, y, 24, 24, label, PopupType.LIST, String.join("\n", entries));
    }

    public static WidgetDefinition image(String id,ResourceLocation icon, int x, int y, String label, ResourceLocation img) {
        return new WidgetDefinition(id, icon, x, y, 24, 24, label, PopupType.IMAGE, img.toString());
    }
}
