package net.tntim1.psychic.widget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Defines one tab in the Atlas Codex GUI.
 *
 * <pre>
 *   TabDefinition tab = new TabDefinition("Spells", 2048, 1024)
 *       .addWidget(WidgetDefinition.info(...))
 *       .addWidget(WidgetDefinition.list(...));
 * </pre>
 */
public class TabDefinition {

    /** Text shown on the tab button. */
    public final String name;

    /** Total scrollable canvas dimensions in canvas-pixels. */
    public final int canvasWidth, canvasHeight;

    public final List<WidgetDefinition> widgets = new ArrayList<>();

    public TabDefinition(String name, int canvasWidth, int canvasHeight) {
        this.name         = name;
        this.canvasWidth  = canvasWidth;
        this.canvasHeight = canvasHeight;
    }

    public TabDefinition addWidget(WidgetDefinition widget) {
        widgets.add(widget);
        return this;
    }

    public TabDefinition addWidgets(WidgetDefinition... defs) {
        widgets.addAll(Arrays.asList(defs));
        return this;
    }
}
