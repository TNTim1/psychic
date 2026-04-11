package net.tntim1.psychic.widget;

/**
 * A single requirement that must be met to activate a widget.
 *
 * <pre>
 *   // Kill 10 zombies
 *   TaskRequirement.kill("minecraft:zombie", 10)
 *
 *   // Collect 5 blaze rods
 *   TaskRequirement.item("minecraft:blaze_rod", 5)
 * </pre>
 */
public class TaskRequirement {

    public enum Type { KILL, ITEM }

    /** What kind of requirement this is. */
    public final Type type;

    /**
     * The target resource location string.
     * For KILL: entity type ID, e.g. "minecraft:zombie"
     * For ITEM: item ID, e.g. "minecraft:blaze_rod"
     */
    public final String targetId;

    /** How many are needed in total. */
    public final int required;

    /** Human-readable label shown in the progress UI, e.g. "Zombies slain" */
    public final String label;

    private TaskRequirement(Type type, String targetId, int required, String label) {
        this.type     = type;
        this.targetId = targetId;
        this.required = required;
        this.label    = label;
    }

    // ── factories ─────────────────────────────────────────────────────────────

    public static TaskRequirement kill(String entityId, int count, String label) {
        return new TaskRequirement(Type.KILL, entityId, count, label);
    }

    /** Uses a default label derived from the entity ID. */
    public static TaskRequirement kill(String entityId, int count) {
        String name = entityId.contains(":") ? entityId.split(":")[1] : entityId;
        return kill(entityId, count, capitalize(name) + " slain");
    }

    public static TaskRequirement item(String itemId, int count, String label) {
        return new TaskRequirement(Type.ITEM, itemId, count, label);
    }

    /** Uses a default label derived from the item ID. */
    public static TaskRequirement item(String itemId, int count) {
        String name = itemId.contains(":") ? itemId.split(":")[1] : itemId;
        return item(itemId, count, capitalize(name.replace('_', ' ')));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}