package net.tntim1.psychic.Spells;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

public class WorldSpellData extends SavedData {
    // A Spell Name maps to a Set of Connections.
    // Each Connection is a Set of 2 Integers.
    private final Map<String, Set<Set<Integer>>> spellPatterns = new HashMap<>();

    public WorldSpellData() {
        defineHardcodedSpells();
    }

    private void defineHardcodedSpells() {
        spellPatterns.put("fireball", createPattern(new int[][]{{1, 4},{4, 6},{6, 2}}));
        spellPatterns.put("teleport", createPattern(new int[][]{{1, 5}, {4, 8}, {2, 3}}));
    }

    // Helper to let the UI or other classes see the spells
    public Map<String, Set<Set<Integer>>> getSpellPatterns() {
        return Collections.unmodifiableMap(this.spellPatterns);
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
    public static WorldSpellData get(ServerLevel level) {
        // computeIfAbsent is the standard way to initialize/get SavedData in 1.20+
        return level.getDataStorage().computeIfAbsent(
                WorldSpellData::load,
                WorldSpellData::new,
                "psychic_spells"
        );
    }

    /**
     * Converts the player's chronological sequence (1, 2, 3)
     * into a Set of connections ({1,2}, {2,3}) to check against the requirement.
     */
    public boolean checkMatch(String spellName, List<Integer> playerInput) {
        // A spell must have an even number of points if we are doing discrete pairs
        if (playerInput.size() < 2 || playerInput.size() % 2 != 0) return false;

        Set<Set<Integer>> inputConnections = new HashSet<>();

        // Step by 2 to capture the discrete pairs created in handleLanePress
        for (int i = 0; i < playerInput.size() - 1; i += 2) {
            Set<Integer> pair = new HashSet<>();
            pair.add(playerInput.get(i));
            pair.add(playerInput.get(i + 1));
            inputConnections.add(pair);
        }

        Set<Set<Integer>> requiredPattern = spellPatterns.get(spellName);
        return requiredPattern != null && inputConnections.equals(requiredPattern);
    }

    // --- Save/Load Logic ---

    public static WorldSpellData load(CompoundTag nbt) {
        WorldSpellData data = new WorldSpellData();
        CompoundTag spellsTag = nbt.getCompound("Spells");
        for (String key : spellsTag.getAllKeys()) {
            int[] flatArray = spellsTag.getIntArray(key);
            Set<Set<Integer>> pattern = new HashSet<>();
            // Read pairs from the flat array (pair1_a, pair1_b, pair2_a, pair2_b...)
            for (int i = 0; i < flatArray.length; i += 2) {
                Set<Integer> pair = new HashSet<>();
                pair.add(flatArray[i]);
                pair.add(flatArray[i+1]);
                pattern.add(pair);
            }
            data.spellPatterns.put(key, pattern);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        CompoundTag spellsTag = new CompoundTag();
        spellPatterns.forEach((name, pattern) -> {
            List<Integer> flatList = new ArrayList<>();
            for (Set<Integer> pair : pattern) {
                flatList.addAll(pair);
            }
            spellsTag.putIntArray(name, flatList.stream().mapToInt(i -> i).toArray());
        });
        nbt.put("Spells", spellsTag);
        return nbt;
    }
}