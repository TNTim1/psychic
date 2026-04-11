package net.tntim1.psychic.player_data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.tntim1.psychic.Psychic;
import net.tntim1.psychic.widget.TabRegistry;
import net.tntim1.psychic.widget.WidgetDefinition;

import java.util.*;

public class PsychicData {

    private final Set<String> unlockedWidgets = new HashSet<>();

    /** Task progress tracker — saved alongside unlocked widget IDs. */
    public final TaskProgress taskProgress = new TaskProgress();

    // ── static accessor ───────────────────────────────────────────────────────

    /**
     * Retrieves the PsychicData capability from any player.
     * Throws if the capability is missing (should never happen after registration).
     *
     * Usage:
     *   PsychicData data = PsychicData.get(player);
     */
    public static PsychicData get(Player player) {
        return player.getCapability(Psychic.PSYCHIC_DATA)
                .orElseThrow(() -> new IllegalStateException(
                        "PsychicData capability missing on player: " + player.getName().getString()
                ));
    }

    // ── mutation ──────────────────────────────────────────────────────────────

    public void unlock(String id) {
        unlockedWidgets.add(id);
    }

    /**
     * Locks a widget and cascades: any widget whose dependency list contains
     * {@code id} (directly or transitively) is also locked.
     */
    public void lockCascading(String id) {
        Set<String> toLock = new HashSet<>();
        collectDownstream(id, toLock, buildDependentMap());
        toLock.add(id);
        unlockedWidgets.removeAll(toLock);
    }

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

    public Set<String> getUnlockedIds() {
        return Collections.unmodifiableSet(unlockedWidgets);
    }

    // ── copy / merge ──────────────────────────────────────────────────────────

    public void copyFrom(PsychicData source) {
        this.unlockedWidgets.clear();
        this.unlockedWidgets.addAll(source.unlockedWidgets);
        // Also copy task progress so it survives death/respawn
        this.taskProgress.applySnapshot(source.taskProgress.snapshot());
    }

    // ── NBT persistence ───────────────────────────────────────────────────────

    public void save(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (String id : unlockedWidgets) {
            list.add(StringTag.valueOf(id));
        }
        nbt.put("UnlockedWidgets", list);
        nbt.put("TaskProgress", taskProgress.save());
    }

    public void load(CompoundTag nbt) {
        unlockedWidgets.clear();
        ListTag list = nbt.getList("UnlockedWidgets", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            unlockedWidgets.add(list.getString(i));
        }
        if (nbt.contains("TaskProgress")) {
            taskProgress.load(nbt.getCompound("TaskProgress"));
        }
    }

    // ── marked dirty helper ───────────────────────────────────────────────────

    /**
     * Call after any mutation so the capability serializer knows to re-save.
     * With Forge capabilities this is a no-op at the data level (the provider
     * serializes on demand), but keeping the call in TaskEventHandler is good
     * practice if you ever switch to a SavedData approach.
     */
    public void setDirty() {
        // no-op for capability-backed storage; override if using WorldSavedData
    }

    // ── cascade helpers ───────────────────────────────────────────────────────

    private static Map<String, List<String>> buildDependentMap() {
        Map<String, List<String>> map = new HashMap<>();
        for (WidgetDefinition w : TabRegistry.getAllWidgets()) {
            for (String dep : w.dependencies) {
                map.computeIfAbsent(dep, k -> new ArrayList<>()).add(w.id);
            }
        }
        return map;
    }

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