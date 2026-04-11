package net.tntim1.psychic;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
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
import net.tntim1.psychic.item.ModItems;
import net.tntim1.psychic.network.ModPackets;
import net.tntim1.psychic.player_data.PsychicData;
import org.slf4j.Logger;

@Mod(Psychic.MODID)
public class Psychic
{
    public static final String MODID = "psychic";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Capability<PsychicData> PSYCHIC_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});

    public Psychic(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(KeyInit::register);
        modEventBus.addListener(this::addCreative);

        ModItems.register(modEventBus);

        // We register this class to the Forge bus for general events (Commands, etc.)
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
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {}
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientForgeEvents
    {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event)
        {
            Minecraft mc = Minecraft.getInstance();
            if (event.phase == TickEvent.Phase.END && mc.screen == null) {
                while (KeyInit.exampleHotKey != null && KeyInit.exampleHotKey.consumeClick()) {
                    mc.setScreen(new CastingUi());
                }
            }
        }
    }
}