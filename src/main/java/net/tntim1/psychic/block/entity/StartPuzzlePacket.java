package net.tntim1.psychic.block.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class StartPuzzlePacket {
    private final String spellId;
    private final int difficulty;

    public StartPuzzlePacket(String spellId, int difficulty) {
        this.spellId = spellId;
        this.difficulty = difficulty;
    }

    // Decoder: READ data from the buffer
    public StartPuzzlePacket(FriendlyByteBuf buf) {
        this.spellId = buf.readUtf();
        this.difficulty = buf.readInt();
    }

    // Encoder: WRITE data to the buffer
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.spellId);
        buf.writeInt(this.difficulty);
    }

    public static void handle(StartPuzzlePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof ResearchTableScreen screen) {
                screen.startLaserGame(msg.spellId, msg.difficulty);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}