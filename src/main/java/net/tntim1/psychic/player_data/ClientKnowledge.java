package net.tntim1.psychic.player_data;

import java.util.HashSet;
import java.util.Set;

public class ClientKnowledge {
    // This is the static instance the GUI can actually talk to
    private static Set<String> unlockedIds = new HashSet<>();

    public static void setUnlockedIds(Set<String> ids) {
        unlockedIds = ids;
    }

    public static boolean isUnlocked(String id) {
        return unlockedIds.contains(id);
    }
}