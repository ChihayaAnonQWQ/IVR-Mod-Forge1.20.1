package net.hulan.ksd.mixin;

import mtr.block.BlockDirectionalDoubleBlockBase;
import mtr.block.BlockTicketProcessor;
import mtr.block.IBlock;
import mtr.data.TicketSystem;
import mtr.mappings.Utilities;
import net.hulan.ksd.data.KCRTicketSystem;
import net.hulan.ksd.item.ItemOctopus;
import net.hulan.ksd.item.ItemSingleTicket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static mtr.block.BlockTicketProcessor.LIGHTS;

@Mixin(BlockTicketProcessor.class)
public class BlockTicketProcessorMixin extends BlockDirectionalDoubleBlockBase {

    @Shadow(remap = false)
    public boolean canEnter;

    @Shadow(remap = false)
    public boolean canExit;

    public BlockTicketProcessorMixin(Properties settings) {
        super(settings);
    }

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand interactionHand,
                     BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (world.isClientSide || IBlock.getStatePropertySafe(state, HALF) != DoubleBlockHalf.UPPER) {
            return;
        }
        ItemStack ticket = findTicket(player);
        if (ticket.isEmpty()) {
            // Not an IVR ticket: leave MTR's own ticket processor alone.
            return;
        }
        TicketSystem.EnumTicketBarrierOpen canOpen = KCRTicketSystem.check(
                world, player, pos, ticket, canEnter, canExit,
                ticket.getItem() instanceof ItemOctopus);
        world.setBlockAndUpdate(pos, state.setValue(LIGHTS,
                canOpen.isOpen() ? BlockTicketProcessor.EnumTicketProcessorLights.GREEN
                        : BlockTicketProcessor.EnumTicketProcessorLights.RED));
        Utilities.scheduleBlockTick(world, pos, this, 20);
        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    private static ItemStack findTicket(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof ItemSingleTicket || mainHand.getItem() instanceof ItemOctopus) {
            return mainHand;
        }
        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof ItemSingleTicket || offHand.getItem() instanceof ItemOctopus) {
            return offHand;
        }
        return ItemStack.EMPTY;
    }
}
