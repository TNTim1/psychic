package net.tntim1.psychic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.tntim1.psychic.player_data.ClientKnowledge;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class SyncTaskProgressPacket {

    private final Map<String, Integer> snapshot;

    public SyncTaskProgressPacket(Map<String, Integer> snapshot) {
        this.snapshot = snapshot;
    }

    public static void encode(SyncTaskProgressPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.snapshot.size());
        pkt.snapshot.forEach((k, v) -> { buf.writeUtf(k); buf.writeInt(v); });
    }

    public static SyncTaskProgressPacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < size; i++) map.put(buf.readUtf(), buf.readInt());
        return new SyncTaskProgressPacket(map);
    }

    public static void handle(SyncTaskProgressPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientKnowledge.applyTaskProgressSnapshot(pkt.snapshot));
        ctx.get().setPacketHandled(true);
    }
}