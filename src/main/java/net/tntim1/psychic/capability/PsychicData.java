package net.tntim1.psychic.player_data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.tntim1.psychic.widget.TabRegistry;
import net.tntim1.psychic.widget.WidgetDefinition;

import java.util.*;

public class PsychicData {

    private final Set<String> unlockedWidgets = new HashSet<>();

    // ── mutation ──────────────────────────────────────────────────────────────

    public void unlock(String id) {
        unlockedWidgets.add(id);
    }

    /**
     * Locks a widget and cascades: any widget whose dependency list contains
     * {@code id} (directly or transitively) is also locked.
     *
     * <p>Example: A → B → C. Locking A also locks B and C.
     */
    public void lockCascading(String id) {
        Set<String> toLock = new HashSet<>();
        collectDownstream(id, toLock, buildDependentMap());
        toLock.add(id);
        unlockedWidgets.removeAll(toLock);
    }

    /**
     * Checks whether ALL declared dependencies of the given widget are unlocked.
     * Server-side authority check used in ActivateWidgetPacket.
     */
    public boolean areDependenciesMet(String widgetId) {
        WidgetDefinition def = TabRegistry.findById(widgetId);
        if (def == null) return false;
        for (String dep : def.dependencies) {
            if (!unlockedWidgets.contains(dep)) return false;
        }
        return true;
    }

    public boolean isUnlocked(String id) {
        return unlockedWidgets.contains(id);
    }

    // ── read-only view (used by SyncKnowledgePacket) ──────────────────────────

    public Set<String> getUnlockedIds() {
        return Collections.unmodifiableSet(unlockedWidgets);
    }

    // ── copy / merge ──────────────────────────────────────────────────────────

    public void copyFrom(PsychicData source) {
        this.unlockedWidgets.clear();
        this.unlockedWidgets.addAll(source.unlockedWidgets);
    }

    // ── NBT persistence ───────────────────────────────────────────────────────

    public void save(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (String id : unlockedWidgets) {
            list.add(StringTag.valueOf(id));
        }
        nbt.put("UnlockedWidgets", list);
    }

    public void load(CompoundTag nbt) {
        unlockedWidgets.clear();
        ListTag list = nbt.getList("UnlockedWidgets", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            unlockedWidgets.add(list.getString(i));
        }
    }

    // ── cascade helpers ───────────────────────────────────────────────────────

    /**
     * Builds a reverse-dependency map:
     *   dep_id → [list of widget IDs that declare dep_id as a dependency]
     *
     * e.g. B.deps=[A], C.deps=[B]  →  { A→[B], B→[C] }
     */
    private static Map<String, List<String>> buildDependentMap() {
        Map<String, List<String>> map = new HashMap<>();
        for (WidgetDefinition w : TabRegistry.getAllWidgets()) {
            for (String dep : w.dependencies) {
                map.computeIfAbsent(dep, k -> new ArrayList<>()).add(w.id);
            }
        }
        return map;
    }

    /**
     * Recursively collects all widget IDs transitively downstream of rootId.
     * Cycle-safe: result.add() returns false for already-visited nodes.
     */
    private static void collectDownstream(String rootId,
                                          Set<String> result,
                                          Map<String, List<String>> dependentMap) {
        for (String child : dependentMap.getOrDefault(rootId, Collections.emptyList())) {
            if (result.add(child)) {
                collectDownstream(child, result, dependentMap);
            }
        }
    }
}