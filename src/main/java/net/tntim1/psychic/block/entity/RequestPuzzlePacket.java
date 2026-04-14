package net.tntim1.psychic.block.entity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.tntim1.psychic.capability.PsychicData;
import net.tntim1.psychic.network.ModPackets;

import java.util.List;
import java.util.function.Supplier;

public class RequestPuzzlePacket {
    // Required for decoding
    public RequestPuzzlePacket(FriendlyByteBuf buf) {}

    // Required for initial creation
    public RequestPuzzlePacket() {}

    // Required for encoding
    public void toBytes(FriendlyByteBuf buf) {}

    public static void handle(RequestPuzzlePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            PsychicData data = PsychicData.get(player);
            List<String> allSpells = List.of("fire_beam", "ice_shard", "chain_lightning");
            List<String> available = allSpells.stream().filter(s -> !data.isUnlocked(s)).toList();

            if (available.isEmpty()) {
                ModPackets.sendToPlayer(new NoPuzzlePacket(), player);
            } else {
                String spell = available.get(player.getRandom().nextInt(available.size()));
                int difficulty = player.getRandom().nextInt(3);
                ModPackets.sendToPlayer(new StartPuzzlePacket(spell, difficulty), player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}