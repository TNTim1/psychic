
// ManaSyncPacket.java
package net.tntim1.psychic.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.tntim1.psychic.player_data.ClientManaStore;

import java.util.function.Supplier;

public class ManaSyncPacket {
    public final float mana, maxMana;

    public ManaSyncPacket(float mana, float maxMana) {
        this.mana = mana; this.maxMana = maxMana;
    }

    public static void encode(ManaSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.mana); buf.writeFloat(msg.maxMana);
    }

    public static ManaSyncPacket decode(FriendlyByteBuf buf) {
        return new ManaSyncPacket(buf.readFloat(), buf.readFloat());
    }

    public static void handle(ManaSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientManaStore.set(msg.mana, msg.maxMana));
        ctx.get().setPacketHandled(true);
    }
}