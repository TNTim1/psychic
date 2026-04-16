package net.tntim1.psychic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.tntim1.psychic.block.entity.AetherTankBlockEntity;
import net.tntim1.psychic.block.entity.ModBlockEntities;
import net.tntim1.psychic.fluids.PsychicFluidTank;

public class AetherTankBlock extends BaseEntityBlock {

    public AetherTankBlock(Properties props) { super(props); }


    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AetherTankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null :
                createTickerHelper(type, ModBlockEntities.AETHER_TANK.get(),
                        AetherTankBlockEntity::tick);
    }
    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true; // Let light pass through the glass
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) {
        return 1.0f; // Prevent the block from creating internal shadows
    }
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL; // Tells the game to render the JSON model AND the BER
    }

    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity be = level.getBlockEntity(pos);
        return be == null ? false : be.triggerEvent(id, param);
    }
    // Inside AetherTankBlock.java

}