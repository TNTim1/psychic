// AetherPipeBlockEntity.java
package net.tntim1.psychic.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.tntim1.psychic.fluids.PsychicFluidTank;

public class AetherPipeBlockEntity extends BlockEntity {

    public static final int PIPE_CAPACITY = 1000;

    private final PsychicFluidTank tank = new PsychicFluidTank(PIPE_CAPACITY);
    private final LazyOptional<IFluidHandler> fluidHandler = LazyOptional.of(() -> tank);

    public AetherPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AETHER_PIPE.get(), pos, state);
    }

    // AetherPipeBlockEntity.java
    public static void tick(Level level, BlockPos pos, BlockState state,
                            AetherPipeBlockEntity be) {
        if (level.isClientSide || level.getGameTime() % 5 != 0) return;
        if (be.tank.isEmpty()) return;

        Fluid carried = be.tank.getFluid().getFluid();

        // Find the best destination tank in the network
        AetherTankBlockEntity target = findBestOutputTank(level, pos, carried, new java.util.HashSet<>());
        if (target == null) return;

        FluidStack toMove = new FluidStack(carried, Math.min(100, be.tank.getFluidAmount()));
        int moved = target.getTank().fillWithCallback(toMove, IFluidHandler.FluidAction.EXECUTE);
        if (moved > 0) {
            be.tank.drain(moved, IFluidHandler.FluidAction.EXECUTE);
            be.setChanged();
            target.setChanged();
        }
    }

    /**
     * BFS through the pipe network to find the best target tank.
     * Best = fullest non-full tank matching the fluid, or any empty tank.
     * Tanks are only reachable via their TOP face (pipe must be above tank).
     */
    private static AetherTankBlockEntity findBestOutputTank(
            Level level, BlockPos start, Fluid fluid,
            java.util.Set<BlockPos> visited) {

        java.util.Queue<BlockPos> queue = new java.util.ArrayDeque<>();
        queue.add(start);
        visited.add(start);

        AetherTankBlockEntity bestMatch = null;  // has same fluid, not full
        AetherTankBlockEntity bestEmpty = null;  // is empty, not full
        int bestMatchAmount = -1;

        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();

            // Check if there's a tank below this pipe position (pipe feeds tank top)
            BlockEntity below = level.getBlockEntity(cur.below());
            if (below instanceof AetherTankBlockEntity tank && !tank.getTank().isFull()) {
                FluidStack stored = tank.getTank().getFluid();

                if (!stored.isEmpty() && stored.getFluid() == fluid) {
                    // Has matching fluid — prefer fullest (closest to overflowing)
                    if (stored.getAmount() > bestMatchAmount) {
                        bestMatchAmount = stored.getAmount();
                        bestMatch = tank;
                    }
                } else if (stored.isEmpty() && bestEmpty == null) {
                    bestEmpty = tank;
                }
            }

            // Spread through adjacent pipes (all 6 directions for routing)
            for (Direction dir : Direction.values()) {
                BlockPos next = cur.relative(dir);
                if (!visited.contains(next) &&
                        level.getBlockEntity(next) instanceof AetherPipeBlockEntity) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }

        // Prefer a tank that already has matching fluid over an empty one
        return bestMatch != null ? bestMatch : bestEmpty;
    }


    public PsychicFluidTank getTank() { return tank; }

    // Only expose to other psychic blocks
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER && side != null) {
            BlockEntity neighbor = level != null ?
                    level.getBlockEntity(worldPosition.relative(side)) : null;
            if (neighbor instanceof AetherPipeBlockEntity ||
                    neighbor instanceof AetherTankBlockEntity) {
                return fluidHandler.cast();
            }
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.put("fluid", tank.writeToNBT(new CompoundTag()));
    }
    @Override
    public void load(CompoundTag tag) {
        super.load(tag); tank.readFromNBT(tag.getCompound("fluid"));
    }
    @Override public void invalidateCaps() { super.invalidateCaps(); fluidHandler.invalidate(); }
}