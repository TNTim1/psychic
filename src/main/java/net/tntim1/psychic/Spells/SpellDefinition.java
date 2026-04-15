package net.tntim1.psychic.Spells;

import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SpellDefinition {
    public final String id;
    public final String title;
    public final String description;
    public final ResourceLocation texture;

    // Core logic pattern (pairs)
    public final Set<Set<Integer>> pattern;

    // UI pattern (ordered input)
    public final List<Integer> displayPattern;

    // Optional: behavior when cast
    public final SpellAction action;

    public  final  int  warpChange;
    public final float manaCost;

    public SpellDefinition(String id,
                           String title,
                           String description,
                           int[][] connections,
                           List<Integer> displayPattern,
                           SpellAction action, ResourceLocation texture,
                           int warpChange,
                           float manaCost) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.pattern = createPattern(connections);
        this.texture = texture;
        this.displayPattern = displayPattern;
        this.action = action;
        this.warpChange= warpChange;
        this.manaCost=manaCost;
    }

    private Set<Set<Integer>> createPattern(int[][] connections) {
        Set<Set<Integer>> pattern = new HashSet<>();
        for (int[] conn : connections) {
            Set<Integer> pair = new HashSet<>();
            pair.add(conn[0]);
            pair.add(conn[1]);
            pattern.add(pair);
        }
        return pattern;
    }
}