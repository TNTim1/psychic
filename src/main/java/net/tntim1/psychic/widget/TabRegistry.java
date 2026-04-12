package net.tntim1.psychic.widget;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TabRegistry {

    private static final List<TabDefinition> TABS = new ArrayList<>();

    public static WidgetDefinition findById(String id) {
        for (TabDefinition tab : TABS)
            for (WidgetDefinition w : tab.widgets)
                if (w.id.equals(id)) return w;
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
        // Chain: fireball → blink → frost_nova → chain_lightning
        // -----------------------------------------------------------------
        TabDefinition spells = new TabDefinition(
                "Spells",
                new ResourceLocation("minecraft", "textures/item/fire_charge.png"),
                2048, 1024);

        // ── Fireball ─────────────────────────────────────────────────────────
        // Multi-page popup: page 1 = spell description, page 2 = lore + blaze render.
        spells.addWidget(WidgetDefinition.simple(
                "fireball",
                new ResourceLocation("minecraft", "textures/item/blaze_powder.png"),
                200, 150,
                "Fireball",
                PopupContent.titledPages(
                        new String[]{ "Overview", "Requirements" },
                        new List[]{
                                // Page 1 — description
                                List.of(
                                        PopupContent.text(
                                                "Launches a concentrated fireball in the\n" +
                                                        "direction you face.\n\n" +
                                                        "Cost: 30 mana\nCooldown: 2s\nDamage: 8 \u2764"
                                        )
                                ),
                                // Page 2 — task flavour with renders
                                List.of(
                                        PopupContent.text("To unlock Fireball you must first\nprove your mastery of fire."),
                                        PopupContent.entityRender("minecraft:blaze",  "Blaze — slay 5"),
                                        PopupContent.itemRender("minecraft:blaze_rod", "Blaze Rod — collect 2")
                                )
                        }
                ),
                null, // no widget deps
                false, // no confirmation checkbox needed
                TaskRequirement.kill("minecraft:blaze",    5, "Blazes slain"),
                TaskRequirement.item("minecraft:blaze_rod", 2, "Blaze rods collected")
        ));

        // ── Blink ─────────────────────────────────────────────────────────────
        // Single-page popup using the convenience method; renders an ender pearl.
        spells.addWidget(WidgetDefinition.simple(
                "blink",
                new ResourceLocation("minecraft", "textures/item/ender_pearl.png"),
                550, 280,
                "Blink",
                PopupContent.page(
                        PopupContent.text(
                                "Instantly teleport up to 16 blocks forward.\n\n" +
                                        "Cost: 20 mana\nCooldown: 5s\n" +
                                        "Passes through solid blocks."
                        ),
                        PopupContent.itemRender("minecraft:ender_pearl", "Ender Pearl — collect 1")
                ),
                new String[]{ "fireball" },
                false,
                TaskRequirement.item("minecraft:ender_pearl", 1, "Ender pearl obtained")
        ));

        // ── Frost Nova ────────────────────────────────────────────────────────
        // Two pages; no explicit entity render (auto-generated via infoWithTasks for
        // brevity — mix of styles is fine).
        spells.addWidget(WidgetDefinition.infoWithTasks(
                "frost_nova",
                new ResourceLocation("minecraft", "textures/item/snowball.png"),
                900, 180,
                "Frost Nova",
                "Freezes all mobs within 5 blocks.\n\n" +
                        "Cost: 45 mana\nDuration: 3s\nCooldown: 8s",
                new String[]{ "blink" },
                TaskRequirement.kill("minecraft:stray", 10, "Strays defeated")
        ));

        // ── Chain Lightning ───────────────────────────────────────────────────
        // Multi-page + confirmation checkbox.
        // requiresConfirmation = true  →  checkbox must be ticked before activating.
        spells.addWidget(WidgetDefinition.simple(
                "chain_lightning",
                new ResourceLocation("minecraft", "textures/item/lightning_rod.png"),
                1300, 350,
                "Chain Lightning",
                PopupContent.titledPages(
                        new String[]{ "Ability", "How to unlock" },
                        new List[]{
                                List.of(
                                        PopupContent.list(
                                                "Strikes nearest mob with lightning",
                                                "Chains up to 4 additional targets",
                                                "Damage reduces 20% per hop",
                                                "Cost: 60 mana | Cooldown: 10s"
                                        )
                                ),
                                List.of(
                                        PopupContent.text("Strike down your enemies and channel\nthe storm to claim this power."),
                                        PopupContent.entityRender("minecraft:skeleton",     "Skeleton — strike 10"),
                                        PopupContent.itemRender("minecraft:lightning_rod",  "Lightning Rod — collect 1")
                                )
                        }
                ),
                new String[]{ "frost_nova", "blink" },
                true, // requires confirmation checkbox
                TaskRequirement.kill("minecraft:skeleton",    10, "Skeletons struck"),
                TaskRequirement.item("minecraft:lightning_rod", 1, "Lightning rod obtained")
        ));

        TABS.add(spells);

        // -----------------------------------------------------------------
        // Tab 2: Psychic Powers
        // Chain: mind_probe → telekinesis → psychic_shield
        // -----------------------------------------------------------------
        TabDefinition psychic = new TabDefinition(
                "Psychic",
                new ResourceLocation("psychic", "textures/item/liber_chaotica.png"),
                2048, 1024);

        // ── Mind Probe ────────────────────────────────────────────────────────
        psychic.addWidget(WidgetDefinition.simple(
                "mind_probe",
                new ResourceLocation("minecraft", "textures/item/compass.png"),
                250, 200,
                "Mind Probe",
                PopupContent.page(
                        PopupContent.text(
                                "Reveals the health, armor, and active\neffects of the targeted entity.\n\n" +
                                        "Range: 32 blocks\nCost: 10 mana"
                        ),
                        PopupContent.entityRender("minecraft:enderman", "Enderman — probe 3")
                ),
                null,
                false,
                TaskRequirement.kill("minecraft:enderman", 3, "Endermen probed")
        ));

        // ── Telekinesis ───────────────────────────────────────────────────────
        // Three-page popup: overview, mechanics, unlock requirement.
        psychic.addWidget(WidgetDefinition.simple(
                "telekinesis",
                new ResourceLocation("minecraft", "textures/item/writable_book.png"),
                650, 150,
                "Telekinesis",
                PopupContent.titledPages(
                        new String[]{ "Overview", "Mechanics", "Unlock" },
                        new List[]{
                                List.of(
                                        PopupContent.text("Channel psychic force to pull objects\ntoward you from a distance.")
                                ),
                                List.of(
                                        PopupContent.list(
                                                "Pull item or mob from up to 20 blocks",
                                                "Heavier mobs require more mana",
                                                "Cannot pull bosses",
                                                "Cost: 15\u201350 mana depending on mass"
                                        )
                                ),
                                List.of(
                                        PopupContent.text("The chorus fruit teaches the\nessence of teleportation."),
                                        PopupContent.itemRender("minecraft:chorus_fruit", "Chorus Fruit — collect 1")
                                )
                        }
                ),
                new String[]{ "mind_probe" },
                false,
                TaskRequirement.item("minecraft:chorus_fruit", 1, "Chorus fruit obtained")
        ));

        // ── Psychic Shield ────────────────────────────────────────────────────
        // Requires confirmation.
        psychic.addWidget(WidgetDefinition.simple(
                "psychic_shield",
                new ResourceLocation("minecraft", "textures/item/amethyst_shard.png"),
                1100, 300,
                "Psychic Shield",
                PopupContent.page(
                        PopupContent.text(
                                "Creates a brief psychic barrier that\nnegates the next hit within 1 second.\n\n" +
                                        "Cost: 25 mana\nCooldown: 6s"
                        ),
                        PopupContent.entityRender("minecraft:amethyst_cluster", "Amethyst Cluster — break 20"),
                        PopupContent.itemRender("minecraft:amethyst_shard",    "Amethyst Shard — collect 3")
                ),
                new String[]{ "telekinesis" },
                true, // confirm checkbox
                TaskRequirement.kill("minecraft:amethyst_cluster", 20, "Amethyst clusters"),
                TaskRequirement.item("minecraft:amethyst_shard",    3,  "Amethyst shards")
        ));

        TABS.add(psychic);

        // -----------------------------------------------------------------
        // Tab 3: Warp — chaos-star layout, same as before
        // (using the legacy infoWithTasks helper for brevity)
        // -----------------------------------------------------------------
        TabDefinition warp = new TabDefinition(
                "Warp",
                new ResourceLocation("psychic", "textures/gui/widgets/fills/chaos_star.png"),
                1536, 1024);

        warp.addWidget(WidgetDefinition.info(
                "center",
                new ResourceLocation("psychic", "textures/gui/widgets/fills/chaos_star.png"),
                0, 0,
                "The Warp",
                "Reveals the health, armor, and active effects\nof the targeted entity.\n\n" +
                        "Range: 32 blocks\nCost: 10 mana",
                48, 48
        ));

        for (String[] entry : new String[][]{
                { "thymos0",   "thymos_symbol",   "-70", "-70", "Thymos"   },
                { "energia0",  "energia_symbol",   "70", "-70", "Energia"  },
                { "mousike0",  "mousike_symbol",   "70",  "70", "Mousike"  },
                { "kyklos0",   "kyklos_symbol",   "-70",  "70", "Kyklos"   },
                { "phthora0",  "phthora_symbol",    "0", "-100", "Phthora"  },
                { "metabole0", "metabole_symbol", "-100",  "0", "Metabole" },
                { "misos0",    "misos_symbol",      "0", "100", "Misos"    },
                { "techne0",   "techne_symbol",   "100",  "0", "Techne"   },
        }) {
            warp.addWidget(WidgetDefinition.infoWithTasks(
                    entry[0],
                    new ResourceLocation("psychic", "textures/gui/widgets/fills/" + entry[1] + ".png"),
                    Integer.parseInt(entry[2]), Integer.parseInt(entry[3]),
                    entry[4],
                    "Reveals the health, armor, and active effects\nof the targeted entity.\n\n" +
                            "Range: 32 blocks\nCost: 10 mana",
                    new String[]{ "center" },
                    TaskRequirement.kill("minecraft:enderman", 3, "Endermen probed")
            ));
        }

        warp.addDecorater(new DecoraterDefinition(
                new ResourceLocation("psychic", "textures/gui/widgets/fills/chaos_star_high_res.png"),
                0, 0, 200, 200
        ));

        TABS.add(warp);
    }

    public static List<TabDefinition> getTabs() {
        return Collections.unmodifiableList(TABS);
    }
}