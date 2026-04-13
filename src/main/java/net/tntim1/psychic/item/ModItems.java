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
    public static final RegistryObject<Item> ATLAS_CODEX =
            ITEMS.register("liber_chaotica", LiberChaotica::new);


    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
