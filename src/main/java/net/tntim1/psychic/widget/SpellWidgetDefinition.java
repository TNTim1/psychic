package net.tntim1.psychic.widget;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;

public class SpellWidgetDefinition extends WidgetDefinition {
    public final List<Integer> pattern;
    public final int size;

    public SpellWidgetDefinition(String id, String title, String description, List<Integer> pattern, ResourceLocation texture, int size) {
        super(
                id,
                texture,

                0, 0,
                size, size,
                title,
                PopupContent.page(PopupContent.text(description)),
                Collections.emptyList(),
                // We add a dummy task or set requiresConfirmation to true if you want the button
                Collections.emptyList(),
                true // Set this to true so the "Activate" button appears in the popup
        );
        this.pattern = pattern;
        this.size = size;
    }
}