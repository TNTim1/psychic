package net.tntim1.psychic.player_data;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks per-player progress toward widget task requirements.
 *
 * Stored inside PsychicData NBT as a "TaskProgress" compound.
 * Key format: "widgetId:requirementIndex" → count achieved so far.
 *
 * Example NBT layout:
 * <pre>
 *   TaskProgress: {
 *     "fireball:0": 3,   // killed 3 of 5 blazes required
 *     "fireball:1": 2,   // collected 2 of 2 blaze rods ← done
 *   }
 * </pre>
 */
public class TaskProgress {
    private final Map<String, Integer> counts = new HashMap<>(); // MUST be non-static

    public CompoundTag save() {
        CompoundTag nbt = new CompoundTag();
        counts.forEach(nbt::putInt);
        return nbt;
    }

    public void load(CompoundTag nbt) {
        counts.clear();
        for (String key : nbt.getAllKeys()) {
            counts.put(key, nbt.getInt(key));
        }
    }

    public Map<String, Integer> snapshot() {
        return new HashMap<>(counts);
    }

    public void applySnapshot(Map<String, Integer> map) {
        this.counts.clear();
        this.counts.putAll(map);
    }
    // ── key helper ────────────────────────────────────────────────────────────

    private static String key(String widgetId, int reqIndex) {
        return widgetId + ":" + reqIndex;
    }

    // ── read / write ──────────────────────────────────────────────────────────

    public int get(String widgetId, int reqIndex) {
        return counts.getOrDefault(key(widgetId, reqIndex), 0);
    }

    /**
     * Increments the count for one requirement.
     * Caps at {@code max} so we never store more than needed.
     *
     * @return the new count (after increment)
     */
    public int increment(String widgetId, int reqIndex, int max) {
        String k = key(widgetId, reqIndex);
        int current = counts.getOrDefault(k, 0);
        int next = Math.min(current + 1, max);
        counts.put(k, next);
        return next;
    }

    /**
     * Sets the count directly. Used when syncing from item inventory checks.
     */
    public void set(String widgetId, int reqIndex, int value) {
        counts.put(key(widgetId, reqIndex), value);
    }

    /** True when this specific requirement is satisfied. */
    public boolean isMet(String widgetId, int reqIndex, int required) {
        return get(widgetId, reqIndex) >= required;
    }

    // ── NBT ──────────────────────────────────────────────────────────────────


}