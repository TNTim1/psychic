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

            // 1. Handle Item Consumption
            if (player.containerMenu instanceof net.tntim1.psychic.block.entity.ResearchTableMenu menu) {
                net.tntim1.psychic.block.entity.LaserPuzzle puzzle = net.tntim1.psychic.block.entity.LaserPuzzle.get(msg.spellId);
                if (puzzle != null) {
                    // Count how many items are needed per Laser ID
                    java.util.Map<Integer, Integer> requirements = new java.util.HashMap<>();
                    for (java.util.List<Integer> group : puzzle.goals) {
                        for (int id : group) {
                            requirements.merge(id, 1, Integer::sum);
                        }
                    }

                    // Remove items from the mapped slots
                    for (java.util.Map.Entry<Integer, Integer> entry : requirements.entrySet()) {
                        int slotIndex = getSlotIndexForLaser(entry.getKey());
                        player.containerMenu.getSlot(slotIndex).getItem().shrink(entry.getValue());
                    }
                }
            }

            // 2. Handle Capability Unlock
            player.getCapability(PsychicCapability.PSYCHIC_DATA_CAP).ifPresent(data -> {
                if (!data.isUnlocked(msg.spellId)) {
                    data.unlockSpell(msg.spellId, player);
                    player.sendSystemMessage(Component.literal("§bUnlocked spell: " + msg.spellId));
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * Mapping Laser IDs (Clockwise starting bottom-right) to Menu Slots (Reading order).
     */
    private static int getSlotIndexForLaser(int laserId) {
        return switch (laserId) {
            // Left Column
            case 1 -> 0;
            case 2 -> 1;
            case 3 -> 2;
            case 4 -> 3;
            // Right Column
            case 5 -> 4;
            case 6 -> 5;
            case 7 -> 6;
            case 8 -> 7;
            default -> 0;
        };
    }
}