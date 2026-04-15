package net.tntim1.psychic.widget;

import net.minecraft.resources.ResourceLocation;
import net.tntim1.psychic.Keybinds.KeyInit;
import net.tntim1.psychic.Spells.SpellAction;
import net.tntim1.psychic.Spells.SpellDefinition;
import net.tntim1.psychic.Spells.SpellRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TabRegistry {

    private static final List<TabDefinition> TABS = new ArrayList<>();
    public static SpellWidgetDefinition findSpellById(String id) {
        for (TabDefinition tab : TABS) {
            for (SpellWidgetDefinition s : tab.spells) {
                if (s.id.equals(id)) {
                    return s;
                }
            }
        }
        return null;
    }

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
                PopupContent.page(
                        PopupContent.pml("""
            <text size="lg" bold color="gold">Fireball</text>
 
            <text margin-top="6">
              Launches a concentrated fireball in the direction you face.
            </text>
 
            <panel bg="dark" border="accent" corner="sharp" margin-top="8" padding="6">
              <text size="sm" color="dim">Spell statistics</text>
              <list bullet="dash" margin-top="4" indent="10">
                <item>Cost: <span color="mana" bold>30 mana</span></item>
                <item>Cooldown: <span bold>2 s</span></item>
                <item>Damage: <span color="red" bold>8 ❤</span></item>
              </list>
            </panel>
 
            <divider margin-top="10"/>
 
            <text size="sm" color="dim" margin-top="2" align="center">
              To unlock, you must prove mastery of fire.
            </text>
 
            <panel direction="horizontal" margin-top="8" gap="8">
              <render type="entity" id="minecraft:blaze"    size="40"
                      label="Blaze" label-side="below" bg="dark" border="accent"/>
              <render type="item"   id="minecraft:blaze_rod" size="32"
                      label="Blaze Rod" label-side="below" bg="dark" border="dim"/>
            </panel>
        """)
                ),
                null,   // no widget dependencies
                false,  // no confirmation required
                TaskRequirement.kill("minecraft:blaze",     5, "Blazes slain"),
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
        // ── Mind Probe / Psychic Awakening ────────────────────────────────────────
        psychic.addWidget(WidgetDefinition.simple(
                "psychic_awakening",
                new ResourceLocation("psychic", "textures/item/liber_chaotica.png"),
                0, 0,
                "Psychic Awakening",
                PopupContent.page(
                        PopupContent.text(
                                "To use psychic abilities you first have to discover spells using the research table. " +
                                        "Once unlocked, press [" + KeyInit.castingKey.getTranslatedKeyMessage().getString() + "] to cast them."
                        )
                ),
                null, // No dependencies
                false // No confirmation checkbox
        ));


        for (String key : SpellRegistry.SPELLS.keySet()) {
            SpellDefinition spell = SpellRegistry.get(key);
            psychic.addSpell(new SpellWidgetDefinition(
                    key, spell.title, spell.description,  spell.displayPattern, spell.texture,40

                    )
            );
        }



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
                "The Warp, a world of chaos separated from the overworld by a thin veil. This veil is stronger in some places than others, and exessive use of magic can weaken it causing various side effects. The Warp is a dangerous place with demons, gods and an unpredictable landscape. It is primarily built out of 8 different lairs each with its own demons forms gods and things for you to conquer or become.",
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