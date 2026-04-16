// AetherPipeBlock.java
package net.tntim1.psychic.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.tntim1.psychic.block.entity.AetherPipeBlockEntity;
import net.tntim1.psychic.block.entity.AetherTankBlockEntity;
import net.tntim1.psychic.block.entity.ModBlockEntities;

public class AetherPipeBlock extends BaseEntityBlock {

    // One property per face — is this face connected?
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST  = BooleanProperty.create("east");
    public static final BooleanProperty WEST  = BooleanProperty.create("west");
    public static final BooleanProperty UP    = BooleanProperty.create("up");
    public static final BooleanProperty DOWN  = BooleanProperty.create("down");

    // Core pipe shape (center 6x6x6 cube)
    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape LEG_UP    = Block.box(6, 11, 6, 10, 16, 10);
    private static final VoxelShape LEG_DOWN  = Block.box(6,  0, 6, 10,  5, 10);
    private static final VoxelShape LEG_NORTH = Block.box(6,  6, 0, 10, 10,  5);
    private static final VoxelShape LEG_SOUTH = Block.box(6,  6,11, 10, 10, 16);
    private static final VoxelShape LEG_EAST  = Block.box(11, 6, 6, 16, 10, 10);
    private static final VoxelShape LEG_WEST  = Block.box( 0, 6, 6,  5, 10, 10);

    public AetherPipeBlock(Properties props) {
        super(props);
        registerDefaultState(defaultBlockState()
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST,  false).setValue(WEST,  false)
                .setValue(UP,    false).setValue(DOWN,  false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    /** A face connects if the neighbor is a pipe OR is a tank on the correct face */
    private boolean connects(LevelAccessor level, BlockPos pos, Direction dir) {
        BlockEntity neighbor = level.getBlockEntity(pos.relative(dir));
        if (neighbor instanceof AetherPipeBlockEntity) return true;
        // Pipe connects to tank bottom (pipe is below tank → UP face)
        // Pipe connects to tank top (pipe is above tank → DOWN face)
        if (neighbor instanceof AetherTankBlockEntity) {
            return dir == Direction.UP || dir == Direction.DOWN;
        }
        return false;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return stateWithConnections(ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir,
                                  BlockState neighborState, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        return stateWithConnections(level, pos);
    }

    private BlockState stateWithConnections(LevelAccessor level, BlockPos pos) {
        return defaultBlockState()
                .setValue(NORTH, connects(level, pos, Direction.NORTH))
                .setValue(SOUTH, connects(level, pos, Direction.SOUTH))
                .setValue(EAST,  connects(level, pos, Direction.EAST))
                .setValue(WEST,  connects(level, pos, Direction.WEST))
                .setValue(UP,    connects(level, pos, Direction.UP))
                .setValue(DOWN,  connects(level, pos, Direction.DOWN));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level,
                               BlockPos pos, CollisionContext ctx) {
        VoxelShape shape = CORE;
        if (state.getValue(UP))    shape = Shapes.or(shape, LEG_UP);
        if (state.getValue(DOWN))  shape = Shapes.or(shape, LEG_DOWN);
        if (state.getValue(NORTH)) shape = Shapes.or(shape, LEG_NORTH);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, LEG_SOUTH);
        if (state.getValue(EAST))  shape = Shapes.or(shape, LEG_EAST);
        if (state.getValue(WEST))  shape = Shapes.or(shape, LEG_WEST);
        return shape;
    }

    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AetherPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null :
                createTickerHelper(type, ModBlockEntities.AETHER_PIPE.get(),
                        AetherPipeBlockEntity::tick);
    }
}