package net.tntim1.psychic.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.tntim1.psychic.Psychic;
import net.tntim1.psychic.block.ModBlocks;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Psychic.MODID);

    public static final RegistryObject<CreativeModeTab> PSYCHIC_TAB = CREATIVE_MODE_TABS.register("psychic_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.LIBER_CHAOTICA.get())) // The "face" of the tab
                    .title(Component.translatable("creativetab.psychic_tab"))
                    .displayItems((parameters, output) -> {
                        // Add your items here in the order you want them to appear
                        output.accept(ModItems.LIBER_CHAOTICA.get());
                        output.accept(ModItems.PHTHORA_ESSENCE.get());
                        output.accept(ModItems.KYKLOS_ESSENCE.get());
                        output.accept(ModItems.METABOLE_ESSENCE.get());
                        output.accept(ModItems.MOUSIKE_ESSENCE.get());
                        output.accept(ModItems.MISOS_ESSENCE.get());
                        output.accept(ModItems.ENERGIA_ESSENCE.get());
                        output.accept(ModItems.TECHNE_ESSENCE.get());
                        output.accept(ModItems.THYMOS_ESSENCE.get());
                        output.accept(ModBlocks.RESEARCH_TABLE.get());
                        output.accept(ModItems.AETHER_PIPE_ITEM.get());
                        output.accept(ModItems.AETHER_TANK_ITEM.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}