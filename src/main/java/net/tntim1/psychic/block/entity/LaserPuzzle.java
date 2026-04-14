package net.tntim1.psychic.block.entity;

import java.util.List;

public class LaserPuzzle {
    public final String spellId;
    public final int difficulty;

    public LaserPuzzle(String spellId, int difficulty) {
        this.spellId = spellId;
        this.difficulty = difficulty;
    }
    public static final List<LaserPuzzle> PUZZLES = List.of(
            new LaserPuzzle("fire_beam", 0),
            new LaserPuzzle("ice_shard", 1),
            new LaserPuzzle("chain_lightning", 2)
    );

}