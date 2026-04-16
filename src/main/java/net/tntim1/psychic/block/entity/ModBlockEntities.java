package net.tntim1.psychic.block.entity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.tntim1.psychic.block.ModBlocks;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, "psychic");

    public static final RegistryObject<BlockEntityType<ResearchTableBlockEntity>> RESEARCH_TABLE =
            BLOCK_ENTITIES.register("research_table", () ->
                    BlockEntityType.Builder.of(ResearchTableBlockEntity::new,
                            ModBlocks.RESEARCH_TABLE.get()).build(null));
    public static final RegistryObject<BlockEntityType<AetherTankBlockEntity>> AETHER_TANK =
            BLOCK_ENTITIES.register("aether_tank", () ->
                    BlockEntityType.Builder
                            .of(AetherTankBlockEntity::new, ModBlocks.AETHER_TANK.get())
                            .build(null));

    public static final RegistryObject<BlockEntityType<AetherPipeBlockEntity>> AETHER_PIPE =
            BLOCK_ENTITIES.register("aether_pipe", () ->
                    BlockEntityType.Builder
                            .of(AetherPipeBlockEntity::new, ModBlocks.AETHER_PIPE.get())
                            .build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}