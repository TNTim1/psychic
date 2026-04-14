package net.tntim1.psychic.network;

import net.minecraftforge.network.NetworkEvent;
import net.tntim1.psychic.player_data.ClientKnowledge;

import java.util.function.Supplier;

public class SyncSpellUnlockPacket {
    private final String spellId;

    public SyncSpellUnlockPacket(String id) { this.spellId = id; }

    // Logic to run on the Client
    public static void handle(SyncSpellUnlockPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // This is the local map your UI reads from
            ClientKnowledge.unlock(pkt.spellId);
        });
        ctx.get().setPacketHandled(true);
    }
}