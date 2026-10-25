package net.hulan.ksd.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import mtr.client.ClientData;
import mtr.data.*;
import mtr.mappings.ScreenMapper;
import mtr.packet.PacketTrainDataGuiClient;
import mtr.screen.DashboardScreen;
import mtr.screen.DeleteConfirmationScreen;
import mtr.screen.WidgetBetterTextField;
import net.hulan.Tuples;
import net.hulan.ksd.client.KSDClientData;
import net.hulan.ksd.data.KSDRoute;
import net.hulan.ksd.data.KSDStation;
import net.hulan.ksd.packet.KSDPacket;
import net.hulan.ksd.packet.KSDPacketClient;
import net.minecraft.util.Tuple;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static mtr.packet.IPacket.PACKET_DELETE_ROUTE;
import static mtr.packet.IPacket.PACKET_DELETE_STATION;
import static net.hulan.ksd.packet.KSDPacket.KSD_PACKET_DELETE_ROUTE;
import static net.hulan.ksd.packet.KSDPacket.KSD_PACKET_DELETE_STATION;

@Mixin(DashboardScreen.class)
public class DashboardScreenMixin {

    @Shadow(remap = false)
    private AreaBase editingArea;

    @Shadow(remap = false)
    private boolean isNew;

    @Shadow(remap = false)
    private Route editingRoute;

    @Unique
    KSDStation editingKSDStation;

    @Unique
    KSDRoute editingKSDRoute;

    @Inject(method = "onSort",
            at = @At(value = "INVOKE",
                    target = "Lmtr/data/Route;setPlatformIds(Ljava/util/function/Consumer;)V",
                    shift =  At.Shift.AFTER),
            remap = false)
    private void onSort(CallbackInfo ci) {
        editingKSDRoute.platformIds.clear();
        editingKSDRoute.platformIds.addAll(editingRoute.platformIds);
        editingKSDRoute.setPlatformIds((packet) -> KSDPacketClient.sendUpdate(KSDPacket.KSD_PACKET_UPDATE_ROUTE, packet));
    }

    @ModifyArg(method = "onDelete", remap = false,
            at = @At(value = "INVOKE",
                    target = "Lmtr/mappings/UtilitiesClient;setScreen(Lnet/minecraft/client/Minecraft;Lmtr/mappings/ScreenMapper;)V",
                    ordinal = 0),
            index = 1)
    private ScreenMapper onDelete1(ScreenMapper screen, @Local(name = "station") Station station) {
        return new DeleteConfirmationScreen(() -> {
            PacketTrainDataGuiClient.sendDeleteData(PACKET_DELETE_STATION, station.id);
            ClientData.STATIONS.remove(station);
            KSDClientData.STATIONS.removeIf(s -> s.id == station.id);
            KSDPacketClient.sendDeleteData(KSD_PACKET_DELETE_STATION, station.id);
        }, IGui.formatStationName(station.name), (DashboardScreen) (Object) this);
    }

    @ModifyArg(method = "onDelete", remap = false,
            at = @At(value = "INVOKE",
                    target = "Lmtr/mappings/UtilitiesClient;setScreen(Lnet/minecraft/client/Minecraft;Lmtr/mappings/ScreenMapper;)V",
                    ordinal = 1),
            index = 1)
    private ScreenMapper onDelete2(ScreenMapper screen, @Local(name = "route") Route route) {
        return new DeleteConfirmationScreen(() -> {
            PacketTrainDataGuiClient.sendDeleteData(PACKET_DELETE_ROUTE, route.id);
            ClientData.ROUTES.remove(route);
            KSDClientData.ROUTES.removeIf(r -> r.id == route.id);
            KSDPacketClient.sendDeleteData(KSD_PACKET_DELETE_ROUTE, route.id);
        }, IGui.formatStationName(route.name), (DashboardScreen) (Object) this);
    }

    @Inject(method = "onDelete",
            at = @At(value = "INVOKE",
                    target = "Lmtr/data/Route;setPlatformIds(Ljava/util/function/Consumer;)V",
                    shift = At.Shift.AFTER),
            remap = false)
    private void onDelete3(NameColorDataBase data, int index, CallbackInfo ci) {
        editingKSDRoute.platformIds.remove(index);
        editingKSDRoute.setPlatformIds((packet) -> KSDPacketClient.sendUpdate(KSDPacket.KSD_PACKET_UPDATE_ROUTE, packet));
    }

    @Inject(method = "startEditingArea", remap = false,
            at = @At(value = "INVOKE",
                    target = "Lmtr/screen/DashboardScreen;toggleButtons()V"))
    private void startingEditingArea(AreaBase editingArea, boolean isNew, CallbackInfo ci) {
        if (editingArea instanceof Station) {
            if (isNew) {
                editingKSDStation = new KSDStation(editingArea.id, editingArea.transportMode);
            } else {
                editingKSDStation = KSDClientData.DATA_CACHE.stationIdMap.get(editingArea.id);
            }
        }
        editingKSDRoute = null;
    }

