package net.tntim1.psychic.block.entity;

import net.minecraft.server.level.ServerPlayer;
import net.tntim1.psychic.capability.PsychicCapability;
import java.util.List;
import java.util.Map;

public class LaserPuzzle {
    public final String spellId;
    public final int difficulty;
    public final List<List<Integer>> goals;

    public LaserPuzzle(String spellId, int difficulty, List<List<Integer>> goals) {
        this.spellId = spellId;
        this.difficulty = difficulty;
        this.goals = goals;
    }

    // Central Registry of Spell Goals
    public static final Map<String, LaserPuzzle> PUZZLES = Map.of(
            "fire_beam", new LaserPuzzle("fire_beam", 0, List.of(List.of(1, 4))),
            "ice_shard", new LaserPuzzle("ice_shard", 1, List.of(List.of(1, 3), List.of(2, 4))),
            "chain_lightning", new LaserPuzzle("chain_lightning", 2, List.of(List.of(1, 2), List.of(3, 4), List.of(1, 3)))
    );

    public static LaserPuzzle get(String spellId) {
        return PUZZLES.getOrDefault(spellId, PUZZLES.get("fire_beam"));
    }
}