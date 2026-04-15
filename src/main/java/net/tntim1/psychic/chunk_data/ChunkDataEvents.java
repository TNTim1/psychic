package net.tntim1.psychic.chunk_data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkWatchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.tntim1.psychic.network.ModPackets;
import net.tntim1.psychic.network.WarpSyncPacket;

@Mod.EventBusSubscriber(modid = "psychic")
public class ChunkDataEvents {
    @SubscribeEvent
    public static void attachChunkCapabilities(AttachCapabilitiesEvent<LevelChunk> event) {
        event.addCapability(
                new ResourceLocation("psychic", "chunk_warp"),
                new ChunkWarpProvider()
        );
    }
    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        ServerPlayer player = event.getPlayer();
        LevelChunk chunk = event.getChunk();

        chunk.getCapability(ChunkWarpProvider.CAP).ifPresent(data -> {
            ModPackets.sendToPlayer(new WarpSyncPacket(data.getWarpStrength()), player);
        });
    }
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LevelChunk chunk = player.serverLevel().getChunkAt(player.blockPosition());

            chunk.getCapability(ChunkWarpProvider.CAP).ifPresent(data -> {
                ModPackets.sendToPlayer(new WarpSyncPacket(data.getWarpStrength()), player);
            });
        }
    }
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END &&
                event.player instanceof ServerPlayer player) {

            if (player.tickCount % 20 == 0) {
                LevelChunk chunk = player.serverLevel().getChunkAt(player.blockPosition());

                chunk.getCapability(ChunkWarpProvider.CAP).ifPresent(data -> {
                    ModPackets.sendToPlayer(new WarpSyncPacket(data.getWarpStrength()), player);
                });
            }
        }
    }
}
