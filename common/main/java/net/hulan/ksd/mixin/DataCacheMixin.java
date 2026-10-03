package net.hulan.ksd.mixin;

import mtr.client.ClientData;
import mtr.data.*;
import net.hulan.ksd.client.KSDClientData;
import net.hulan.ksd.data.KSDRailwayData;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static net.hulan.ksd.KSDMain.overworld;
import static net.hulan.ksd.KSDMain.the_nether;
import static net.hulan.ksd.KSDMain.the_end;

@Mixin(DataCache.class)
public class DataCacheMixin {

    @Inject(method = "mapSavedRailIdToStation", at = @At(value = "HEAD"), cancellable = true, remap = false)
    private static <U extends SavedRailBase, V extends AreaBase> void areaIdToSavedRails(Map<Long, V> map, Set<U> savedRails, Set<V> areas, CallbackInfo ci) {
        map.clear();
        savedRails.forEach((savedRail) -> {
            BlockPos pos = savedRail.getMidPos();
            if (savedRail instanceof Platform) {
                if (areas == ClientData.STATIONS) {
                    KSDClientData.STATIONS.forEach(station -> {
                        for(V area : areas) {
                            if (Objects.equals(station.id, area.id) && station.isTransportMode(savedRail.transportMode) && station.inArea(pos)) {
                                map.put(savedRail.id, area);
                                break;
                            }
                        }
                    });
                } else {
                    RailwayData mtrOverworld = overworld == null ? null : RailwayData.getInstance(overworld);
                    KSDRailwayData ksdOverworld = overworld == null ? null : KSDRailwayData.getInstance(overworld);
                    RailwayData mtrTheNether = the_nether == null ? null : RailwayData.getInstance(the_nether);
                    KSDRailwayData ksdTheNether = the_nether == null ? null : KSDRailwayData.getInstance(the_nether);
                    RailwayData mtrTheEnd = the_end == null ? null : RailwayData.getInstance(the_end);
                    KSDRailwayData ksdTheEnd = the_end == null ? null : KSDRailwayData.getInstance(the_end);
                    if (overworld != null && the_nether != null && the_end != null) {
                        if (ksdOverworld != null && mtrOverworld != null && areas == mtrOverworld.stations) {
                            ksdOverworld.stations.forEach(station -> {
                                for(V area : areas) {
                                    if (Objects.equals(station.id, area.id) && station.isTransportMode(savedRail.transportMode) && station.inArea(pos)) {
                                        map.put(savedRail.id, area);
                                        break;
                                    }
                                }
                            });
                        }
                        if (ksdTheNether != null && mtrTheNether != null && areas == mtrTheNether.stations) {
                            ksdTheNether.stations.forEach(station -> {
                                for(V area : areas) {
                                    if (Objects.equals(station.id, area.id) && station.isTransportMode(savedRail.transportMode) && station.inArea(pos)) {
                                        map.put(savedRail.id, area);
                                        break;
                                    }
                                }
                            });
                        }
                        if (ksdTheEnd != null && mtrTheEnd != null && areas == mtrTheEnd.stations) {
                            ksdTheEnd.stations.forEach(station -> {
                                for(V area : areas) {
                                    if (Objects.equals(station.id, area.id) && station.isTransportMode(savedRail.transportMode) && station.inArea(pos)) {
                                        map.put(savedRail.id, area);
                                        break;
                                    }
                                }
                            });
                        }
                    }
                }
            }
            if (!map.containsKey(savedRail.id)) {
                for(V area : areas) {
                    if (area.isTransportMode(savedRail.transportMode) && area.inArea(pos.getX(), pos.getZ())) {
                        map.put(savedRail.id, area);
                        break;
                    }
                }
            }
        });
        ci.cancel();
    }
}
