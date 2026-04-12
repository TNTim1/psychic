package net.tntim1.psychic.widget;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TabRegistry {

    private static final List<TabDefinition> TABS = new ArrayList<>();

    public static WidgetDefinition findById(String id) {
        for (TabDefinition tab : TABS) {
            for (WidgetDefinition w : tab.widgets) {
                if (w.id.equals(id)) return w;
            }
        }
        return null;
    }

    public static List<WidgetDefinition> getAllWidgets() {
        List<WidgetDefinition> all = new ArrayList<>();
        for (TabDefinition tab : TABS) all.addAll(tab.widgets);
        return all;
    }

    // =========================================================================
    // TABS
    // =========================================================================
    static {

        // -----------------------------------------------------------------
        // Tab 1: Spells
        //
        // Unlock chain:
        //   fireball       (root — kill 5 blazes + collect 2 blaze rods)
        //     └─ blink          (dep: fireball  | task: collect 1 ender pearl)
        //         └─ frost_nova      (dep: blink     | task: kill 10 strays)
        //             └─ chain_lightning (deps: frost_nova + blink | tasks: kill 10 skeletons + get 1 lightning rod)
        // -----------------------------------------------------------------
        TabDefinition spells = new TabDefinition(
                "Spells",
                new ResourceLocation("minecraft", "textures/item/fire_charge.png"),
                2048, 1024);

        // ── Fireball ─────────────────────────────────────────────────────────
        // Root widget: no widget dependencies, but requires two task completions
        // before it activates. The popup will show two progress bars.
        spells.addWidget(WidgetDefinition.infoWithTasks(
                "fireball",
                new ResourceLocation("minecraft", "textures/item/blaze_powder.png"),
                200, 150,
                "Fireball",
                "Launches a concentrated fireball in the direction you face.\n\n" +
                        "Cost: 30 mana\nCooldown: 2s\nDamage: 8 ❤",
                null,   // no widget dependencies
                TaskRequirement.kill("minecraft:blaze", 5,  "Blazes slain"),
                TaskRequirement.item("minecraft:blaze_rod", 2, "Blaze rods collected")
        ));

        // ── Blink ─────────────────────────────────────────────────────────────
        // Widget dep: fireball must be unlocked first.
        // Task: pick up 1 ender pearl (simple single-item gate).
        spells.addWidget(WidgetDefinition.infoWithTasks(
                "blink",
                new ResourceLocation("minecraft", "textures/item/ender_pearl.png"),
                550, 280,
                "Blink",
                "Instantly teleport up to 16 blocks forward.\n\n" +
                        "Cost: 20 mana\nCooldown: 5s\nPasses through solid blocks.",
                new String[]{"fireball"},   // widget dep
                TaskRequirement.item("minecraft:ender_pearl", 1, "Ender pearl obtained")
        ));

        // ── Frost Nova ────────────────────────────────────────────────────────
        // Widget dep: blink.
        // Tasks: kill 10 strays (cold-themed gating — fits the spell's lore).
        spells.addWidget(WidgetDefinition.infoWithTasks(
                "frost_nova",
                new ResourceLocation("minecraft", "textures/item/snowball.png"),
                900, 180,
                "Frost Nova",
                "Freezes all mobs within 5 blocks.\n\n" +
                        "Cost: 45 mana\nDuration: 3s\nCooldown: 8s",
                new String[]{"blink"},
                TaskRequirement.kill("minecraft:stray", 10, "Strays defeated")
        ));

        // ── Chain Lightning ───────────────────────────────────────────────────
        // Widget deps: BOTH frost_nova AND blink required.
        // Tasks: kill 10 skeletons AND collect 1 lightning rod.
        // The popup will show two separate progress bars, each with a checkmark
        // when done. The widget only auto-activates when BOTH are complete.
        spells.addWidget(WidgetDefinition.listWithTasks(
                "chain_lightning",
                new ResourceLocation("minecraft", "textures/item/lightning_rod.png"),
                1300, 350,
                "Chain Lightning",
                new String[]{"frost_nova", "blink"},
                new TaskRequirement[]{
                        TaskRequirement.kill("minecraft:skeleton",    10, "Skeletons struck"),
                        TaskRequirement.item("minecraft:lightning_rod", 1, "Lightning rod obtained")
                },
                "Strikes nearest mob with lightning",
                "Chains up to 4 additional targets",
                "Damage reduces 20% per hop",
                "Cost: 60 mana | Cooldown: 10s"
        ));

        TABS.add(spells);

        // -----------------------------------------------------------------
        // Tab 2: Psychic Powers
        //
        //   mind_probe    (task: kill 3 endermen — "read their minds")
        //     └─ telekinesis   (dep: mind_probe | task: collect 1 chorus fruit)
        //         └─ psychic_shield (dep: telekinesis | tasks: survive 20 hits + collect 3 amethyst shards)
        // -----------------------------------------------------------------
        TabDefinition psychic = new TabDefinition(
                "Psychic",
                new ResourceLocation("psychic", "textures/item/liber_chaotica.png"),
                2048, 1024);

        // ── Mind Probe ────────────────────────────────────────────────────────
        // Root — single kill task. Endermen as the lore-friendly gate ("probe their alien minds").
        psychic.addWidget(WidgetDefinition.infoWithTasks(
                "mind_probe",
                new ResourceLocation("minecraft", "textures/item/compass.png"),
                250, 200,
                "Mind Probe",
                "Reveals the health, armor, and active effects\nof the targeted entity.\n\n" +
                        "Range: 32 blocks\nCost: 10 mana",
                null,
                TaskRequirement.kill("minecraft:enderman", 3, "Endermen probed")
        ));

        // ── Telekinesis ───────────────────────────────────────────────────────
        // Dep: mind_probe. Task: chorus fruit (teleportation theme).
        psychic.addWidget(WidgetDefinition.listWithTasks(
                "telekinesis",
                new ResourceLocation("minecraft", "textures/item/writable_book.png"),
                650, 150,
                "Telekinesis",
                new String[]{"mind_probe"},
                new TaskRequirement[]{
                        TaskRequirement.item("minecraft:chorus_fruit", 1, "Chorus fruit obtained")
                },
                "Pull an item or mob toward you from up to 20 blocks",
                "Heavier mobs require more mana",
                "Cannot pull bosses",
                "Cost: 15–50 mana depending on mass"
        ));

        // ── Psychic Shield ────────────────────────────────────────────────────
        // Dep: telekinesis. Two tasks: survive 20 hits (damage events, tracked
        // separately via a PlayerHurtEvent listener) AND collect amethyst shards.
        // This shows the "mixed type" multi-bar layout in the popup.
        psychic.addWidget(WidgetDefinition.infoWithTasks(
                "psychic_shield",
                new ResourceLocation("minecraft", "textures/item/amethyst_shard.png"),
                1100, 300,
                "Psychic Shield",
                "Creates a brief psychic barrier that negates\nthe next hit within 1 second.\n\n" +
                        "Cost: 25 mana\nCooldown: 6s",
                new String[]{"telekinesis"},
                // NOTE: "hits survived" uses a custom Type.DAMAGE handled by a
                // separate PlayerHurtEvent listener that calls the same
                // TaskProgress.increment("psychic_shield", 0, 20) path.
                // You'd add Type.DAMAGE to TaskRequirement.Type for this,
                // or model it as a kill of a dummy tracker entity — whichever
                // fits your architecture. Shown here for completeness.
                TaskRequirement.kill("minecraft:amethyst_cluster", 20, "Amethyst cluster"), // placeholder
                TaskRequirement.item("minecraft:amethyst_shard",    3,  "Amethyst shards")
        ));

        TABS.add(psychic);

        // -----------------------------------------------------------------
        // Tab 3: Lore — no tasks, no deps. These use the plain info() factory
        // so nothing about them changes. Kept identical to the original.
        // -----------------------------------------------------------------
        TabDefinition warp = new TabDefinition(
                "Warp",
                new ResourceLocation("psychic", "textures/gui/widgets/fills/chaos_star.png"),
                1536, 1024);

        warp.addWidget(WidgetDefinition.infoWithTasks(
                "center",
                new ResourceLocation("minecraft", "textures/item/compass.png"),
                0, 0,
                "The Warp",
                "Reveals the health, armor, and active effects\nof the targeted entity.\n\n" +
                        "Range: 32 blocks\nCost: 10 mana",
                null,
                TaskRequirement.kill("minecraft:enderman", 3, "Endermen probed")
        ));
        TABS.add(warp);
    }

    public static List<TabDefinition> getTabs() {
        return Collections.unmodifiableList(TABS);
    }
}