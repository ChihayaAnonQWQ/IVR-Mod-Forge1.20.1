package net.hulan.ksd.mixin;

import mtr.block.BlockTicketBarrier;
import mtr.data.TicketSystem;
import mtr.mappings.BlockDirectionalMapper;
import mtr.mappings.Utilities;
import net.hulan.ksd.data.KCRTicketSystem;
import net.hulan.ksd.item.ItemOctopus;
import net.hulan.ksd.item.ItemSingleTicket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static mtr.block.BlockTicketBarrier.OPEN;

@Mixin(BlockTicketBarrier.class)
public class BlockTicketBarrierMixin extends BlockDirectionalMapper {

    @Shadow(remap = false)
    @Final
    private boolean isEntrance;

    public BlockTicketBarrierMixin(Properties properties) {
        super(properties);
    }

    /**
     * MTR 3.6.x moved the whole ticket-barrier interaction into entityInside(). The old
     * port cancelled entityInside unconditionally and replaced a use() method that no
     * longer exists, so all MTR and addon ticket barriers stopped working. Only take over
     * when the player actually carries an IVR/KSD ticket; otherwise let MTR run.
     */
    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true, require = 0)
    private void entityInside(BlockState state, Level world, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (world.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        ItemStack ticket = findTicket(player);
        if (ticket.isEmpty()) {
            return;
        }
        TicketSystem.EnumTicketBarrierOpen canOpen = KCRTicketSystem.check(
                world, player, pos, ticket, isEntrance, !isEntrance,
                ticket.getItem() instanceof ItemOctopus);
        world.setBlockAndUpdate(pos, state.setValue(OPEN, canOpen));
        if (canOpen.isOpen() && !world.getBlockTicks().hasScheduledTick(pos, this)) {
            Utilities.scheduleBlockTick(world, pos, this, 40);
        }
        ci.cancel();
    }

    /**
     * MTR 3.2.x still has a right-click use() method; keep this path optional for older
     * MTR versions. It is not applied on 3.6.x (the method is absent).
     */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true, require = 0)
    private void use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand interactionHand,
                     BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (world.isClientSide) {
            return;
        }
        ItemStack ticket = findTicket(player);
        if (ticket.isEmpty()) {
            return;
        }
        TicketSystem.EnumTicketBarrierOpen canOpen = KCRTicketSystem.check(
                world, player, pos, ticket, isEntrance, !isEntrance,
                ticket.getItem() instanceof ItemOctopus);
        world.setBlockAndUpdate(pos, state.setValue(OPEN, canOpen));
        if (canOpen.isOpen() && !world.getBlockTicks().hasScheduledTick(pos, this)) {
            Utilities.scheduleBlockTick(world, pos, this, 40);
        }
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
