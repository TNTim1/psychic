// AetherTankBlockEntity.java
package net.tntim1.psychic.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.tntim1.psychic.chunk_data.ChunkWarpProvider;
import net.tntim1.psychic.fluids.PsychicFluidTank;
import net.tntim1.psychic.network.ModPackets;
import net.tntim1.psychic.network.WarpSyncPacket;

public class AetherTankBlockEntity extends BlockEntity {

    public static final int CAPACITY = 8000;

    // How much fluid is consumed per 1 warp added
    private static final int FLUID_PER_WARP = 10; // 10 mB per warp unit
    private static final float WARP_PER_TICK = 0.05f;

    private float pendingWarp = 0f;



    private final PsychicFluidTank tank = new PsychicFluidTank(CAPACITY);
    private final LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(() -> tank);

    public AetherTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AETHER_TANK.get(), pos, state);
    }

    // AetherTankBlockEntity.java
    public static void tick(Level level, BlockPos pos, BlockState state,
                            AetherTankBlockEntity be) {
        if (level.isClientSide || be.tank.isEmpty()) return;

        // --- Warp generation (unchanged) ---
        be.pendingWarp += WARP_PER_TICK;
        if (be.pendingWarp >= 1f) {
            int warpToAdd = (int) be.pendingWarp;
            be.pendingWarp -= warpToAdd;

            int fluidCost = warpToAdd * FLUID_PER_WARP;
            int available = be.tank.getFluidAmount();

            if (available < fluidCost) {
                warpToAdd = available / FLUID_PER_WARP;
                fluidCost = warpToAdd * FLUID_PER_WARP;
            }

            if (warpToAdd > 0) {
                be.tank.drain(fluidCost, IFluidHandler.FluidAction.EXECUTE);
                be.sync();
                be.setChanged();
                addWarpToChunk(level, pos, warpToAdd);
            }
        }

        // --- Output: push DOWN into pipe below every 10 ticks ---
        if (level.getGameTime() % 10 != 0) return;

        BlockEntity below = level.getBlockEntity(pos.below());
        if (!(below instanceof AetherPipeBlockEntity pipe)) return;

        FluidStack toMove = new FluidStack(
                be.tank.getFluid(), Math.min(100, be.tank.getFluidAmount()));
        int moved = pipe.getTank().fillWithCallback(toMove, IFluidHandler.FluidAction.EXECUTE);
        if (moved > 0) {
            be.tank.drain(moved, IFluidHandler.FluidAction.EXECUTE);
            be.setChanged();
        }
    }
    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            // This triggers getUpdatePacket() and sends it to all nearby clients
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }



    private static void addWarpToChunk(Level level, BlockPos pos, int amount) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        LevelChunk chunk = level.getChunkAt(pos);
        ChunkPos chunkPos = chunk.getPos();

        chunk.getCapability(ChunkWarpProvider.CAP).ifPresent(data -> {
            data.addWarp(amount);
            chunk.setUnsaved(true);

            int currentWarp = data.getWarpStrength();
            WarpSyncPacket packet = new WarpSyncPacket(currentWarp);

            // OPTIMIZATION: Only target players who are actually IN this chunk
            // serverLevel.getChunkSource().chunkMap is the most efficient way to find relevant players
            serverLevel.getChunkSource().chunkMap.getPlayers(chunkPos, false).forEach(player -> {
                if (player.chunkPosition().equals(chunkPos)) {
                    ModPackets.sendToPlayer(packet, player);
                }
            });
        });
    }
    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata(); // Send NBT data to client on chunk load
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this); // Send update when setChanged() is called
    }

    // Only expose capability to psychic pipes
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            // Internal access (no side = our own tick code)
            if (side == null) return fluidHandler.cast();

            // Top = input: accept from pipe above
            if (side == Direction.UP) {
                BlockEntity above = level != null ?
                        level.getBlockEntity(worldPosition.above()) : null;
                if (above instanceof AetherPipeBlockEntity)
                    return fluidHandler.cast();
            }

            // Bottom = output: push to pipe below (pipe reads this to drain)
            // Tanks push themselves in tick() so no external pull needed here
            // but expose it so pipes can query fill level if needed
            if (side == Direction.DOWN) {
                BlockEntity below = level != null ?
                        level.getBlockEntity(worldPosition.below()) : null;
                if (below instanceof AetherPipeBlockEntity)
                    return fluidHandler.cast();
            }

            // Sides = dead
            return LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }
    public PsychicFluidTank getTank() { return tank; }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("fluid", tank.writeToNBT(new CompoundTag()));
        tag.putFloat("pendingWarp", pendingWarp);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        tank.readFromNBT(tag.getCompound("fluid"));
        pendingWarp = tag.getFloat("pendingWarp");
    }


    @Override
    public void invalidateCaps() { super.invalidateCaps(); fluidHandler.invalidate(); }
}