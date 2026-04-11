package net.tntim1.psychic.player_data;

import net.tntim1.psychic.widget.TabRegistry;
import net.tntim1.psychic.widget.WidgetDefinition;

import java.util.*;

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
    public static void unlock(String widgetId) {
        unlockedIds.add(widgetId);
        queueUnlockToast(widgetId);
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
    // Task progress: "widgetId:reqIndex" → count
    private static final Map<String, Integer> TASK_PROGRESS = new HashMap<>();

    // Toast queues
    public static final java.util.Queue<UnlockToast> UNLOCK_TOASTS = new java.util.ArrayDeque<>();
    public static final java.util.Queue<TaskToast>   TASK_TOASTS   = new java.util.ArrayDeque<>();

    // ── task progress ─────────────────────────────────────────────────────────

    public static int getTaskProgress(String widgetId, int reqIndex) {
        return TASK_PROGRESS.getOrDefault(widgetId + ":" + reqIndex, 0);
    }

    public static void applyTaskProgressSnapshot(Map<String, Integer> snapshot) {
        TASK_PROGRESS.clear();
        TASK_PROGRESS.putAll(snapshot);
    }

    /** True when all task requirements for a widget are met. */
    public static boolean areTasksMet(String widgetId) {
        WidgetDefinition w = TabRegistry.findById(widgetId);
        if (w == null || !w.hasTasks()) return true; // no tasks = always met
        for (int i = 0; i < w.taskRequirements.size(); i++) {
            if (getTaskProgress(widgetId, i) < w.taskRequirements.get(i).required) return false;
        }
        return true;
    }

    // ── toast queuing ─────────────────────────────────────────────────────────

    public static void queueUnlockToast(String widgetId) {
        WidgetDefinition w = TabRegistry.findById(widgetId);
        if (w != null) UNLOCK_TOASTS.add(new UnlockToast(w.label, System.currentTimeMillis()));
    }

    public static void queueTaskToast(String widgetId, String taskLabel, boolean completed) {
        WidgetDefinition w = TabRegistry.findById(widgetId);
        String widgetLabel = w != null ? w.label : widgetId;
        TASK_TOASTS.add(new TaskToast(widgetLabel, taskLabel, completed, System.currentTimeMillis()));
    }

    // ── toast record types ────────────────────────────────────────────────────

    public record UnlockToast(String widgetLabel, long createdAt) {}
    public record TaskToast(String widgetLabel, String taskLabel, boolean completed, long createdAt) {}
}