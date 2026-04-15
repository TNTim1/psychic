package net.tntim1.psychic.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.tntim1.psychic.UI.CastingUi;

import java.util.function.Supplier;

public class ManaSpendResultPacket {
    private final boolean success;

    public ManaSpendResultPacket(boolean success) { this.success = success; }

    public static void encode(ManaSpendResultPacket msg, FriendlyByteBuf buf) { buf.writeBoolean(msg.success); }
    public static ManaSpendResultPacket decode(FriendlyByteBuf buf) { return new ManaSpendResultPacket(buf.readBoolean()); }

    public static void handle(ManaSpendResultPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof CastingUi ui) {
                if (msg.success) {
                    ui.confirmRhythmStart();
                } else {
                    ui.cancelRhythmStart();
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}