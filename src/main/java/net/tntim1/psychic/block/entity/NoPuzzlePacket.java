package net.tntim1.psychic.block.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class NoPuzzlePacket {
    public NoPuzzlePacket(FriendlyByteBuf buf) {}
    public NoPuzzlePacket() {}
    public void toBytes(FriendlyByteBuf buf) {}

    public static void handle(NoPuzzlePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Logic must stay on the client side
            Minecraft.getInstance().player.displayClientMessage(
                    Component.literal("§aAll spells already unlocked!"), true);
        });
        ctx.get().setPacketHandled(true);
    }
}