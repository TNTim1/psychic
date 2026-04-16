package net.tntim1.psychic.block;


import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.tntim1.psychic.Psychic;
import net.tntim1.psychic.item.ModItems;
import java.util.function.Supplier;


public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Psychic.MODID);

    public static final RegistryObject<Block> RESEARCH_TABLE = registerBlock("research_table",
            () -> new ResearchTableBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
    // ModBlocks.java additions
    public static final RegistryObject<Block> AETHER_TANK = BLOCKS.register("aether_tank",
            () -> new AetherTankBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0f)
                    .noOcclusion() // <--- CRITICAL: This prevents X-raying
                    .isValidSpawn((state, getter, pos, type) -> false) // Prevent mobs spawning on glass
            ));

    public static final RegistryObject<Block> AETHER_PIPE =
            BLOCKS.register("aether_pipe", () ->
                    new AetherPipeBlock(BlockBehaviour.Properties.of()
                            .strength(1.5f).requiresCorrectToolForDrops()));


    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}