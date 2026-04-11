package net.tntim1.psychic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.tntim1.psychic.capability.PsychicCapability;

import java.util.function.Supplier;

/**
 * Sent from the client to the server when the player clicks ACTIVATE on a widget popup.
 *
 * <p>The server:
 * <ol>
 *   <li>Verifies all dependencies are unlocked via {@code PsychicData.areDependenciesMet()}</li>
 *   <li>Unlocks the widget</li>
 *   <li>Sends a {@link SyncKnowledgePacket} back so the client GUI updates immediately</li>
 * </ol>
 */
public class ActivateWidgetPacket {

    private final String widgetId;

    public ActivateWidgetPacket(String widgetId) {
        this.widgetId = widgetId;
    }

    public static void encode(ActivateWidgetPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.widgetId);
    }

    public static ActivateWidgetPacket decode(FriendlyByteBuf buf) {
        return new ActivateWidgetPacket(buf.readUtf());
    }

    public static void handle(ActivateWidgetPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(PsychicCapability.PSYCHIC_DATA_CAP).ifPresent(data -> {

                // ── DEPENDENCY GATE (authoritative server check) ──────────────
                // Rejects the packet if the client somehow sent it while deps
                // weren't met — prevents cheating / race conditions.
                if (!data.areDependenciesMet(msg.widgetId)) return;

                // ── ADDITIONAL GATE HOOKS ─────────────────────────────────────
                // Add your own checks here, e.g.:
                //   if (data.getMana() < cost) return;
                // ─────────────────────────────────────────────────────────────

                data.unlock(msg.widgetId);
                SyncKnowledgePacket.sendToPlayer(player, data);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}