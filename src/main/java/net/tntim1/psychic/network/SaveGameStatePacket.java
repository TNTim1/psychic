package net.tntim1.psychic.network;

// ─────────────────────────────────────────────────────────────────────────────
// ADDITIONAL PACKET: SaveGameStatePacket
//
// Send this from the client to the server whenever the player closes the
// Research Table screen (or when you want to checkpoint progress).
// The server reads the NBT and stores it in the block entity.
// ─────────────────────────────────────────────────────────────────────────────

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraftforge.network.NetworkEvent;
import net.tntim1.psychic.block.entity.ResearchTableBlockEntity;

import java.util.function.Supplier;

public class SaveGameStatePacket {

    private final BlockPos pos;
    private final CompoundTag gameState;

    // ── Constructor (client side) ─────────────────────────────────────────────

    public SaveGameStatePacket(BlockPos pos, CompoundTag gameState) {
        this.pos       = pos;
        this.gameState = gameState;
    }

    // ── Encoding / decoding ───────────────────────────────────────────────────

    public SaveGameStatePacket(FriendlyByteBuf buf) {
        this.pos       = buf.readBlockPos();
        this.gameState = buf.readNbt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeNbt(gameState);
    }

    // ── Handler (server side) ─────────────────────────────────────────────────

    public boolean handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            BlockEntity be = player.level().getBlockEntity(pos);
            if (be instanceof ResearchTableBlockEntity rbe) {
                // Directly store the raw NBT; no validation needed because
                // the client only sends data that came from init() in the
                // first place.
                rbe.saveGameStateRaw(gameState);
            }
        });
        ctx.get().setPacketHandled(true);
        return true;
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Add this helper to ResearchTableBlockEntity:
//
//   /** Stores raw mirror-game NBT (called by SaveGameStatePacket). */
//   public void saveGameStateRaw(CompoundTag tag) {
//       this.savedGameState = tag;
//       setChanged();
//   }
//
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
// HOW TO SEND THE PACKET FROM THE SCREEN
//
// Override onClose() in ResearchTableScreen:
//
//   @Override
//   public void onClose() {
//       if (activeGame != null) {
//           BlockPos pos = getMenu().getBlockEntity().getBlockPos();
//           CompoundTag nbt = activeGame.serializeToNBT();
//           ModPackets.sendToServer(new SaveGameStatePacket(pos, nbt));
//       }
//       super.onClose();
//   }
//
// ─────────────────────────────────────────────────────────────────────────────

// ─────────────────────────────────────────────────────────────────────────────
// HOW TO RESTORE STATE WHEN THE SCREEN OPENS
//
// In your RequestPuzzlePacket server handler, after creating the menu,
// read the saved state from the block entity and send it back with the
// puzzle response packet:
//
//   CompoundTag saved = rbe.getSavedGameState();
//   // Include `saved` in your existing puzzle-response packet so the
//   // client can pass it to startLaserGame(spellId, difficulty, saved).
//
// ─────────────────────────────────────────────────────────────────────────────