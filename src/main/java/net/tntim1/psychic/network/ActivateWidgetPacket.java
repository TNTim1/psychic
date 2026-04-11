package net.tntim1.psychic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.tntim1.psychic.capability.PsychicCapability;

import java.util.function.Supplier;

/**
 * Sent from the client to the server when the player clicks ACTIVATE on a widget popup.
 *
 * <p>The server validates the request (add your own gate logic here), then:
 * <ol>
 *   <li>Unlocks the widget in the player's {@link net.tntim1.psychic.player_data.PsychicData}</li>
 *   <li>Saves it via the capability</li>
 *   <li>Sends a {@link SyncKnowledgePacket} back so the client GUI updates immediately</li>
 * </ol>
 */
public class ActivateWidgetPacket {

    private final String widgetId;

    public ActivateWidgetPacket(String widgetId) {
        this.widgetId = widgetId;
    }

    // ── serialisation ─────────────────────────────────────────────────────────

    public static void encode(ActivateWidgetPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.widgetId);
    }

    public static ActivateWidgetPacket decode(FriendlyByteBuf buf) {
        return new ActivateWidgetPacket(buf.readUtf());
    }

    // ── server-side handler ───────────────────────────────────────────────────

    public static void handle(ActivateWidgetPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(PsychicCapability.PSYCHIC_DATA_CAP).ifPresent(data -> {

                // ── GATE CHECK ────────────────────────────────────────────────
                // Insert your requirements here before allowing the unlock.
                // Examples:
                //   if (!data.hasPrerequisite(msg.widgetId)) return;
                //   if (data.getMana() < cost) return;
                // For now every click unlocks freely.
                // ─────────────────────────────────────────────────────────────

                data.unlock(msg.widgetId);

                // Push the updated set back to this client
                SyncKnowledgePacket.sendToPlayer(player, data);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}