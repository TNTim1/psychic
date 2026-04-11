package net.tntim1.psychic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.tntim1.psychic.player_data.ClientKnowledge;

import java.util.function.Supplier;

public class WidgetAutoActivatePacket {

    private final String widgetId;

    public WidgetAutoActivatePacket(String widgetId) { this.widgetId = widgetId; }

    public static void encode(WidgetAutoActivatePacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.widgetId);
    }

    public static WidgetAutoActivatePacket decode(FriendlyByteBuf buf) {
        return new WidgetAutoActivatePacket(buf.readUtf());
    }

    public static void handle(WidgetAutoActivatePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientKnowledge.unlock(pkt.widgetId);
            // Queue the "UNLOCKED" toast — rendered by HudOverlay
            ClientKnowledge.queueUnlockToast(pkt.widgetId);
        });
        ctx.get().setPacketHandled(true);
    }
}