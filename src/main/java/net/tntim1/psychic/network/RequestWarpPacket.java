package net.tntim1.psychic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.NetworkEvent;
import net.tntim1.psychic.chunk_data.ChunkWarpProvider;

import java.util.function.Supplier;

public class RequestWarpPacket {

    public RequestWarpPacket() {}

    public static void encode(RequestWarpPacket msg, FriendlyByteBuf buf) {}

    public static RequestWarpPacket decode(FriendlyByteBuf buf) {
        return new RequestWarpPacket();
    }

    public static void handle(RequestWarpPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            LevelChunk chunk = player.serverLevel().getChunkAt(player.blockPosition());

            chunk.getCapability(ChunkWarpProvider.CAP).ifPresent(data -> {
                ModPackets.sendToPlayer(
                        new WarpSyncPacket(data.getWarpStrength()),
                        player
                );
            });
        });
        ctx.get().setPacketHandled(true);
    }
}