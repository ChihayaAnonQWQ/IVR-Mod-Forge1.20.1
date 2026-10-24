package net.hulan.ivr.block;

import mtr.block.IBlock;
import mtr.mappings.*;
import net.hulan.ivr.IVRBlockEntityTypes;
import net.hulan.ksd.data.SingleTicketSystem;
import net.hulan.ksd.packet.KSDPacketServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.List;

public class BlockKCRSingleTicketMachine extends BlockDirectionalMapper implements EntityBlockMapper {

    private static final BooleanProperty STORED = BooleanProperty.create("stored");
    public static final EnumProperty<Side> SIDE = EnumProperty.create("side", Side.class);
    public static final EnumProperty<Height> HEIGHT = EnumProperty.create("height", Height.class);
    public final SingleTicketSystem.TicketType ticketType;
    public final boolean isWall;

    public BlockKCRSingleTicketMachine(SingleTicketSystem.TicketType ticketType, boolean isWall) {
        super(net.hulan.ksd.utils.Utilities.getInstance().createBlockProperties()
                .requiresCorrectToolForDrops()
                .strength(2)
                .lightLevel(state -> 14));
        this.ticketType = ticketType;
        this.isWall = isWall;
        registerDefaultState(defaultBlockState().setValue(STORED, false).setValue(SIDE, Side.LEFT).setValue(HEIGHT, Height.DOWN));
    }

