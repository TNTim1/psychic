package net.tntim1.psychic.player_data;

import net.tntim1.psychic.widget.TabRegistry;
import net.tntim1.psychic.widget.WidgetDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Client-side mirror of the player's unlocked widget set.
 *
 * <p>Updated by {@link net.tntim1.psychic.network.SyncKnowledgePacket} whenever
 * the server changes the player's data.
 */
public class ClientKnowledge {

    private static Set<String> unlockedIds = new HashSet<>();

    /** Called by SyncKnowledgePacket on the client main thread. */
    public static void setUnlockedIds(Set<String> ids) {
        unlockedIds = new HashSet<>(ids);
    }

    /** True if the given widget ID is in the unlocked set. */
    public static boolean isUnlocked(String id) {
        return unlockedIds.contains(id);
    }

    /**
     * True if every dependency declared by the widget with {@code widgetId}
     * is currently unlocked on this client.
     *
     * <p>A widget with no dependencies always returns true.
     */
    public static boolean areDependenciesMet(String widgetId) {
        WidgetDefinition def = TabRegistry.findById(widgetId);
        if (def == null) return false;
        for (String dep : def.dependencies) {
            if (!unlockedIds.contains(dep)) return false;
        }
        return true;
    }

    /**
     * Returns the labels of any dependencies that are NOT yet unlocked,
     * for display in the popup's locked state message.
     *
     * <p>Returns an empty list when all dependencies are met.
     */
    public static List<String> getMissingDependencyLabels(String widgetId) {
        WidgetDefinition def = TabRegistry.findById(widgetId);
        if (def == null || def.dependencies.isEmpty()) return Collections.emptyList();

        List<String> missing = new ArrayList<>();
        for (String dep : def.dependencies) {
            if (!unlockedIds.contains(dep)) {
                WidgetDefinition depDef = TabRegistry.findById(dep);
                missing.add(depDef != null ? depDef.label : dep);
            }
        }
        return missing;
    }
}