    @Inject(method = "startEditingRoute", remap = false,
            at = @At(value = "INVOKE",
                    target = "Lmtr/screen/DashboardScreen;toggleButtons()V"))
    private void startEditingRoute(Route editingRoute, boolean isNew, CallbackInfo ci) {
        editingKSDStation = null;
        if (isNew) {
            editingKSDRoute = new KSDRoute(editingRoute.id, editingRoute.transportMode);
        } else {
            editingKSDRoute = KSDClientData.DATA_CACHE.routeIdMap.get(editingRoute.id);
        }
    }

    @Inject(method = "onDrawCorners", remap = false,
            at = @At(value = "INVOKE",
                    target = "Lmtr/screen/DashboardScreen;toggleButtons()V"))
    private void onDrawCorners(Tuple<Integer, Integer> corner1, Tuple<Integer, Integer> corner2, CallbackInfo ci) {
        if (editingArea instanceof Station) {
            editingKSDStation.corner1 = new Tuples<>(corner1.getA(), editingKSDStation.corner1 == null ? -64 : editingKSDStation.corner1.getY(), corner1.getB());
            editingKSDStation.corner2 = new Tuples<>(corner2.getA(), editingKSDStation.corner2 == null ? 319 : editingKSDStation.corner2.getY(), corner2.getB());
        }
    }

    @Inject(method = "onDrawCornersMouseRelease",
            at = @At(value = "INVOKE",
                    target = "Lmtr/data/AreaBase;setCorners(Ljava/util/function/Consumer;)V",
                    shift =  At.Shift.AFTER),
            remap = false)
    private void onDrawCornersMouseRelease(CallbackInfo ci) {
        if (editingArea instanceof Station) {
            editingKSDStation.setCornersToMTRStation((packet) -> KSDPacketClient.sendUpdate(KSDPacket.KSD_PACKET_UPDATE_STATION, packet));
        }
    }

    @Inject(method = "onClickAddPlatformToRoute",
            at = @At(value = "INVOKE",
                    target = "Lmtr/data/Route;setPlatformIds(Ljava/util/function/Consumer;)V",
                    shift =  At.Shift.AFTER),
            remap = false)
    private  void onClickAddPlatformToRoute(long platformId, CallbackInfo ci) {
        editingKSDRoute.platformIds.add(new Route.RoutePlatform(platformId));
        editingKSDRoute.setPlatformIds((packet) -> KSDPacketClient.sendUpdate(KSDPacket.KSD_PACKET_UPDATE_ROUTE, packet));
    }

    @Inject(method = "onDoneEditingArea",
            at = @At(value = "INVOKE",
                    target = "Lmtr/data/AreaBase;setNameColor(Ljava/util/function/Consumer;)V",
                    shift =  At.Shift.AFTER),
            remap = false)
    private void onDoneEditingArea(CallbackInfo ci) {
        if (editingArea instanceof Station) {
            if (isNew) {
                KSDClientData.STATIONS.add(editingKSDStation);
            }
            editingKSDStation.name = editingArea.name;
            editingKSDStation.color = editingArea.color;
            editingKSDStation.setNameColor((packet) -> KSDPacketClient.sendUpdate(KSDPacket.KSD_PACKET_UPDATE_STATION, packet));
        }
    }

    @Inject(method = "onDoneEditingRoute",
            at = @At(value = "INVOKE",
                    target = "Lmtr/data/Route;setNameColor(Ljava/util/function/Consumer;)V",
                    shift =  At.Shift.AFTER),
            remap = false)
    private void onDoneEditingRoute(CallbackInfo ci) {
        if (isNew) {
            KSDClientData.ROUTES.add(editingKSDRoute);
        }
        editingKSDRoute.name = editingRoute.name;
        editingKSDRoute.color = editingRoute.color;
        editingKSDRoute.setNameColor((packet) -> KSDPacketClient.sendUpdate(KSDPacket.KSD_PACKET_UPDATE_ROUTE, packet));
    }

    @Inject(method = "onDoneEditingRouteDestination",
            at = @At("RETURN"),
            remap = false,
            require = 0)
    private void onDoneEditingRouteDestination(CallbackInfo ci) {
        // MTR 3.6.x removed this screen/method together with the custom-destination
        // field. On older MTR builds the fields still exist; read them reflectively so
        // the same class does not fail to apply on 3.6.x.
        try {
            java.lang.reflect.Field indexField = DashboardScreen.class.getDeclaredField("editingRoutePlatformIndex");
            java.lang.reflect.Field textField = DashboardScreen.class.getDeclaredField("textFieldCustomDestination");
            indexField.setAccessible(true);
            textField.setAccessible(true);
            int index = indexField.getInt(this);
            Object field = textField.get(this);
            if (index >= 0 && index < editingKSDRoute.platformIds.size() && field instanceof WidgetBetterTextField widget) {
                editingKSDRoute.platformIds.get(index).customDestination = widget.getValue();
                editingKSDRoute.setPlatformIds((packet) -> KSDPacketClient.sendUpdate(KSDPacket.KSD_PACKET_UPDATE_ROUTE, packet));
            }
        } catch (ReflectiveOperationException ignored) {
            // Field/method absent on this MTR version: nothing to mirror.
        }
    }
}