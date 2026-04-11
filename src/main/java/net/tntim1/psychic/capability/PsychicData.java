package net.tntim1.psychic.player_data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PsychicData {

    private final Set<String> unlockedWidgets = new HashSet<>();

    // ── mutation ──────────────────────────────────────────────────────────────

    public void unlock(String id) {
        unlockedWidgets.add(id);
    }

    /** Remove an unlock (useful for testing or admin commands). */
    public void lock(String id) {
        unlockedWidgets.remove(id);
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
}