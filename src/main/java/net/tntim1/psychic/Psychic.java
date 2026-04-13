package net.tntim1.psychic;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
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
import net.tntim1.psychic.block.entity.ModBlockEntities;
import net.tntim1.psychic.block.entity.ModMenus;
import net.tntim1.psychic.block.entity.ResearchTableScreen;
import net.tntim1.psychic.item.ModItems;
import net.tntim1.psychic.network.ModPackets;
import net.tntim1.psychic.player_data.ClientKnowledge;
import net.tntim1.psychic.player_data.PsychicData;
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

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus); // Add this
        ModBlockEntities.register(modEventBus); // Add this
        ModMenus.register(modEventBus); // Add this

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
    public void onRegisterCommands(RegisterCommandsEvent event) {}

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> {
                // Links the Menu logic to the visual Screen
                MenuScreens.register(ModMenus.RESEARCH_TABLE_MENU.get(), ResearchTableScreen::new);
            });
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