package net.tntim1.psychic.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.tntim1.psychic.Spells.SpellDefinition;
import net.tntim1.psychic.Spells.SpellRegistry;
import net.tntim1.psychic.player_data.PlayerManaProvider;

import java.util.function.Supplier;

public class SpendManaPacket {
    private final String spellId;

    public SpendManaPacket(String spellId) { this.spellId = spellId; }

    public static void encode(SpendManaPacket msg, FriendlyByteBuf buf) { buf.writeUtf(msg.spellId); }
    public static SpendManaPacket decode(FriendlyByteBuf buf) { return new SpendManaPacket(buf.readUtf()); }

    public static void handle(SpendManaPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            SpellDefinition spell = SpellRegistry.SPELLS.get(msg.spellId);
            if (spell == null) return;

            player.getCapability(PlayerManaProvider.CAP).ifPresent(mana -> {
                if (!mana.spend(spell.manaCost)) {
                    // Not enough — tell client to cancel
                    ModPackets.sendToPlayer(new ManaSpendResultPacket(false), player);
                } else {
                    // Sync new mana value and confirm
                    ModPackets.sendToPlayer(
                            new ManaSyncPacket(mana.getMana(), mana.getMaxMana()), player);
                    ModPackets.sendToPlayer(new ManaSpendResultPacket(true), player);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}