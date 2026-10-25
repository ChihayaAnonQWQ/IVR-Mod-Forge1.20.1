package net.hulan.ksd.mixin;

import mtr.client.ClientCache;
import mtr.client.ClientData;
import mtr.data.AreaBase;
import mtr.data.Platform;
import mtr.data.SavedRailBase;
import mtr.data.Station;
import net.hulan.ksd.client.KSDClientData;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Mixin(ClientCache.class)
public class ClientCacheMixin {

    @Inject(method = "areaIdToSavedRails",
            at = @At(value = "HEAD"),
            cancellable = true,
            remap = false)
    private static <U extends AreaBase, V extends SavedRailBase> void areaIdToSavedRails(U area, Set<V> savedRails, CallbackInfoReturnable<Map<Long, V>> cir) {
        cir.setReturnValue(areaIdToSavedRails(area, savedRails));
    }

    @Unique
    private static <U extends AreaBase, V extends SavedRailBase> Map<Long, V> areaIdToSavedRails(U area, Set<V> savedRails) {
        Map<Long, V> savedRailMap = new HashMap<>();
        savedRails.forEach((savedRail) -> {
            BlockPos pos = savedRail.getMidPos();
            if (savedRail instanceof Platform && ClientData.STATIONS.contains(area)) {
                // KSD stations keep the expanded Y range, so prefer the KSD area match.
                boolean[] matchedKsdStation = {false};
                KSDClientData.STATIONS.forEach(station -> {
                    if (Objects.equals(station.id, area.id)
                            && station.isTransportMode(savedRail.transportMode)
                            && station.inArea(pos)) {
                        matchedKsdStation[0] = true;
                    }
                });
                if (matchedKsdStation[0]) {
                    savedRailMap.put(savedRail.id, savedRail);
                } else if (area.isTransportMode(savedRail.transportMode)
                        && area.inArea(pos.getX(), pos.getZ())) {
                    // KSD data may not have reached the client yet (it is sent after the
                    // MTR data). Fall back to MTR's own area test so other MTR addons
                    // still see this platform's station instead of an empty mapping.
                    savedRailMap.put(savedRail.id, savedRail);
                }
            } else if (area.isTransportMode(savedRail.transportMode)
                    && area.inArea(pos.getX(), pos.getZ())) {
                savedRailMap.put(savedRail.id, savedRail);
            }
        });
        return savedRailMap;
    }
}
