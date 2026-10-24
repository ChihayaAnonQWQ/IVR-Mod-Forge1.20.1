package net.hulan.ivr.block;

import mtr.block.IBlock;
import mtr.mappings.BlockEntityMapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.NotNull;

public class BlockKCRStationNameEntrance extends BlockKCRStationNameBase {

    public static final IntegerProperty STYLE = IntegerProperty.create("propagate_property", 0, 5);

    public BlockKCRStationNameEntrance() {
        super();
    }

    @SuppressWarnings("deprecation")
    public @NotNull InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand interactionHand, net.minecraft.world.phys.BlockHitResult blockHitResult) {
        return IBlock.checkHoldingBrush(world, player, () -> {
            world.setBlockAndUpdate(pos, state.cycle(STYLE));
            propagate(world, pos, IBlock.getStatePropertySafe(state, FACING).getClockWise(), STYLE, 1);
            propagate(world, pos, IBlock.getStatePropertySafe(state, FACING).getCounterClockWise(), STYLE, 1);
        });
    }

    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        Direction facing = IBlock.getStatePropertySafe(state, FACING);
        return world.getBlockState(pos.relative(facing)).isSolid();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction side = ctx.getClickedFace();
        return side != Direction.UP && side != Direction.DOWN ? defaultBlockState().setValue(FACING, side.getOpposite()) : null;
    }

    @SuppressWarnings("deprecation")
    public @NotNull BlockState updateShape(BlockState state, Direction direction, BlockState newState, LevelAccessor world, BlockPos pos, BlockPos posFrom) {
        return direction.getOpposite() == IBlock.getStatePropertySafe(state, FACING).getOpposite() && !state.canSurvive(world, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @SuppressWarnings("deprecation")
    public net.minecraft.world.phys.shapes.@NotNull VoxelShape getShape(BlockState state, BlockGetter blockGetter, BlockPos pos, CollisionContext collisionContext) {
        boolean tall = IBlock.getStatePropertySafe(state, STYLE) % 2 == 1;
        return IBlock.getVoxelShapeByDirection(0.0F, tall ? (double)0.0F : (double)4.0F, 0.0F, 16.0F, tall ? (double)16.0F : (double)12.0F, 1.0F, IBlock.getStatePropertySafe(state, FACING));
    }

    @SuppressWarnings("deprecation")
    public net.minecraft.world.phys.shapes.@NotNull VoxelShape getCollisionShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return Shapes.empty();
    }

    @Override
    public BlockEntityMapper createBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityKCRStationNameEntrance(pos, state);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STYLE);
    }

    public static class TileEntityKCRStationNameEntrance extends TileEntityKCRStationNameBase {

        public TileEntityKCRStationNameEntrance(BlockPos pos, BlockState state) {
            super(KCR_STATION_NAME_ENTRANCE_TILE_ENTITY.get(), pos, state, 0.0F, 0.00625F, false);
        }

        @Override
        public int getColor(BlockState state) {
            return switch (IBlock.getStatePropertySafe(state, BlockKCRStationNameBase.COLOR)) {
                case 1 -> -5592406;
                case 2 -> ARGB_BLACK;
                default -> -1;
            };
        }
    }
}
