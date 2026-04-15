package net.tntim1.psychic.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.tntim1.psychic.UI.CastingUi;

import java.util.function.Supplier;

public class WarpSyncPacket {
    private final int warp;

    public WarpSyncPacket(int warp) {
        this.warp = warp;
    }

    public static void encode(WarpSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.warp);
    }

    public static WarpSyncPacket decode(FriendlyByteBuf buf) {
        return new WarpSyncPacket(buf.readInt());
    }

    public static void handle(WarpSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof CastingUi ui) {
                ui.setWarpStrength(msg.warp);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}