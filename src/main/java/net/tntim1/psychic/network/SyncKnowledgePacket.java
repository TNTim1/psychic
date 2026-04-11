package net.tntim1.psychic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.tntim1.psychic.player_data.ClientKnowledge;
import net.tntim1.psychic.player_data.PsychicData;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Sent from the server to the client whenever the player's unlocked-widget set changes.
 *
 * <p>Also sent on player login/respawn so the client GUI is always up-to-date.
 */
public class SyncKnowledgePacket {

    private final Set<String> unlockedIds;

    public SyncKnowledgePacket(Set<String> unlockedIds) {
        this.unlockedIds = unlockedIds;
    }

    // ── serialisation ─────────────────────────────────────────────────────────

    public static void encode(SyncKnowledgePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.unlockedIds.size());
        for (String id : msg.unlockedIds) {
            buf.writeUtf(id);
        }
    }

    public static SyncKnowledgePacket decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Set<String> ids = new HashSet<>(size);
        for (int i = 0; i < size; i++) {
            ids.add(buf.readUtf());
        }
        return new SyncKnowledgePacket(ids);
    }

    // ── client-side handler ───────────────────────────────────────────────────

    public static void handle(SyncKnowledgePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // This runs on the CLIENT main thread — safe to touch ClientKnowledge directly
            ClientKnowledge.setUnlockedIds(msg.unlockedIds);
        });
        ctx.get().setPacketHandled(true);
    }

    // ── helper: send from server to a specific player ─────────────────────────

    /**
     * Convenience method called by the server to push current data to one player.
     *
     * @param player the target
     * @param data   the authoritative server-side data for that player
     */
    public static void sendToPlayer(ServerPlayer player, PsychicData data) {
        // Build a snapshot of the ID set and ship it
        Set<String> snapshot = data.getUnlockedIds();   // see PsychicData changes below
        ModPackets.CHANNEL.sendTo(
                new SyncKnowledgePacket(snapshot),
                player.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT
        );
    }
}