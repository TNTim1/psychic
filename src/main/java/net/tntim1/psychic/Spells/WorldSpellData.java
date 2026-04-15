package net.tntim1.psychic.Spells;

import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import java.util.*;

public class WorldSpellData extends SavedData {

    public WorldSpellData() {
        // No longer need to store a local map or hardcode spells here
    }

    /**
     * Checks player input directly against the static SpellRegistry.
     */
    public boolean checkMatch(String spellName, List<Integer> playerInput) {
        // 1. Basic Validation (Must be pairs: 2, 4, 6 points...)
        if (playerInput.size() < 2 || playerInput.size() % 2 != 0) return false;

        // 2. Build the player's input set
        Set<Set<Integer>> inputConnections = new HashSet<>();
        for (int i = 0; i < playerInput.size() - 1; i += 2) {
            Set<Integer> pair = new HashSet<>();
            pair.add(playerInput.get(i));
            pair.add(playerInput.get(i + 1));
            inputConnections.add(pair);
        }

        // 3. Lookup directly from the Registry
        SpellDefinition def = SpellRegistry.get(spellName);
        if (def == null) return false;

        // 4. Compare player set to registry set
        return inputConnections.equals(def.pattern);
    }

    public static WorldSpellData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                nbt -> new WorldSpellData(), // Load does nothing
                WorldSpellData::new,
                "psychic_spells"
        );
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        return nbt; // Save nothing
    }
}