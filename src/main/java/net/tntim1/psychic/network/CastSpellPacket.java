package net.tntim1.psychic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.NetworkEvent;
import net.tntim1.psychic.Spells.SpellDefinition;
import net.tntim1.psychic.Spells.SpellRegistry;
import net.tntim1.psychic.chunk_data.ChunkWarpProvider;
import net.tntim1.psychic.player_data.PlayerManaProvider;

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

            player.getCapability(PlayerManaProvider.CAP).ifPresent(mana -> {



                // --- Generic spell action (runs for all spells including force_field) ---
                if (spell.action != null) spell.action.execute(player);

                // --- Apply warp change ---
                if (spell.warpChange != 0) {
                    LevelChunk chunk = player.serverLevel().getChunkAt(player.blockPosition());
                    chunk.getCapability(ChunkWarpProvider.CAP).ifPresent(data -> {
                        data.addWarp(spell.warpChange);
                        chunk.setUnsaved(true);
                        ModPackets.sendToPlayer(new WarpSyncPacket(data.getWarpStrength()), player);
                    });
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}