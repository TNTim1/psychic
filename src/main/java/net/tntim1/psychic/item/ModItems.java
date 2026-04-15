package net.tntim1.psychic.item;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.Item;
import net.tntim1.psychic.Psychic;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Psychic.MODID);

    /** The Atlas Codex item — right-click to open the tabbed codex GUI. */
    public static final RegistryObject<Item> LIBER_CHAOTICA =
            ITEMS.register("liber_chaotica", LiberChaotica::new);
    public static final RegistryObject<Item> ENERGIA_ESSENCE =
            ITEMS.register("energia_essence", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> KYKLOS_ESSENCE =
            ITEMS.register("kyklos_essence", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> METABOLE_ESSENCE =
            ITEMS.register("metabole_essence", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MISOS_ESSENCE =
            ITEMS.register("misos_essence", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MOUSIKE_ESSENCE =
            ITEMS.register("mousike_essence", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PHTHORA_ESSENCE =
            ITEMS.register("phthora_essence", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TECHNE_ESSENCE =
            ITEMS.register("techne_essence", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> THYMOS_ESSENCE =
            ITEMS.register("thymos_essence", () -> new Item(new Item.Properties()));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
