package net.tntim1.psychic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.tntim1.psychic.capability.PsychicCapability;

import java.util.function.Supplier;

/**
 * Sent from the client to the server to lock (deactivate) a widget.
 *
 * <p>Cascades: every widget that depends on this one (directly or transitively)
 * is also locked. The updated state is synced back to the client immediately.
 *
 * <p>Typical use: an admin button in the popup, or a command that revokes a power.
 * You can also call this server-side directly via
 * {@code data.lockCascading(id); SyncKnowledgePacket.sendToPlayer(player, data);}.
 */
public class DeactivateWidgetPacket {

    private final String widgetId;

    public DeactivateWidgetPacket(String widgetId) {
        this.widgetId = widgetId;
    }

    public static void encode(DeactivateWidgetPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.widgetId);
    }

    public static DeactivateWidgetPacket decode(FriendlyByteBuf buf) {
        return new DeactivateWidgetPacket(buf.readUtf());
    }

    public static void handle(DeactivateWidgetPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(PsychicCapability.PSYCHIC_DATA_CAP).ifPresent(data -> {
                // lockCascading removes this widget AND all downstream dependents
                data.lockCascading(msg.widgetId);
                SyncKnowledgePacket.sendToPlayer(player, data);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}