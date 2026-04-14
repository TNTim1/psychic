package net.tntim1.psychic.block.entity;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.tntim1.psychic.capability.PsychicCapability;

import java.util.function.Supplier;

public class CompletePuzzlePacket {
    private final String spellId;

    // Default constructor for sending from Client
    public CompletePuzzlePacket(String spellId) {
        this.spellId = spellId;
    }

    // Decoder: Reads the spellId sent from the client
    public CompletePuzzlePacket(FriendlyByteBuf buf) {
        this.spellId = buf.readUtf();
    }

    // Encoder: Writes the spellId to the network buffer
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.spellId);
    }

    public static void handle(CompletePuzzlePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            player.getCapability(PsychicCapability.PSYCHIC_DATA_CAP).ifPresent(data -> {
                if (!data.isUnlocked(msg.spellId)) {
                    data.unlockSpell(msg.spellId, player);

                    player.sendSystemMessage(
                            Component.literal("§bUnlocked spell: " + msg.spellId));
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}