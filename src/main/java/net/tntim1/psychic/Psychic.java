package net.tntim1.psychic;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
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
import org.slf4j.Logger;

@Mod(Psychic.MODID)
public class Psychic
{
    public static final String MODID = "psychic";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Psychic(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(KeyInit::register);

        ModItems.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        // PsychicCapabilityEventHandler uses @Mod.EventBusSubscriber on both
        // its inner classes, so Forge discovers and wires them automatically.
        // Do NOT manually register it here — that would double-fire events.

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Capability registration moved to RegisterCapabilitiesEvent
            // in PsychicCapabilityEventHandler.ModBusEvents — nothing to call here.
            ModPackets.register();
        });
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