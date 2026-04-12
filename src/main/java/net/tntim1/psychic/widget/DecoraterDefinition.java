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
public class DecoraterDefinition {

    /** Texture rendered as the widget's icon on the canvas. */
    public final ResourceLocation iconTexture;

    /** Position on the scrollable canvas (canvas-pixels, origin top-left). */
    public final int canvasX, canvasY;

    /** Icon render size in screen-pixels before zoom is applied. */
    public final int iconW, iconH;

// ── Updated constructor (add taskRequirements parameter at the end) ───────────

    public DecoraterDefinition(
                            ResourceLocation iconTexture,
                            int canvasX, int canvasY,
                            int iconW, int iconH) {
        this.iconTexture      = iconTexture;
        this.canvasX          = canvasX;
        this.canvasY          = canvasY;
        this.iconW            = iconW;
        this.iconH            = iconH;
    }


}