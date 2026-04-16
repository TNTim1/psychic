package net.tntim1.psychic.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.tntim1.psychic.block.entity.AetherTankBlockEntity;
import net.tntim1.psychic.fluids.ModFluids;

public class EssenceItem extends Item {

    private final int essenceIndex; // index into ModFluids.SOURCES

    public EssenceItem(int essenceIndex) {
        super(new Item.Properties().stacksTo(64));
        this.essenceIndex = essenceIndex;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);

        if (!(be instanceof AetherTankBlockEntity tank)) return InteractionResult.PASS;

        // Always return success on client to prevent arm-swing stutter,
        // but logic happens on server.
        if (level.isClientSide) return InteractionResult.SUCCESS;

        // 1 item = 250 mB
        FluidStack toAdd = new FluidStack(ModFluids.SOURCES[essenceIndex].get(), 250);
        int filled = tank.getTank().fillWithCallback(toAdd, IFluidHandler.FluidAction.EXECUTE);

        if (filled > 0) {
            if (!ctx.getPlayer().isCreative()) {
                ctx.getItemInHand().shrink(1);
            }

            // --- THE UPDATE LOGIC ---
            tank.setChanged(); // Saves to disk

            // Tells the game to sync the BlockEntity data to the client
            // Param 1 & 2: The old and new state (usually the same for BE updates)
            // Param 3: A bitmask (3 = block update + observers notified)
            level.sendBlockUpdated(pos, tank.getBlockState(), tank.getBlockState(), 3);

            return InteractionResult.CONSUME;
        }

        return InteractionResult.FAIL;
    }
}