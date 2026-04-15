package net.tntim1.psychic.block.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.function.Supplier;

public class StartPuzzlePacket {
    private final String spellId;
    //private final int difficulty;
    private final CompoundTag savedState; // Added this

    public StartPuzzlePacket(String spellId, @Nullable CompoundTag savedState) {
        this.spellId = spellId;
        //this.difficulty = difficulty;
        this.savedState = savedState;
    }

    public StartPuzzlePacket(FriendlyByteBuf buf) {
        this.spellId = buf.readUtf();
        //this.difficulty = buf.readInt();
        this.savedState = buf.readNbt(); // Read NBT from buffer
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(this.spellId);
        //buf.writeInt(this.difficulty);
        buf.writeNbt(this.savedState); // Write NBT to buffer
    }

    public static void handle(StartPuzzlePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof ResearchTableScreen screen) {
                // Pass the actual NBT instead of null
                screen.startLaserGame(msg.spellId, msg.savedState);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}