package net.tntim1.psychic.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tntim1.psychic.network.SyncKnowledgePacket;
import net.tntim1.psychic.player_data.PsychicData;

/**
 * Handles lifecycle events for the PsychicData capability.
 *
 * <p>This file contains TWO inner classes / event bus subscribers:
 * <ul>
 *   <li>{@link ModBusEvents}  — MOD bus:   registers the capability type</li>
 *   <li>{@link ForgeBusEvents} — FORGE bus: attaches, clones, syncs per-player data</li>
 * </ul>
 *
 * Register both in your mod constructor:
 * <pre>{@code
 *   IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
 *   modBus.register(PsychicCapabilityEventHandler.ModBusEvents.class);
 *   MinecraftForge.EVENT_BUS.register(PsychicCapabilityEventHandler.ForgeBusEvents.class);
 * }</pre>
 */
public class PsychicCapabilityEventHandler {

    // ── MOD BUS: capability registration ─────────────────────────────────────

    @Mod.EventBusSubscriber(modid = "psychic", bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {

        /**
         * 1.20.1 Forge requires capabilities to be registered here instead of
         * via the old {@code CapabilityManager.INSTANCE.register()} / {@code @CapabilityInject}.
         */
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.register(PsychicData.class);
        }
    }

    // ── FORGE BUS: per-player lifecycle ──────────────────────────────────────

    @Mod.EventBusSubscriber(modid = "psychic", bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ForgeBusEvents {

        private static final ResourceLocation CAP_KEY = new ResourceLocation("psychic", "psychic_data");

        @SubscribeEvent
        public static void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
            if (!(event.getObject() instanceof Player)) return;

            PsychicCapability.Provider provider = new PsychicCapability.Provider();
            event.addCapability(CAP_KEY, provider);
            event.addListener(provider::invalidate);
        }

        @SubscribeEvent
        public static void onPlayerClone(PlayerEvent.Clone event) {
            event.getOriginal().reviveCaps();

            event.getOriginal().getCapability(PsychicCapability.PSYCHIC_DATA_CAP).ifPresent(oldData ->
                    event.getEntity().getCapability(PsychicCapability.PSYCHIC_DATA_CAP).ifPresent(newData ->
                            newData.copyFrom(oldData)
                    )
            );

            event.getOriginal().invalidateCaps();
        }

        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            syncToClient(event.getEntity());
        }

        @SubscribeEvent
        public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
            syncToClient(event.getEntity());
        }

        private static void syncToClient(Player player) {
            if (!(player instanceof ServerPlayer sp)) return;
            sp.getCapability(PsychicCapability.PSYCHIC_DATA_CAP).ifPresent(data ->
                    SyncKnowledgePacket.sendToPlayer(sp, data)
            );
        }
    }
}