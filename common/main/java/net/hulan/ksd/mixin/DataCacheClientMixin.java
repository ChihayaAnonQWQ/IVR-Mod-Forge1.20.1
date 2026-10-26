package net.hulan.ksd.mixin;

import mtr.client.ClientData;
import mtr.data.AreaBase;
import mtr.data.DataCache;
import mtr.data.Platform;
import mtr.data.SavedRailBase;
import net.hulan.ksd.client.KSDClientData;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Client-only DataCache mapping; kept out of the server mixin list. */
@Mixin(DataCache.class)
public class DataCacheClientMixin {

    @Inject(method = "mapSavedRailIdToStation", at = @At(value = "HEAD"), cancellable = true, remap = false)
    private static <U extends SavedRailBase, V extends AreaBase> void areaIdToSavedRails(Map<Long, V> map, Set<U> savedRails, Set<V> areas, CallbackInfo ci) {
        if (areas != ClientData.STATIONS) {
            return;
        }
        map.clear();
        savedRails.forEach((savedRail) -> {
            if (!(savedRail instanceof Platform)) {
                return;
            }
            BlockPos pos = savedRail.getMidPos();
            KSDClientData.STATIONS.forEach(station -> {
                for (V area : areas) {
                    if (Objects.equals(station.id, area.id)
                            && station.isTransportMode(savedRail.transportMode)
                            && station.inArea(pos)) {
                        map.put(savedRail.id, area);
                        break;
                    }
                }
            });
        });
        ci.cancel();
    }
}
