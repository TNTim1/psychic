package net.tntim1.psychic.block.entity;

import net.tntim1.psychic.Spells.SpellDefinition;
import net.tntim1.psychic.Spells.SpellRegistry;
import java.util.List;
import java.util.stream.Collectors;

public class LaserPuzzle {
    public final String spellId;
    public final List<List<Integer>> goals;

    public LaserPuzzle(String spellId) {
        this.spellId = spellId;
        this.goals = fetchGoalsFromRegistry(spellId);
    }

    private List<List<Integer>> fetchGoalsFromRegistry(String id) {
        SpellDefinition spell = SpellRegistry.get(id);

        // If the spell doesn't exist, return an empty list or a default
        if (spell == null) return List.of();

        // Convert Set<Set<Integer>> to List<List<Integer>>
        return spell.pattern.stream()
                .map(set -> set.stream().collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    // Static helper to create a puzzle instance
    public static LaserPuzzle create(String spellId) {
        return new LaserPuzzle(spellId);
    }
    public static LaserPuzzle get(String spellId) {
        // We call the constructor, which runs fetchGoalsFromRegistry
        LaserPuzzle puzzle = new LaserPuzzle(spellId);

        // If the registry didn't have the spell, goals will be empty.
        // You could return a default here if you prefer.
        return puzzle;
    }

}