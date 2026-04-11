package net.tntim1.psychic.widget;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central registry for all Atlas Codex tabs.
 *
 * <p><b>To add a tab or widget, edit the static block at the bottom of this file.</b>
 *
 * <h3>Dependency syntax</h3>
 * Pass widget IDs as trailing varargs to any factory method:
 * <pre>
 *   WidgetDefinition.info("frost_nova", icon, 900, 180, "Frost Nova", "...", "fireball");
 *   //                                                                         ↑ must unlock fireball first
 * </pre>
 * For list widgets use the {@code String[] depIds} overload:
 * <pre>
 *   WidgetDefinition.list("chain_lightning", icon, 1300, 350, "Chain Lightning",
 *       new String[]{"frost_nova", "blink"},   // ← both required
 *       "Strikes nearest mob…", "Chains up to 4…");
 * </pre>
 */
public class TabRegistry {

    private static final List<TabDefinition> TABS = new ArrayList<>();

    // ── lookups (used by PsychicData for cascade logic) ───────────────────────

    /** Returns the widget with the given ID across all tabs, or null. */
    public static WidgetDefinition findById(String id) {
        for (TabDefinition tab : TABS) {
            for (WidgetDefinition w : tab.widgets) {
                if (w.id.equals(id)) return w;
            }
        }
        return null;
    }

    /** Returns a flat list of every widget across all tabs. */
    public static List<WidgetDefinition> getAllWidgets() {
        List<WidgetDefinition> all = new ArrayList<>();
        for (TabDefinition tab : TABS) {
            all.addAll(tab.widgets);
        }
        return all;
    }

    // =========================================================================
    // TABS — edit / extend freely below
    // =========================================================================
    static {

        // -----------------------------------------------------------------
        // Tab 1: Spells
        //
        // Dependency chain:
        //   fireball  (root — no deps)
        //     └─ blink      (requires fireball)
        //         └─ frost_nova   (requires blink)
        //             └─ chain_lightning (requires frost_nova AND blink)
        // -----------------------------------------------------------------
        TabDefinition spells = new TabDefinition(
                "Spells",
                new ResourceLocation("minecraft", "textures/item/fire_charge.png"),
                2048, 1024);

        // Root: no dependencies
        spells.addWidget(WidgetDefinition.info(
                "fireball",
                new ResourceLocation("minecraft", "textures/item/blaze_powder.png"),
                200, 150,
                "Fireball",
                "Launches a concentrated fireball in the direction you face.\n\n" +
                        "Cost: 30 mana\nCooldown: 2s\nDamage: 8 ❤"
                // no deps
        ));

        // Requires fireball
        spells.addWidget(WidgetDefinition.list_dependencies(
                "blink",
                new ResourceLocation("minecraft", "textures/item/ender_pearl.png"),
                550, 280,
                "Blink",
                new String[]{"fireball"},           // ← dependency
                "Instantly teleport up to 16 blocks forward",
                "Passes through solid blocks",
                "Cost: 20 mana | Cooldown: 5s",
                "Cannot blink into the void"
        ));

        // Requires blink
        spells.addWidget(WidgetDefinition.info(
                "frost_nova",
                new ResourceLocation("minecraft", "textures/item/snowball.png"),
                900, 180,
                "Frost Nova",
                "Freezes all mobs within 5 blocks.\n\nCost: 45 mana\nDuration: 3s\nCooldown: 8s",
                "blink"                             // ← dependency
        ));

        // Requires BOTH frost_nova and blink
        spells.addWidget(WidgetDefinition.list_dependencies(
                "chain_lightning",
                new ResourceLocation("minecraft", "textures/item/lightning_rod.png"),
                1300, 350,
                "Chain Lightning",
                new String[]{"frost_nova", "blink"}, // ← two deps, both required
                "Strikes nearest mob with lightning",
                "Chains up to 4 additional targets",
                "Damage reduces 20% per hop",
                "Cost: 60 mana | Cooldown: 10s"
        ));

        TABS.add(spells);

        // -----------------------------------------------------------------
        // Tab 2: Psychic Powers
        //
        //   mind_probe  (root)
        //     └─ telekinesis  (requires mind_probe)
        //         └─ psychic_shield (requires telekinesis)
        // -----------------------------------------------------------------
        TabDefinition psychic = new TabDefinition(
                "Psychic",
                new ResourceLocation("psychic", "textures/item/liber_chaotica.png"),
                2048, 1024);

        psychic.addWidget(WidgetDefinition.info(
                "mind_probe",
                new ResourceLocation("minecraft", "textures/item/compass.png"),
                250, 200,
                "Mind Probe",
                "Reveals the health, armor, and active effects\nof the targeted entity.\n\n" +
                        "Range: 32 blocks\nCost: 10 mana"
        ));

        psychic.addWidget(WidgetDefinition.list_dependencies(
                "telekinesis",
                new ResourceLocation("minecraft", "textures/item/writable_book.png"),
                650, 150,
                "Telekinesis",
                new String[]{"mind_probe"},
                "Pull an item or mob toward you from up to 20 blocks",
                "Heavier mobs require more mana",
                "Cannot pull bosses",
                "Cost: 15–50 mana depending on mass"
        ));

        psychic.addWidget(WidgetDefinition.info(
                "psychic_shield",
                new ResourceLocation("minecraft", "textures/item/amethyst_shard.png"),
                1100, 300,
                "Psychic Shield",
                "Creates a brief psychic barrier that negates\nthe next hit within 1 second.\n\n" +
                        "Cost: 25 mana\nCooldown: 6s",
                "telekinesis"
        ));

        TABS.add(psychic);

        // -----------------------------------------------------------------
        // Tab 3: Lore  (no dependency chain — all roots)
        // -----------------------------------------------------------------
        TabDefinition lore = new TabDefinition(
                "Lore",
                new ResourceLocation("minecraft", "textures/item/enchanted_book.png"),
                1536, 1024);

        lore.addWidget(WidgetDefinition.info(
                "lore_origin",
                new ResourceLocation("minecraft", "textures/item/book.png"),
                200, 200,
                "Origin of Psychic Arts",
                "Long before the First Age, wandering scholars\ndiscovered that emotion itself could be weaponised.\n\n" +
                        "They called the discipline 'Psychica' — the art\nof bending reality with thought alone."
        ));

        lore.addWidget(WidgetDefinition.list(
                "lore_factions",
                new ResourceLocation("minecraft", "textures/item/map.png"),
                700, 300,
                "Known Factions",
                null,                               // no deps (null → empty list)
                "The Veil — seekers of forbidden knowledge",
                "Iron Conclave — suppressors of psychic power",
                "Ember Circle — pyromancy & psychic fusion",
                "The Unbound — rogue practitioners"
        ));

        lore.addWidget(WidgetDefinition.info(
                "lore_timeline",
                new ResourceLocation("minecraft", "textures/item/clock.png"),
                1150, 180,
                "Timeline",
                "Year 0   — Psychica first recorded\nYear 340 — The Veil founded\nYear 612 — Iron Conclave wars\n" +
                        "Year 890 — The Unbound Schism\nYear 1024 — Present day"
        ));

        TABS.add(lore);
    }

    public static List<TabDefinition> getTabs() {
        return Collections.unmodifiableList(TABS);
    }
}