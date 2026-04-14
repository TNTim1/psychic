package net.tntim1.psychic.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.tntim1.psychic.Psychic;
import net.tntim1.psychic.network.ModPackets;
import net.tntim1.psychic.network.SyncKnowledgePacket;
import net.tntim1.psychic.network.SyncSpellHistoryPacket;
import net.tntim1.psychic.player_data.TaskProgress;
import net.tntim1.psychic.widget.TabRegistry;
import net.tntim1.psychic.widget.WidgetDefinition;

import java.util.*;

public class PsychicData {
    // ONE set for logic, ONE list for history
    private final Set<String> unlockedIds = new HashSet<>();
    private final List<String> unlockedSpellsOrder = new ArrayList<>();

    public final TaskProgress taskProgress = new TaskProgress();

    public static PsychicData get(Player player) {
        return player.getCapability(Psychic.PSYCHIC_DATA)
                .orElseThrow(() -> new IllegalStateException("PsychicData missing on: " + player.getName().getString()));
    }

    // --- Unified Mutation ---
    public void unlock(String id) {
        this.unlockedIds.add(id);
        if (!this.unlockedSpellsOrder.contains(id)) {
            this.unlockedSpellsOrder.add(id);
        }
    }

    public void unlockSpell(String id, ServerPlayer player) {
        this.unlock(id);
        // Sync everything
        SyncKnowledgePacket.sendToPlayer(player, this);
        ModPackets.sendToPlayer(new SyncSpellHistoryPacket(this.unlockedSpellsOrder), player);
    }

    // --- Correct Predicates ---
    public boolean isUnlocked(String id) {
        return unlockedIds.contains(id);
    }

    public Set<String> getUnlockedIds() {
        return Collections.unmodifiableSet(unlockedIds);
    }

    public List<String> getUnlockedSpellsOrder() {
        return unlockedSpellsOrder;
    }

    // --- Fixed Persistence (Merge everything into these two) ---
    public void saveNBTData(CompoundTag nbt) {
        // 1. Save Knowledge Set
        ListTag knowledgeList = new ListTag();
        for (String id : unlockedIds) knowledgeList.add(StringTag.valueOf(id));
        nbt.put("psychic_knowledge", knowledgeList);

        // 2. Save History List
        ListTag historyList = new ListTag();
        for (String id : unlockedSpellsOrder) historyList.add(StringTag.valueOf(id));
        nbt.put("psychic_history", historyList);

        // 3. Save Task Progress
        nbt.put("TaskProgress", taskProgress.save());
    }

    public void loadNBTData(CompoundTag nbt) {
        // 1. Load Knowledge
        unlockedIds.clear();
        if (nbt.contains("psychic_knowledge", Tag.TAG_LIST)) {
            ListTag list = nbt.getList("psychic_knowledge", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) unlockedIds.add(list.getString(i));
        }

        // 2. Load History
        unlockedSpellsOrder.clear();
        if (nbt.contains("psychic_history", Tag.TAG_LIST)) {
            ListTag list = nbt.getList("psychic_history", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) unlockedSpellsOrder.add(list.getString(i));
        }

        // 3. Load Tasks
        if (nbt.contains("TaskProgress")) {
            taskProgress.load(nbt.getCompound("TaskProgress"));
        }
    }

    // --- Survival Fix ---
    public void copyFrom(PsychicData source) {
        this.unlockedIds.clear();
        this.unlockedIds.addAll(source.unlockedIds);
        this.unlockedSpellsOrder.clear();
        this.unlockedSpellsOrder.addAll(source.unlockedSpellsOrder);
        this.taskProgress.applySnapshot(source.taskProgress.snapshot());
    }

    // --- Cascade Logic (Updated to use unlockedIds) ---
    public boolean areDependenciesMet(String widgetId) {
        WidgetDefinition def = TabRegistry.findById(widgetId);
        if (def == null) return false;
        for (String dep : def.dependencies) {
            if (!unlockedIds.contains(dep)) return false;
        }
        return true;
    }
    public void setDirty() {
        // This can be empty, it just lets TaskEventHandler know you've acknowledged the change
    }
    /**
     * Locks a widget and cascades: any widget that depends on this one
     * is also locked. Also removes them from the spiral history.
     */
    public void lockCascading(String id) {
        Set<String> toLock = new HashSet<>();
        // Find everything that needs to be removed
        collectDownstream(id, toLock, buildDependentMap());
        toLock.add(id);

        // Remove from the master knowledge set
        unlockedIds.removeAll(toLock);

        // Remove from the spiral history list
        unlockedSpellsOrder.removeAll(toLock);
    }

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