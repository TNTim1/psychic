package net.tntim1.psychic.item;

import net.minecraft.world.item.BlockItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.Item;
import net.tntim1.psychic.Psychic;
import net.tntim1.psychic.block.ModBlocks;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Psychic.MODID);

    /** The Atlas Codex item — right-click to open the tabbed codex GUI. */
    public static final RegistryObject<Item> LIBER_CHAOTICA =
            ITEMS.register("liber_chaotica", LiberChaotica::new);
    public static final RegistryObject<Item> ENERGIA_ESSENCE =
            ITEMS.register("energia_essence", () -> new EssenceItem(0));
    public static final RegistryObject<Item> KYKLOS_ESSENCE =
            ITEMS.register("kyklos_essence", () -> new EssenceItem(1));
    public static final RegistryObject<Item> METABOLE_ESSENCE =
            ITEMS.register("metabole_essence", () -> new EssenceItem(2));
    public static final RegistryObject<Item> MISOS_ESSENCE =
            ITEMS.register("misos_essence", () -> new EssenceItem(3));
    public static final RegistryObject<Item> MOUSIKE_ESSENCE =
            ITEMS.register("mousike_essence", () -> new EssenceItem(4));
    public static final RegistryObject<Item> PHTHORA_ESSENCE =
            ITEMS.register("phthora_essence", () -> new EssenceItem(5));
    public static final RegistryObject<Item> TECHNE_ESSENCE =
            ITEMS.register("techne_essence", () -> new EssenceItem(6));
    public static final RegistryObject<Item> THYMOS_ESSENCE =
            ITEMS.register("thymos_essence", () -> new EssenceItem(7));
    public static final RegistryObject<Item> AETHER_TANK_ITEM =
            ITEMS.register("aether_tank", () ->
                    new BlockItem(ModBlocks.AETHER_TANK.get(), new Item.Properties()));

    public static final RegistryObject<Item> AETHER_PIPE_ITEM =
            ITEMS.register("aether_pipe", () ->
                    new BlockItem(ModBlocks.AETHER_PIPE.get(), new Item.Properties()));


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
