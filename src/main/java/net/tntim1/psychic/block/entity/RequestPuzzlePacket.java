package net.tntim1.psychic.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.tntim1.psychic.Spells.SpellRegistry;
import net.tntim1.psychic.capability.PsychicData;
import net.tntim1.psychic.network.ModPackets;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class RequestPuzzlePacket {
    private final BlockPos pos;

    public RequestPuzzlePacket(BlockPos pos) {
        this.pos = pos;
    }

    public RequestPuzzlePacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    public static void handle(RequestPuzzlePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            if (player.level().getBlockEntity(msg.pos) instanceof ResearchTableBlockEntity rbe) {
                PsychicData data = PsychicData.get(player);

                // 1. Get ALL registered spell IDs dynamically from your registry
                // Assuming SpellRegistry.SPELLS is public, or add a method to get keys
                Set<String> allSpellIds = SpellRegistry.SPELLS.keySet();

                CompoundTag savedState = rbe.getSavedGameState();
                String spell = null;

                // 2. Try to reuse saved spell
                if (savedState != null && savedState.contains("spellId")) {
                    String savedSpell = savedState.getString("spellId");

                    // Check if the saved spell actually exists in our registry and isn't unlocked
                    if (allSpellIds.contains(savedSpell) && !data.isUnlocked(savedSpell)) {
                        spell = savedSpell;
                        System.out.println("[Puzzle] Reusing saved spell: " + spell);
                    }
                }

                // 3. Fallback: generate new spell from Registry
                if (spell == null) {
                    List<String> available = allSpellIds.stream()
                            .filter(id -> !data.isUnlocked(id))
                            .toList();

                    if (available.isEmpty()) {
                        ModPackets.sendToPlayer(new NoPuzzlePacket(), player);
                        return;
                    }

                    spell = available.get(player.getRandom().nextInt(available.size()));
                    System.out.println("[Puzzle] Generated new spell from Registry: " + spell);
                }

                // 4. Send to client
                ModPackets.sendToPlayer(new StartPuzzlePacket(spell, savedState), player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}