package net.tntim1.psychic;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.tntim1.psychic.Keybinds.KeyInit;
import net.tntim1.psychic.UI.CastingUi;
import net.tntim1.psychic.block.ModBlocks;
import net.tntim1.psychic.block.entity.AetherTankRenderer;
import net.tntim1.psychic.block.entity.ModBlockEntities;
import net.tntim1.psychic.block.entity.ModMenus;
import net.tntim1.psychic.block.entity.ResearchTableScreen;
import net.tntim1.psychic.capability.PsychicCapability;
import net.tntim1.psychic.fluids.ModFluids;
import net.tntim1.psychic.item.ModCreativeModeTabs;
import net.tntim1.psychic.item.ModItems;
import net.tntim1.psychic.network.*;
import net.tntim1.psychic.player_data.ClientKnowledge;
import net.tntim1.psychic.capability.PsychicData;
import org.slf4j.Logger;

@Mod(Psychic.MODID)
public class Psychic
{
    public static final String MODID = "psychic";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Capability<PsychicData> PSYCHIC_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    public Psychic(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModCreativeModeTabs.register(modEventBus);

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus); // Add this
        ModBlockEntities.register(modEventBus); // Add this
        ModMenus.register(modEventBus); // Add this
        ModFluids.FLUIDS.register(modEventBus);
        ModFluids.FLUID_TYPES.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(KeyInit::register);
        modEventBus.addListener(this::addCreative);

        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModPackets::register);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {}

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {}

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // Access the dispatcher from the event object
        event.getDispatcher().register(Commands.literal("psychic")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("unlockspell")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("spellId", StringArgumentType.string())
                                        .executes(context -> {
                                            ServerPlayer player = EntityArgument.getPlayer(context, "target");
                                            String spellId = StringArgumentType.getString(context, "spellId");

                                            // 1. Persist to server-side player data (this is where you call your capability)
                                            player.getCapability(PsychicCapability.PSYCHIC_DATA_CAP).ifPresent(data -> {
                                                data.unlockSpell(spellId, player);
                                                // This method (in PsychicData) MUST send the SyncSpellHistoryPacket
                                            });

                                            context.getSource().sendSuccess(() ->
                                                    Component.literal("Unlocked " + spellId + " for " + player.getScoreboardName()), true);
                                            return 1;
                                        })
                                )
                        )
                )
        );
    }
    @SubscribeEvent
    public void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) { // Removed static
        ClientKnowledge.resetClientData();
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) { // Removed static
        if (event.getEntity() instanceof ServerPlayer player) {
            PsychicData data = PsychicData.get(player);
            ModPackets.sendToPlayer(new SyncTaskProgressPacket(data.taskProgress.snapshot()), player);
            ModPackets.sendToPlayer(new SyncKnowledgePacket(data.getUnlockedIds()), player);
            ModPackets.sendToPlayer(new SyncSpellHistoryPacket(data.getUnlockedSpellsOrder()), player);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                // Links the Menu logic to the visual Screen
                MenuScreens.register(ModMenus.RESEARCH_TABLE_MENU.get(), ResearchTableScreen::new);

            });

        }
        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(ModBlockEntities.AETHER_TANK.get(), AetherTankRenderer::new);
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.AETHER_TANK.get(), RenderType.translucent());
        }
    }


    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientForgeEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            Minecraft mc = Minecraft.getInstance();

            // Only run on the END phase and when no other screen is open
            if (event.phase == TickEvent.Phase.END && mc.screen == null && mc.player != null) {

                while (KeyInit.castingKey != null && KeyInit.castingKey.consumeClick()) {
                    // Fetch the capability from the local player
                    mc.player.getCapability(Psychic.PSYCHIC_DATA).ifPresent(data -> {

                        // Replace "psychic_awakening" with the ID of your starter WidgetDefinition
                        if (ClientKnowledge.isUnlocked("psychic_awakening")) {
                            mc.setScreen(new CastingUi());
                        } else {
                            // Optional: Provide feedback so the player knows why it won't open
                            mc.player.displayClientMessage(
                                    net.minecraft.network.chat.Component.literal("Your psychic powers are still dormant..."),
                                    true
                            );
                        }
                    });
                }
            }
        }
    }

}