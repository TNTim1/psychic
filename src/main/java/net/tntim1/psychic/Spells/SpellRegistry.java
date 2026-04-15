package net.tntim1.psychic.Spells;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpellRegistry {
    public static final Map<String, SpellDefinition> SPELLS = new HashMap<>();

    static {
        register(new SpellDefinition(
                "fire_beam",
                "Fire Beam",
                "A basic pyromancy spell.",
                new int[][]{{3,4},{4,6},{6,2}},
                List.of(3,4,4,6,6,2),
                player -> {
                    // TODO: fire beam logic
                },  new ResourceLocation("minecraft", "textures/item/blaze_powder.png")
                ,3
        ));

        register(new SpellDefinition(
                "teleport",
                "Teleport",
                "Move instantly.",
                new int[][]{{1,5},{4,8},{2,3}},
                List.of(1,5,4,8,2,3),
                player -> {
                    // teleport logic
                },  new ResourceLocation("psychic", "textures/gui/widgets/fills/chaos_star.png")
                , -20
        ));

    }

    private static void register(SpellDefinition spell) {
        SPELLS.put(spell.id, spell);
    }

    public static SpellDefinition get(String id) {
        return SPELLS.get(id);
    }
}