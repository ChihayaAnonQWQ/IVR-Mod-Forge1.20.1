package net.hulan.ksd.mixin;

import mtr.data.TicketSystem;
import net.hulan.ksd.data.KCRTicketSystem;
import net.hulan.ksd.data.KSDRailwayData;
import net.hulan.ksd.item.ItemOctopus;
import net.hulan.ksd.item.ItemSingleTicket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

/**
 * Lets IVR/KSD tickets work with addon barriers that call MTR's
 * TicketSystem.passThrough directly (for example Nanbin's CRT barrier), instead of
 * only with MTR's own BlockTicketBarrier.
 */
@Mixin(TicketSystem.class)
public class TicketSystemMixin {

    /** Addon barriers may also call passThrough from entityInside every tick. */
    private static final Map<String, Long> ivr$nextCheck = new HashMap<>();

    @Inject(method = "passThrough", at = @At("HEAD"), cancellable = true, remap = false)
    private static void passThrough(Level world, BlockPos pos, Player player, boolean canEnter, boolean canExit,
                                    SoundEvent sound1, SoundEvent sound2, SoundEvent sound3,
                                    SoundEvent sound4, SoundEvent sound5, boolean flag,
                                    CallbackInfoReturnable<TicketSystem.EnumTicketBarrierOpen> cir) {
        ItemStack ticket = findTicket(player);
        if (ticket.isEmpty()) {
            return;
        }
        final KSDRailwayData railwayData = KSDRailwayData.getInstance(world);
        if (railwayData != null) {
            final CompoundTag itemTag = ticket.getOrCreateTag();
            final boolean entered = KCRTicketSystem.isEntered(itemTag, railwayData.stations,
                    ticket.getItem() instanceof ItemOctopus);
            if ((canEnter && !canExit && entered) || (!canEnter && canExit && !entered)) {
                return;
            }
        }
        final String key = player.getUUID() + "@" + pos.asLong();
        final long now = world.getGameTime();
        final Long next = ivr$nextCheck.get(key);
        if (next != null && now < next) {
            return;
        }
        ivr$nextCheck.put(key, now + 40);
        cir.setReturnValue(KCRTicketSystem.check(world, player, pos, ticket, canEnter, canExit,
                ticket.getItem() instanceof ItemOctopus));
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
