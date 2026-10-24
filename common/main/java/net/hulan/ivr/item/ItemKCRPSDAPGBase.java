package net.hulan.ivr.item;

import mtr.Blocks;
import mtr.block.BlockPSDAPGBase;
import mtr.block.IBlock;
import mtr.block.ITripleBlock;
import mtr.item.ItemWithCreativeTabBase;
import mtr.mappings.Text;
import net.hulan.ivr.IVRBlocks;
import net.hulan.ivr.IVRCreativeModTabs;
import net.hulan.ivr.block.BlockKCRPSDTop;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ItemKCRPSDAPGBase extends ItemWithCreativeTabBase implements IBlock, IVRBlocks {
    
    private final EnumKCRPSDAPGItem item;
    private final EnumKCRPSDAPGType type;

    public ItemKCRPSDAPGBase(EnumKCRPSDAPGItem item, EnumKCRPSDAPGType type) {
        super(IVRCreativeModTabs.IVR_);
        this.item = item;
        this.type = type;
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        int horizontalBlocks = item.isDoor ? (type.isOdd ? 3 : 2) : 1;
        if (blocksNotReplaceable(context, horizontalBlocks, type.isPSD ? 3 : 2, getBlockStateFromItem().getBlock())) {
            return InteractionResult.FAIL;
        } else {
            Level world = context.getLevel();
            Direction playerFacing = context.getHorizontalDirection();
            BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
            for(int x = 0; x < horizontalBlocks; ++x) {
                BlockPos newPos = pos.relative(playerFacing.getClockWise(), x);
                for(int y = 0; y < 2; ++y) {
                    BlockState state = getBlockStateFromItem().setValue(BlockPSDAPGBase.FACING, playerFacing).setValue(HALF, y == 1 ? DoubleBlockHalf.UPPER : DoubleBlockHalf.LOWER);
                    if (item.isDoor) {
                        BlockState newState = state.setValue(SIDE, x == 0 ? EnumSide.LEFT : EnumSide.RIGHT);
                        if (type.isOdd) {
                            newState = newState.setValue(ITripleBlock.ODD, x > 0 && x < horizontalBlocks - 1);
                        }
                        world.setBlockAndUpdate(newPos.above(y), newState);
                    } else {
                        world.setBlockAndUpdate(newPos.above(y), state.setValue(SIDE_EXTENDED, EnumSide.SINGLE));
                    }
                }
                if (type.isPSD) {
                    world.setBlockAndUpdate(newPos.above(2), BlockKCRPSDTop.getActualState(world, newPos.above(2)));
                }
            }
            context.getItemInHand().shrink(1);
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public void appendHoverText(ItemStack itemStack, Level level, List<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.add(Text.translatable(type.isLift ? (type.isOdd ? "tooltip.mtr.railway_sign_odd" : "tooltip.mtr.railway_sign_even") : "tooltip.mtr." + item.getSerializedName()).setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY)));
    }

    private BlockState getBlockStateFromItem() {
        return switch (type) {
            case PSD_1 -> switch (item) {
                case PSD_APG_DOOR -> KCR_PSD_DOOR_1.get().defaultBlockState();
                case PSD_APG_GLASS -> KCR_PSD_GLASS_1.get().defaultBlockState();
                case PSD_APG_GLASS_END -> KCR_PSD_GLASS_END_1.get().defaultBlockState();
            };
            case PSD_2 -> switch (item) {
                case PSD_APG_DOOR -> KCR_PSD_DOOR_2.get().defaultBlockState();
                case PSD_APG_GLASS -> KCR_PSD_GLASS_2.get().defaultBlockState();
                case PSD_APG_GLASS_END -> KCR_PSD_GLASS_END_2.get().defaultBlockState();
            };
            case APG -> switch (item) {
                case PSD_APG_DOOR -> KCR_APG_DOOR.get().defaultBlockState();
                case PSD_APG_GLASS -> KCR_APG_GLASS.get().defaultBlockState();
                case PSD_APG_GLASS_END -> KCR_APG_GLASS_END.get().defaultBlockState();
            };
            case LIFT_DOOR_1 -> Blocks.LIFT_DOOR_EVEN_1.get().defaultBlockState();
            case LIFT_DOOR_ODD_1 -> Blocks.LIFT_DOOR_ODD_1.get().defaultBlockState();
        };
    }

    public static boolean blocksNotReplaceable(UseOnContext context, int width, int height, Block blacklistBlock) {
        Direction facing = context.getHorizontalDirection();
        Level world = context.getLevel();
        BlockPos startingPos = context.getClickedPos().relative(context.getClickedFace());
        for(int x = 0; x < width; ++x) {
            BlockPos offsetPos = startingPos.relative(facing.getClockWise(), x);
            if (blacklistBlock != null) {
                boolean isBlacklistedBelow = world.getBlockState(offsetPos.below()).is(blacklistBlock);
                boolean isBlacklistedAbove = world.getBlockState(offsetPos.above(height)).is(blacklistBlock);
                if (isBlacklistedBelow || isBlacklistedAbove) {
                    return true;
                }
            }
            for(int y = 0; y < height; ++y) {
                if (!world.getBlockState(offsetPos.above(y)).canBeReplaced()) {
                    return true;
                }
            }
        }
        return false;
    }

    public enum EnumKCRPSDAPGType {
        PSD_1(true, false, false),
        PSD_2(true, false, false),
        APG(false, false, false),
        LIFT_DOOR_1(false, false, true),
        LIFT_DOOR_ODD_1(false, true, true);

        private final boolean isPSD;
        private final boolean isOdd;
        private final boolean isLift;

        EnumKCRPSDAPGType(boolean isPSD, boolean isOdd, boolean isLift) {
            this.isPSD = isPSD;
            this.isOdd = isOdd;
            this.isLift = isLift;
        }
    }

    public enum EnumKCRPSDAPGItem implements StringRepresentable {
        PSD_APG_DOOR("psd_apg_door", true),
        PSD_APG_GLASS("psd_apg_glass", false),
        PSD_APG_GLASS_END("psd_apg_glass_end", false);

        private final String name;
        private final boolean isDoor;

        EnumKCRPSDAPGItem(String name, boolean isDoor) {
            this.name = name;
            this.isDoor = isDoor;
        }

        @Override
        public @NotNull String getSerializedName() {
            return name;
        }
    }
}
