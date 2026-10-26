package net.hulan.ksd.mixin;

import mtr.client.ClientData;
import mtr.data.*;
import net.hulan.ksd.client.KSDClientData;
import net.hulan.ksd.data.KSDPlatform;
import net.hulan.ksd.data.KSDStation;
import net.hulan.ksd.data.KSDRailwayData;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Set;

/**
 * Client-only RailwayData hooks. Kept separate from RailwayDataMixin so the common
 * (server-loaded) mixin never references ClientData/KSDClientData, whose static
 * initialisation pulls in net.minecraft.client.LocalPlayer on a dedicated server
 * (RuntimeDistCleaner crash).
 */
@Mixin(RailwayData.class)
public class RailwayDataClientMixin {

    @Unique
    private static Station ivr$toMtrStation(KSDStation station) {
        if (station == null || station.name == null || station.name.isEmpty()) {
            return null;
        }
        return station.toMTRStation();
    }

    @Inject(method = "getStation", remap = false, at = @At("HEAD"), cancellable = true)
    private static void getStation(Set<Station> stations, DataCache dataCache, BlockPos pos, CallbackInfoReturnable<Station> cir) {
        if (stations != ClientData.STATIONS) {
            return;
        }
        Station mtrStation = ivr$toMtrStation(KSDRailwayData.getStation(KSDClientData.STATIONS, pos));
        if (mtrStation != null) {
            cir.setReturnValue(mtrStation);
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
        if (platforms != ClientData.PLATFORMS) {
            return;
        }
        KSDClientData.PLATFORMS.add(new KSDPlatform(savedRailId, transportMode, posStart, posEnd));
        KSDClientData.DATA_CACHE.sync();
    }
}
