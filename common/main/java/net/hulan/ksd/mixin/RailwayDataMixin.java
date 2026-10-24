package net.hulan.ksd.mixin;

import io.netty.buffer.Unpooled;
import mtr.Registry;
import mtr.client.ClientData;
import mtr.data.*;
import net.hulan.ksd.client.KSDClientData;
import net.hulan.ksd.data.*;
import net.hulan.ksd.packet.KSDPacket;
import net.hulan.ksd.utils.DataUtilities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Set;

import static net.hulan.ksd.KSDMain.overworld;
import static net.hulan.ksd.KSDMain.the_nether;
import static net.hulan.ksd.KSDMain.the_end;

@Mixin(RailwayData.class)
public class RailwayDataMixin {

    @Inject(method = "getStation",
            remap = false,
            at = @At(value = "HEAD"),
            cancellable = true)
    private static void getStation(Set<Station> stations, DataCache dataCache, BlockPos pos, CallbackInfoReturnable<Station> cir) {
        if (stations == ClientData.STATIONS) {
            KSDStation station = KSDRailwayData.getStation(KSDClientData.STATIONS, pos);
            cir.setReturnValue(station != null ? station.toMTRStation() : null);
        } else {
            RailwayData mtrOverworld = overworld == null ? null : RailwayData.getInstance(overworld);
            KSDRailwayData ksdOverworld = overworld == null ? null : KSDRailwayData.getInstance(overworld);
            RailwayData mtrTheNether = the_nether == null ? null : RailwayData.getInstance(the_nether);
            KSDRailwayData ksdTheNether = the_nether == null ? null : KSDRailwayData.getInstance(the_nether);
            RailwayData mtrTheEnd = the_end == null ? null : RailwayData.getInstance(the_end);
            KSDRailwayData ksdTheEnd = the_end == null ? null : KSDRailwayData.getInstance(the_end);
            if (ksdOverworld != null && mtrOverworld != null && stations == mtrOverworld.stations) {
                KSDStation station = KSDRailwayData.getStation(ksdOverworld.stations, pos);
                cir.setReturnValue(station != null ? station.toMTRStation() : null);
            } else if (ksdTheNether != null && mtrTheNether != null && stations == mtrTheNether.stations) {
                KSDStation station = KSDRailwayData.getStation(ksdTheNether.stations, pos);
                cir.setReturnValue(station != null ? station.toMTRStation() : null);
            } else if (ksdTheEnd != null && mtrTheEnd != null && stations == mtrTheEnd.stations) {
                KSDStation station = KSDRailwayData.getStation(ksdTheEnd.stations, pos);
                cir.setReturnValue(station != null ? station.toMTRStation() : null);
            }
        }
    }

    @Inject(method = "addRail(Ljava/util/Map;Ljava/util/Set;Ljava/util/Set;Lmtr/data/TransportMode;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lmtr/data/Rail;J)V",
            remap = false,
            at = @At(value = "INVOKE",
                    target = "Ljava/util/Set;add(Ljava/lang/Object;)Z",
                    ordinal = 0,
                    shift = At.Shift.AFTER))
    private static void addRail(Map<BlockPos, Map<BlockPos, Rail>> rails,
                                Set<Platform> platforms,
                                Set<Siding> sidings,
                                TransportMode transportMode,
                                BlockPos posStart,
                                BlockPos posEnd,
                                Rail rail,
                                long savedRailId,
                                CallbackInfo ci) {
        if (platforms == ClientData.PLATFORMS) {
            KSDClientData.PLATFORMS.add(new KSDPlatform(savedRailId, transportMode, posStart, posEnd));
            KSDClientData.DATA_CACHE.sync();
        } else {
            RailwayData mtrOverworld = overworld == null ? null : RailwayData.getInstance(overworld);
            KSDRailwayData ksdOverworld = overworld == null ? null : KSDRailwayData.getInstance(overworld);
            RailwayData mtrTheNether = the_nether == null ? null : RailwayData.getInstance(the_nether);
            KSDRailwayData ksdTheNether = the_nether == null ? null : KSDRailwayData.getInstance(the_nether);
            RailwayData mtrTheEnd = the_end == null ? null : RailwayData.getInstance(the_end);
            KSDRailwayData ksdTheEnd = the_end == null ? null : KSDRailwayData.getInstance(the_end);
            if (ksdOverworld != null && mtrOverworld != null && platforms == mtrOverworld.platforms) {
                ksdOverworld.platforms.add(new KSDPlatform(savedRailId, transportMode, posStart, posEnd));
                ksdOverworld.dataCache.sync();
            } else if (ksdTheNether != null && mtrTheNether != null && platforms == mtrTheNether.platforms) {
                ksdTheNether.platforms.add(new KSDPlatform(savedRailId, transportMode, posStart, posEnd));
                ksdTheNether.dataCache.sync();
            } else if (ksdTheEnd != null && mtrTheEnd != null && platforms == mtrTheEnd.platforms) {
                ksdTheEnd.platforms.add(new KSDPlatform(savedRailId, transportMode, posStart, posEnd));
                ksdTheEnd.dataCache.sync();
            }
        }
    }

    @Inject(method = "lambda$removeSavedRailS2C$39",
            at = @At(value = "INVOKE",
                    target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V",
                    shift =  At.Shift.AFTER),
            remap = false)
    private static void removeSavedRailS2C(Map<BlockPos, Map<BlockPos, Rail>> rails,
                                           Level world,
                                           ResourceLocation packetId,
                                           SavedRailBase savedRailBase,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (savedRailBase instanceof Platform) {
            KSDRailwayData railwayData = KSDRailwayData.getInstance(world);
            if (railwayData != null) {
                KSDPlatform platform = DataUtilities.getPlatform(railwayData.platforms, savedRailBase.id);
                if (platform != null) {
                    railwayData.platforms.remove(platform);
                    railwayData.dataCache.sync();
                    FriendlyByteBuf packet = new FriendlyByteBuf(Unpooled.buffer());
                    packet.writeLong(platform.id);
                    world.players().forEach((player) -> Registry.sendToPlayer((ServerPlayer)player, KSDPacket.KSD_PACKET_DELETE_PLATFORM, packet));
                }
            }
        }
    }
}
