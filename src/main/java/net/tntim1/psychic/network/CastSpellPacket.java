package net.tntim1.psychic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.NetworkEvent;
import net.tntim1.psychic.Spells.SpellDefinition;
import net.tntim1.psychic.Spells.SpellRegistry;
import net.tntim1.psychic.chunk_data.ChunkWarpProvider;

import java.util.function.Supplier;

// CastSpellPacket.java
public class CastSpellPacket {
    private final String spellId;
    private final float accuracy;

    public CastSpellPacket(String spellId, float accuracy) {
        this.spellId = spellId;
        this.accuracy = accuracy;
    }

    public static void encode(CastSpellPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.spellId);
        buf.writeFloat(msg.accuracy);
    }

    public static CastSpellPacket decode(FriendlyByteBuf buf) {
        return new CastSpellPacket(buf.readUtf(), buf.readFloat());
    }

    public static void handle(CastSpellPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            SpellDefinition spell = SpellRegistry.SPELLS.get(msg.spellId);
            if (spell == null) return;

            // 1. Run whatever custom action the spell has
            if (spell.action != null) {
                spell.action.execute(player);
            }

            // 2. Always apply warp change on top, regardless of action
            if (spell.warpChange != 0) {
                LevelChunk chunk = player.serverLevel().getChunkAt(player.blockPosition());
                chunk.getCapability(ChunkWarpProvider.CAP).ifPresent(data -> {
                    data.addWarp(spell.warpChange);
                    chunk.setUnsaved(true);
                    ModPackets.sendToPlayer(new WarpSyncPacket(data.getWarpStrength()), player);
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}