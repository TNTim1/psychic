package net.tntim1.psychic.widget;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central registry for all Atlas Codex tabs.
 *
 * <p><b>To add a tab or widget, edit the static block at the bottom of this file.</b>
 * No other files need to be touched.
 *
 * <p>Tabs appear left-to-right across the top bar in registration order.
 */
public class TabRegistry {

    private static final List<TabDefinition> TABS = new ArrayList<>();

    // =========================================================================
    // TABS — edit / extend freely below
    // =========================================================================
    static {

        // -----------------------------------------------------------------
        // Tab 1: Spells
        // -----------------------------------------------------------------
        TabDefinition spells = new TabDefinition("Spells", 2048, 1024);

        spells.addWidget(WidgetDefinition.info(
            new ResourceLocation("minecraft", "textures/item/blaze_powder.png"),
            200, 150,
            "Fireball",
            "Launches a concentrated fireball in the direction you face.\n\n" +
            "Cost: 30 mana\nCooldown: 2s\nDamage: 8 ❤"
        ));

        spells.addWidget(WidgetDefinition.list(
            new ResourceLocation("minecraft", "textures/item/ender_pearl.png"),
            550, 280,
            "Blink",
            "Instantly teleport up to 16 blocks forward",
            "Passes through solid blocks",
            "Cost: 20 mana | Cooldown: 5s",
            "Cannot blink into the void"
        ));

        spells.addWidget(WidgetDefinition.info(
            new ResourceLocation("minecraft", "textures/item/snowball.png"),
            900, 180,
            "Frost Nova",
            "Freezes all mobs within 5 blocks.\n\nCost: 45 mana\nDuration: 3s\nCooldown: 8s"
        ));

        spells.addWidget(WidgetDefinition.list(
            new ResourceLocation("minecraft", "textures/item/lightning_rod.png"),
            1300, 350,
            "Chain Lightning",
            "Strikes nearest mob with lightning",
            "Chains up to 4 additional targets",
            "Damage reduces 20% per hop",
            "Cost: 60 mana | Cooldown: 10s"
        ));

        TABS.add(spells);

        // -----------------------------------------------------------------
        // Tab 2: Psychic Powers
        // -----------------------------------------------------------------
        TabDefinition psychic = new TabDefinition("Psychic", 2048, 1024);

        psychic.addWidget(WidgetDefinition.info(
            new ResourceLocation("minecraft", "textures/item/compass.png"),
            250, 200,
            "Mind Probe",
            "Reveals the health, armor, and active effects\nof the targeted entity.\n\nRange: 32 blocks\nCost: 10 mana"
        ));

        psychic.addWidget(WidgetDefinition.list(
            new ResourceLocation("minecraft", "textures/item/writable_book.png"),
            650, 150,
            "Telekinesis",
            "Pull an item or mob toward you from up to 20 blocks",
            "Heavier mobs require more mana",
            "Cannot pull bosses",
            "Cost: 15–50 mana depending on mass"
        ));

        psychic.addWidget(WidgetDefinition.info(
            new ResourceLocation("minecraft", "textures/item/amethyst_shard.png"),
            1100, 300,
            "Psychic Shield",
            "Creates a brief psychic barrier that negates\nthe next hit within 1 second.\n\nCost: 25 mana\nCooldown: 6s"
        ));

        TABS.add(psychic);

        // -----------------------------------------------------------------
        // Tab 3: Lore
        // -----------------------------------------------------------------
        TabDefinition lore = new TabDefinition("Lore", 1536, 1024);

        lore.addWidget(WidgetDefinition.info(
            new ResourceLocation("minecraft", "textures/item/book.png"),
            200, 200,
            "Origin of Psychic Arts",
            "Long before the First Age, wandering scholars\ndiscovered that emotion itself could be weaponised.\n\n" +
            "They called the discipline 'Psychica' — the art\nof bending reality with thought alone."
        ));

        lore.addWidget(WidgetDefinition.list(
            new ResourceLocation("minecraft", "textures/item/map.png"),
            700, 300,
            "Known Factions",
            "The Veil — seekers of forbidden knowledge",
            "Iron Conclave — suppressors of psychic power",
            "Ember Circle — pyromancy & psychic fusion",
            "The Unbound — rogue practitioners"
        ));

        lore.addWidget(WidgetDefinition.info(
            new ResourceLocation("minecraft", "textures/item/clock.png"),
            1150, 180,
            "Timeline",
            "Year 0   — Psychica first recorded\nYear 340 — The Veil founded\nYear 612 — Iron Conclave wars\n" +
            "Year 890 — The Unbound Schism\nYear 1024 — Present day"
        ));

        TABS.add(lore);

        // =================================================================
        // ADD YOUR OWN TABS HERE:
        //
        // TabDefinition myTab = new TabDefinition("My Tab", 2048, 1024);
        //
        // myTab.addWidget(WidgetDefinition.info(
        //     new ResourceLocation("psychic", "textures/item/my_icon.png"),
        //     400, 300,
        //     "My Widget",
        //     "Popup text here.\nSupports newlines."
        // ));
        //
        // myTab.addWidget(WidgetDefinition.list(
        //     new ResourceLocation("minecraft", "textures/item/compass.png"),
        //     800, 250,
        //     "My List",
        //     "First entry",
        //     "Second entry"
        // ));
        //
        // TABS.add(myTab);
        // =================================================================
    }

    public static List<TabDefinition> getTabs() {
        return Collections.unmodifiableList(TABS);
    }
}
