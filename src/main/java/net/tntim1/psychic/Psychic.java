package net.tntim1.psychic;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.tntim1.psychic.Spells.WorldSpellData;
import net.tntim1.psychic.UI.CastingUi;
import net.tntim1.psychic.item.ModItems;           // ← NEW
import net.tntim1.psychic.screen.AtlasCodexScreen; // ← NEW (keybind alternative, not used here)
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

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

        // ── Atlas Codex: register items ───────────────────────────────────
        ModItems.register(modEventBus);
        // ─────────────────────────────────────────────────────────────────

        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {}

    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        // ── Atlas Codex: add to your existing creative tab if desired ────
        // event.accept(ModItems.ATLAS_CODEX);
        // ─────────────────────────────────────────────────────────────────
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {}

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

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {}
}
