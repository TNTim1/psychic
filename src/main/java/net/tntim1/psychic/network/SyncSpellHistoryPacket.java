
package net.tntim1.psychic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.tntim1.psychic.player_data.ClientKnowledge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record SyncSpellHistoryPacket(List<String> orderedSpells) {

    public static void encode(SyncSpellHistoryPacket msg, FriendlyByteBuf buffer) {
        buffer.writeCollection(msg.orderedSpells, FriendlyByteBuf::writeUtf);
    }

    public static SyncSpellHistoryPacket decode(FriendlyByteBuf buffer) {
        return new SyncSpellHistoryPacket(buffer.readCollection(ArrayList::new, FriendlyByteBuf::readUtf));
    }

    public static void handle(SyncSpellHistoryPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientKnowledge.setUnlockOrder(pkt.orderedSpells);
        });
        ctx.get().setPacketHandled(true);
    }
}