    @SuppressWarnings("deprecation")
    public @NotNull InteractionResult use(BlockState blockState, Level world, BlockPos pos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        BlockPos leftBottomPos = getLeftBottomPos(blockState, pos);
        if (!world.isClientSide) {
            if (!isLeftBottom(blockState) || (isLeftBottom(blockState) && !IBlock.getStatePropertySafe(blockState, STORED))) {
                KSDPacketServer.openSTMachineScreenS2C((ServerPlayer) player, ticketType, leftBottomPos);
            } else {
                BlockEntity entity = world.getBlockEntity(leftBottomPos);
                if (entity instanceof TileEntityKCRSingleTicketMachine stmEntity) {
                    stmEntity.releaseItems(Utilities.getInventory(player));
                    world.setBlock(leftBottomPos, blockState.setValue(STORED, false), 3);
                }
            }
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getClockWise();
        return IBlock.isReplaceable(context, Direction.UP, 3)
                && canBePlaced(context.getLevel(), context.getClickedPos(), facing) ?
                defaultBlockState()
                .setValue(FACING, facing)
                .setValue(SIDE, Side.LEFT)
                .setValue(HEIGHT, Height.DOWN)
                .setValue(STORED, false)
                : null;
    }

    public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide) {
            Direction widthDirection = IBlock.getStatePropertySafe(state, FACING);
            BlockPos leftBottom = pos.relative(widthDirection, state.getValue(SIDE) == Side.RIGHT ? -1 : 0).below(state.getValue(HEIGHT).offset);
            for (Side side : Side.values()) {
                for (Height height : Height.values()) {
                    BlockPos partPos = leftBottom.relative(widthDirection, side == Side.RIGHT ? 1 : 0).above(height.offset);
                    if (!partPos.equals(pos) && world.getBlockState(partPos).getBlock() == this) {
                        world.destroyBlock(partPos, false);
                    }
                }
            }
        }
        super.playerWillDestroy(world, pos, state, player);
    }

    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if (!world.isClientSide) {
            Direction widthDirection = IBlock.getStatePropertySafe(state, FACING);
            if (!canBePlaced(world, pos, widthDirection)) {
                world.removeBlock(pos, false);
                return;
            }
            for (Side side : Side.values()) {
                for (Height height : Height.values()) {
                    if (side == Side.LEFT && height == Height.DOWN) {
                        continue;
                    }
                    BlockPos partPos = pos.relative(widthDirection, side == Side.RIGHT ? 1 : 0).above(height.offset);
                    world.setBlock(partPos, defaultBlockState()
                            .setValue(FACING, state.getValue(FACING))
                            .setValue(SIDE, side)
                            .setValue(HEIGHT, height)
                            .setValue(STORED, false), 3);
                }
            }
        }
        super.setPlacedBy(world, pos, state, placer, itemStack);
    }

    private boolean canBePlaced(Level world, BlockPos leftBottom, Direction widthDirection) {
        for (Side side : Side.values()) {
            for (Height height : Height.values()) {
                if (side == Side.LEFT && height == Height.DOWN) {
                    continue;
                }
                BlockPos partPos = leftBottom.relative(widthDirection, side == Side.RIGHT ? 1 : 0).above(height.offset);
                if (!world.getBlockState(partPos).canBeReplaced()) {
                    return false;
                }
            }
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    public @NotNull VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        Direction shapeDirection = IBlock.getStatePropertySafe(blockState, FACING).getClockWise();
        double minY = (blockState.getValue(HEIGHT) == Height.DOWN) && isWall ? 10 : 0;
        double minZ = blockState.getValue(HEIGHT) == Height.TOP ? -2.26898 : -0.1;
        return IBlock.getVoxelShapeByDirection(0, minY, minZ, 16, 16, 11.9, shapeDirection);
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STORED, SIDE, HEIGHT);
    }

    private static boolean isLeftBottom(BlockState state) {
        return state.getValue(SIDE) == Side.LEFT && state.getValue(HEIGHT) == Height.DOWN;
    }

    private static BlockPos getLeftBottomPos(BlockState state, BlockPos pos) {
        Direction widthDirection = IBlock.getStatePropertySafe(state, FACING);
        return pos.relative(widthDirection, state.getValue(SIDE) == Side.RIGHT ? -1 : 0)
                .below(state.getValue(HEIGHT).offset);
    }

    public enum Side implements StringRepresentable {

        LEFT("left"),
        RIGHT("right");

        private final String name;

        Side(String name) {
            this.name = name;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }

    public enum Height implements StringRepresentable {

        DOWN("down", 0),
        MIDDLE("middle", 1),
        TOP("top", 2);

        private final String name;
        private final int offset;

        Height(String name, int offset) {
            this.name = name;
            this.offset = offset;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }

    @Override
    public BlockEntityMapper createBlockEntity(BlockPos blockPos, BlockState blockState) {
        return isLeftBottom(blockState) ? new TileEntityKCRSingleTicketMachine(ticketType, isWall, blockPos, blockState) : null;
    }

    public static class TileEntityKCRSingleTicketMachine extends BlockEntityClientSerializableMapper implements StorableBlockEntity {

        private final List<ItemStack> items = new ArrayList<>();

        public TileEntityKCRSingleTicketMachine(SingleTicketSystem.TicketType ticketType, boolean isWall, BlockPos blockPos, BlockState blockState) {
            super(switch (ticketType) {
                        case MTR -> isWall ?
                                IVRBlockEntityTypes.KCR_SINGLE_TICKET_MACHINE_MTR_WALL_TILE_ENTITY.get() :
                                IVRBlockEntityTypes.KCR_SINGLE_TICKET_MACHINE_MTR_TILE_ENTITY.get();
                        case KCR ->isWall ?
                                IVRBlockEntityTypes.KCR_SINGLE_TICKET_MACHINE_KCR_WALL_TILE_ENTITY.get() :
                                IVRBlockEntityTypes.KCR_SINGLE_TICKET_MACHINE_KCR_TILE_ENTITY.get();
                        case LRT -> isWall ?
                                IVRBlockEntityTypes.KCR_SINGLE_TICKET_MACHINE_LIGHT_RAIL_TILE_ENTITY.get() :
                                IVRBlockEntityTypes.KCR_SINGLE_TICKET_MACHINE_LIGHT_RAIL_WALL_TILE_ENTITY.get();
                    },
                    blockPos,
                    blockState);
        }

        public void storeItems(List<ItemStack> items) {
            this.items.addAll(items);
            if (level != null && getBlockState() != null) {
                level.setBlock(getBlockPos(), getBlockState().setValue(STORED, true), 3);
            }
            setChanged();
            syncData();
        }

        public void releaseItems(Inventory inventory) {
            for (ItemStack item : items) {
                inventory.add(item.copy());
            }
            items.clear();
            setChanged();
            syncData();
        }

        public void readCompoundTag(CompoundTag compoundTag) {
            ListTag listTag = compoundTag.getList("items", 10);
            for (int i = 0; i < listTag.size(); ++i) {
                items.add(ItemStack.of(listTag.getCompound(i)));
            }
        }

        public void writeCompoundTag(CompoundTag compoundTag) {
            ListTag listTag = new ListTag();
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) {
                    CompoundTag tag = new CompoundTag();
                    stack.save(tag);
                    listTag.add(tag);
                }
            }
            compoundTag.put("items", listTag);
        }
    }
}
