package net.hulan.ksd.sreen;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.client.ClientData;
import mtr.client.IDrawing;
import mtr.data.*;
import mtr.mappings.ScreenMapper;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import mtr.packet.IPacket;
import mtr.packet.PacketTrainDataGuiClient;
import mtr.screen.*;
import net.hulan.Tuples;
import net.hulan.ksd.client.KSDClientData;
import net.hulan.ksd.data.*;
import net.hulan.ksd.packet.KSDPacket;
import net.hulan.ksd.packet.KSDPacketClient;
import net.hulan.ksd.utils.DataUtilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.util.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class KSDDashboardScreen extends ScreenMapper implements IGui, IPacket {

    private SelectedTab selectedTab;
    private KSDStation editingStation;
    private KSDRoute editingRoute;
    private int editingRoutePlatformIndex;
    private boolean isNew;
    private final KSDWidgetMap widgetMap;
    private final Button buttonTabStations;
    private final Button buttonTabRoutes;
    private final Button buttonAddStation;
    private final Button buttonAddRoute;
    private final Button buttonDoneEditingStation;
    private final Button buttonDoneEditingRoute;
    private final Button buttonDoneEditingRouteDestination;
    private final Button buttonZoomIn;
    private final Button buttonZoomOut;
    private final Button buttonRailActions;
    private final Button buttonOptions;
    private final Button buttonSelectMapType;
    private final WidgetBetterTextField textFieldName;
    private final WidgetBetterTextField textFieldCustomDestination;
    private final WidgetColorSelector colorSelector;
    private final DashboardList dashboardList;

    public KSDDashboardScreen(TransportMode transportMode, boolean useTimeAndWindSync) {
        super(Text.literal(""));
        textFieldName = new WidgetBetterTextField(Text.translatable("gui.mtr.name").getString());
        textFieldCustomDestination = new WidgetBetterTextField(Text.translatable("gui.mtr.custom_destination_suggestion").getString());
        colorSelector = new WidgetColorSelector(this, true, this::toggleButtons);
        widgetMap = new KSDWidgetMap(KSDWidgetMap.MapType.X_Z, transportMode, this::onDrawCorners, this::onDrawCornersMouseRelease, this::onClickAddPlatformToRoute, this::onClickEditSavedRail, colorSelector::isMouseOver);
        buttonTabStations = UtilitiesClient.newButton(Text.translatable("gui.mtr.stations"), (button) -> onSelectTab(SelectedTab.STATIONS));
        buttonTabRoutes = UtilitiesClient.newButton(Text.translatable("gui.mtr.routes"), button -> onSelectTab(SelectedTab.ROUTES));
        buttonAddStation = UtilitiesClient.newButton(Text.translatable("gui.mtr.add_station"), (button) -> startEditingArea(new KSDStation(), true));
        buttonAddRoute = UtilitiesClient.newButton(Text.translatable("gui.mtr.add_route"), button -> startEditingRoute(new KSDRoute(transportMode), true));
        buttonDoneEditingStation = UtilitiesClient.newButton(Text.translatable("gui.done"), (button) -> onDoneEditingArea());
        buttonDoneEditingRoute = UtilitiesClient.newButton(Text.translatable("gui.done"), button -> onDoneEditingRoute());
        buttonDoneEditingRouteDestination = UtilitiesClient.newButton(Text.translatable("gui.done"), button -> onDoneEditingRouteDestination());
        buttonZoomIn = UtilitiesClient.newButton(Text.literal("+"), (button) -> widgetMap.scale(1.0F));
        buttonZoomOut = UtilitiesClient.newButton(Text.literal("-"), (button) -> widgetMap.scale(-1.0F));
        buttonRailActions = UtilitiesClient.newButton(Text.translatable("gui.mtr.rail_actions_button"), (button) -> {
            if (minecraft != null) {
                UtilitiesClient.setScreen(minecraft, new RailActionsScreen());
            }
        });
        buttonOptions = UtilitiesClient.newButton(Text.translatable("menu.options"), (button) -> {
            if (minecraft != null) {
                UtilitiesClient.setScreen(minecraft, new ConfigScreen(useTimeAndWindSync));
            }
        });
        buttonSelectMapType = UtilitiesClient.newButton(Text.translatable("gui.mtr.add_value"), (button) -> selectMapType(widgetMap.mapType.next()));
        dashboardList = new DashboardList(this::onFind, this::onDrawArea, this::onEdit, this::onSort, null, this::onDelete, this::getList, () -> ClientData.DASHBOARD_SEARCH, (text) -> ClientData.DASHBOARD_SEARCH = text);
        onSelectTab(SelectedTab.STATIONS);
    }

    protected void init() {
        super.init();
        final int tabCount = 2;
        int bottomRowY = height - 20;
        widgetMap.setPositionAndSize(144, 0, width - 144, height);
        IDrawing.setPositionAndWidth(buttonTabStations, 0, 0, PANEL_WIDTH / tabCount);
        IDrawing.setPositionAndWidth(buttonTabRoutes, PANEL_WIDTH / tabCount, 0, PANEL_WIDTH / tabCount);
        IDrawing.setPositionAndWidth(buttonAddStation, 0, bottomRowY, PANEL_WIDTH);
        IDrawing.setPositionAndWidth(buttonAddRoute, 0, bottomRowY, PANEL_WIDTH);
        IDrawing.setPositionAndWidth(buttonDoneEditingStation, 0, bottomRowY, PANEL_WIDTH);
        IDrawing.setPositionAndWidth(buttonDoneEditingRoute, 0, bottomRowY, PANEL_WIDTH);
        IDrawing.setPositionAndWidth(buttonDoneEditingRouteDestination, 0, bottomRowY, PANEL_WIDTH);
        IDrawing.setPositionAndWidth(buttonZoomIn, width - 40, bottomRowY, 20);
        IDrawing.setPositionAndWidth(buttonZoomOut, width - 20, bottomRowY, 20);
        IDrawing.setPositionAndWidth(buttonRailActions, width - 200, bottomRowY, 100);
        IDrawing.setPositionAndWidth(buttonOptions, width - 100, bottomRowY, 60);
        IDrawing.setPositionAndWidth(buttonSelectMapType, width - 300, bottomRowY, 100);
        IDrawing.setPositionAndWidth(textFieldName, 2, bottomRowY - 20 - 2, 92);
        IDrawing.setPositionAndWidth(textFieldCustomDestination, 2, bottomRowY - 20 - 2, 140);
        IDrawing.setPositionAndWidth(colorSelector, 98, bottomRowY - 20 - 2, 44);
        dashboardList.x = 0;
        dashboardList.y = 20;
        dashboardList.width = 144;
        toggleButtons();
        dashboardList.init(this::addDrawableChild);
        addWidget(widgetMap);
        addDrawableChild(buttonTabStations);
        addDrawableChild(buttonTabRoutes);
        addDrawableChild(buttonAddStation);
        addDrawableChild(buttonAddRoute);
        addDrawableChild(buttonDoneEditingStation);
        addDrawableChild(buttonDoneEditingRoute);
        addDrawableChild(buttonDoneEditingRouteDestination);
        addDrawableChild(buttonZoomIn);
        addDrawableChild(buttonZoomOut);
        addDrawableChild(buttonRailActions);
        addDrawableChild(buttonOptions);
        addDrawableChild(buttonSelectMapType);
        addDrawableChild(textFieldName);
        addDrawableChild(textFieldCustomDestination);
        addDrawableChild(colorSelector);
        selectMapType(KSDWidgetMap.MapType.X_Z);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        try {
            renderBackground(guiGraphics);
            // Flush the batched background before the widget map draws through raw vertex buffers.
            guiGraphics.flush();
            widgetMap.render(guiGraphics, mouseX, mouseY, delta);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0F, 0.0F, 500.0F);
            guiGraphics.fill(0, 0, 144, height, ARGB_BACKGROUND);
            dashboardList.render(guiGraphics, font);
            super.render(guiGraphics, mouseX, mouseY, delta);
            guiGraphics.pose().popPose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void mouseMoved(double mouseX, double mouseY) {
        dashboardList.mouseMoved(mouseX, mouseY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        dashboardList.mouseScrolled(mouseX, mouseY, amount);
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    public void tick() {
        textFieldName.tick();
        textFieldCustomDestination.tick();
        dashboardList.tick();
        try {
            switch (selectedTab) {
                case STATIONS -> {
                    if (editingStation == null) {
                        dashboardList.setData(KSDClientData.STATIONS, true, true, true, false, false, true);
                    } else {
                        Map<Long, KSDPlatform> platformData = KSDClientData.DATA_CACHE.requestStationIdToPlatforms(editingStation.id);
                        this.dashboardList.setData(platformData == null ? new ArrayList<>() : new ArrayList<>(platformData.values()), true, false, true, false, false, false);
                    }
                }
                case ROUTES -> {
                    if (editingRoute == null) {
                        dashboardList.setData(ClientData.getFilteredDataSet(TransportMode.TRAIN, KSDClientData.ROUTES), false, true, true, false, false, true);
                    } else {
                        final List<DataConverter> routeData = DataUtilities.mapAndNonNullToList(editingRoute.platformIds, platformId -> {
                            final KSDPlatform platform = KSDClientData.DATA_CACHE.platformIdMap.get(platformId.platformId);
                            if (platform == null) {
                                return null;
                            } else {
                                final String customDestinationPrefix = platformId.customDestination.isEmpty() ?
                                        "" : KSDRoute.destinationIsReset(platformId.customDestination) ? "\"" : "*";
                                final KSDStation station = KSDClientData.DATA_CACHE.platformIdToStation.get(platform.id);
                                if (station != null) {
                                    return new DataConverter(String.format("%s%s (%s)", customDestinationPrefix, station.name, platform.name), station.color);
                                } else {
                                    return new DataConverter(String.format("%s(%s)", customDestinationPrefix, platform.name), 0);
                                }
                            }
                        });
                        dashboardList.setData(routeData, false, false, true, true, false, true);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isPauseScreen() {
        return false;
    }

    private void onSelectTab(SelectedTab tab) {
        selectedTab = tab;
        buttonTabStations.active = tab != SelectedTab.STATIONS;
        buttonTabRoutes.active = tab != SelectedTab.ROUTES;
        stopEditing();
    }

    private void onFind(NameColorDataBase data, int index) {
        if (selectedTab == SelectedTab.STATIONS) {
            if (editingStation == null && data instanceof KSDAreaBase area) {
                if (KSDAreaBase.nonNullCorners(area)) {
                    widgetMap.find((double) area.corner1.getX(), (double) area.corner1.getZ(), (double) area.corner2.getX(), (double) area.corner2.getZ());
                }
            } else {
                final KSDPlatform platform = (KSDPlatform) data;
                widgetMap.find(platform.getMidPos());
            }
        }
    }

    private void onDrawArea(NameColorDataBase data, int index) {
        switch (selectedTab) {
            case STATIONS -> {
                if (editingStation == null && data instanceof KSDStation) {
                    startEditingArea((KSDStation) data, false);
                }
            }
            case ROUTES -> {
                if (data instanceof KSDRoute) {
                    startEditingRoute((KSDRoute) data, false);
                }
            }
        }
        dashboardList.clearSearch();
    }

    private void onEdit(NameColorDataBase data, int index) {
        if (minecraft != null) {
            switch (selectedTab) {
                case STATIONS -> {
                    if (editingStation == null) {
                        if (data instanceof KSDStation) {
                            UtilitiesClient.setScreen(minecraft, new EditKSDStationScreen((KSDStation) data, this));
                        } else if (data instanceof KSDPlatform) {
                            UtilitiesClient.setScreen(minecraft, new KSDPlatformScreen((KSDPlatform)data, TransportMode.TRAIN, this));
                        }
                    }
                }
                case ROUTES -> {
                    if (editingRoute == null && data instanceof KSDRoute) {
                        UtilitiesClient.setScreen(minecraft, new EditKSDRouteScreen((KSDRoute) data, this));
                    } else {
                        startEditingRouteDestination(index);
                    }
                }
            }
        }
    }

    private void onSort() {
        if (selectedTab == SelectedTab.ROUTES && editingRoute != null) {
            editingRoute.setPlatformIds(packet -> KSDPacketClient.sendUpdate(KSDPacket.KSD_PACKET_UPDATE_ROUTE, packet));
            executeRouteUpdate(mtrRoute -> {
                mtrRoute.platformIds.clear();
                mtrRoute.platformIds.addAll(editingRoute.platformIds);
                mtrRoute.setPlatformIds(packet -> PacketTrainDataGuiClient.sendUpdate(PACKET_UPDATE_ROUTE, packet));
            });
        }
    }

    private void onDelete(NameColorDataBase data, int index) {
        try {
            if (minecraft != null) {
                switch (selectedTab) {
                    case STATIONS -> {
                        KSDStation station = (KSDStation) data;
                        UtilitiesClient.setScreen(minecraft, new ConfirmationScreen(() -> {
                            KSDPacketClient.sendDeleteData(KSDPacket.KSD_PACKET_DELETE_STATION, station.id);
                            KSDClientData.STATIONS.remove(station);
                            PacketTrainDataGuiClient.sendDeleteData(PACKET_DELETE_STATION, station.id);
                            ClientData.STATIONS.removeIf(mtrStation -> mtrStation.id == station.id);
                        }, Text.translatable("gui.mtr.delete_confirmation", IGui.formatStationName(station.name)), this));
                    }
                    case ROUTES -> {
                        if (editingRoute == null) {
                            if (data instanceof KSDRoute route) {
                                UtilitiesClient.setScreen(minecraft, new ConfirmationScreen(() -> {
                                    KSDPacketClient.sendDeleteData(KSDPacket.KSD_PACKET_DELETE_ROUTE, route.id);
                                    KSDClientData.ROUTES.remove(route);
                                    PacketTrainDataGuiClient.sendDeleteData(PACKET_DELETE_ROUTE, route.id);
                                    ClientData.ROUTES.removeIf(mtrRoute -> mtrRoute.id == route.id);
                                }, Text.translatable("gui.mtr.delete_confirmation", IGui.formatStationName(route.name)), this));
                            }
                        } else {
                            editingRoute.platformIds.remove(index);
                            editingRoute.setPlatformIds(packet -> KSDPacketClient.sendUpdate(KSDPacket.KSD_PACKET_UPDATE_ROUTE, packet));
                            executeRouteUpdate(mtrRoute -> {
                                mtrRoute.platformIds.remove(index);
                                mtrRoute.setPlatformIds(packet -> PacketTrainDataGuiClient.sendUpdate(PACKET_UPDATE_ROUTE, packet));
                            });
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Route.RoutePlatform> getList() {
        return editingRoute == null ? new ArrayList<>() : editingRoute.platformIds;
    }

    private void startEditingArea(KSDStation editingStation, boolean isNew) {
        this.editingStation = editingStation;
        editingRoute = null;
        this.isNew = isNew;
        textFieldName.setValue(editingStation.name);
        colorSelector.setColor(editingStation.color);
        widgetMap.startEditingArea(editingStation);
        toggleButtons();
    }

    private void startEditingRoute(KSDRoute editingRoute, boolean isNew) {
        editingStation = null;
        this.editingRoute = editingRoute;
        this.isNew = isNew;
        editingRoutePlatformIndex = -1;
        textFieldName.setValue(editingRoute.name);
        colorSelector.setColor(editingRoute.color);
        widgetMap.startEditingRoute();
        toggleButtons();
    }

    private void startEditingRouteDestination(int index) {
        editingRoutePlatformIndex = index;
        if (isValidRoutePlatformIndex()) {
            textFieldCustomDestination.setValue(editingRoute.platformIds.get(index).customDestination);
        }
        toggleButtons();
    }

    private void onDrawCorners(Tuple<Integer, Integer> corner1, Tuple<Integer, Integer> corner2) {
        switch (widgetMap.mapType) {
            case X_Y -> {
                editingStation.corner1 = editingStation.corner1 == null ? new Tuples<>(corner1.getA(), corner1.getB(), 0) : new Tuples<>(corner1.getA(), corner1.getB(), editingStation.corner1.getZ());
                editingStation.corner2 = editingStation.corner2 == null ? new Tuples<>(corner2.getA(), corner2.getB(), 0) : new Tuples<>(corner2.getA(), corner2.getB(), editingStation.corner2.getZ());
                executeStationUpdate(mtrStation -> {
                    mtrStation.corner1 = new Tuple<>(corner1.getA(), mtrStation.corner1.getB());
                    mtrStation.corner2 = new Tuple<>(corner2.getA(), mtrStation.corner2.getB());
                });
            }
            case X_Z -> {
                editingStation.corner1 = editingStation.corner1 == null ? new Tuples<>(corner1.getA(), -64, corner1.getB()) : new Tuples<>(corner1.getA(), editingStation.corner1.getY(), corner1.getB());
                editingStation.corner2 = editingStation.corner2 == null ? new Tuples<>(corner2.getA(), 319, corner2.getB()) : new Tuples<>(corner2.getA(), editingStation.corner2.getY(), corner2.getB());
                executeStationUpdate(mtrStation -> {
                    mtrStation.corner1 = new Tuple<>(corner1.getA(), corner1.getB());
                    mtrStation.corner2 = new Tuple<>(corner2.getA(), corner2.getB());
                });
            }
            case Z_Y -> {
                editingStation.corner1 = editingStation.corner1 == null ? new Tuples<>(0, corner1.getB(), corner1.getA()) : new Tuples<>(editingStation.corner1.getX(), corner1.getB(), corner1.getA());
                editingStation.corner2 = editingStation.corner2 == null ? new Tuples<>(0, corner2.getB(), corner2.getA()) : new Tuples<>(editingStation.corner2.getX(), corner2.getB(), corner2.getA());
                executeStationUpdate(mtrStation -> {
                    mtrStation.corner1 = new Tuple<>(mtrStation.corner1.getA(), corner1.getA());
                    mtrStation.corner2 = new Tuple<>(mtrStation.corner2.getA(), corner2.getA());
                });
            }
        }
        toggleButtons();
    }

    private void onDrawCornersMouseRelease() {
        editingStation.setCorners((packet) -> KSDPacketClient.sendUpdate(KSDPacket.KSD_PACKET_UPDATE_STATION, packet));
        executeRouteUpdate(mtrRoute -> mtrRoute.setNameColor(packet -> PacketTrainDataGuiClient.sendUpdate(PACKET_UPDATE_ROUTE, packet)));
    }

    private void onClickAddPlatformToRoute(long platformId) {
        editingRoute.platformIds.add(new Route.RoutePlatform(platformId));
        editingRoute.setPlatformIds(packet -> KSDPacketClient.sendUpdate(KSDPacket.KSD_PACKET_UPDATE_ROUTE, packet));
        executeRouteUpdate(mtrRoute -> {
            mtrRoute.platformIds.add(new Route.RoutePlatform(platformId));
            mtrRoute.setPlatformIds(packet -> PacketTrainDataGuiClient.sendUpdate(PACKET_UPDATE_ROUTE, packet));
        });
    }

    private void onClickEditSavedRail(SavedRailBase savedRail) {
        if (savedRail instanceof KSDPlatform) {
            UtilitiesClient.setScreen(Minecraft.getInstance(), new KSDPlatformScreen((KSDPlatform) savedRail, TransportMode.TRAIN, this));
        }
    }

    private void onDoneEditingArea() {
        if (isNew) {
            KSDClientData.STATIONS.add(editingStation);
            ClientData.STATIONS.add(editingStation.toMTRStation());
        }
        editingStation.name = IGui.textOrUntitled(textFieldName.getValue());
        editingStation.color = colorSelector.getColor();
        editingStation.setNameColor((packet) -> KSDPacketClient.sendUpdate(KSDPacket.KSD_PACKET_UPDATE_STATION, packet));
        executeStationUpdate(mtrStation -> {
            mtrStation.name = IGui.textOrUntitled(textFieldName.getValue());
            mtrStation.color = colorSelector.getColor();
            mtrStation.setNameColor((packet) -> PacketTrainDataGuiClient.sendUpdate(PACKET_UPDATE_STATION, packet));
            mtrStation.setCorners((packet) -> PacketTrainDataGuiClient.sendUpdate(PACKET_UPDATE_STATION, packet));
        });
        stopEditing();
    }

    private void onDoneEditingRoute() {
        if (isNew) {
            KSDClientData.ROUTES.add(editingRoute);
            ClientData.ROUTES.add(editingRoute.toMTRRoute());
        }
        editingRoute.name = IGui.textOrUntitled(textFieldName.getValue());
        editingRoute.color = colorSelector.getColor();
        editingRoute.setNameColor(packet -> KSDPacketClient.sendUpdate(KSDPacket.KSD_PACKET_UPDATE_ROUTE, packet));
        executeRouteUpdate(mtrRoute -> {
            mtrRoute.name = IGui.textOrUntitled(textFieldName.getValue());
            mtrRoute.color = colorSelector.getColor();
            mtrRoute.setNameColor(packet -> PacketTrainDataGuiClient.sendUpdate(PACKET_UPDATE_ROUTE, packet));
        });
        stopEditing();
    }

    private void onDoneEditingRouteDestination() {
        if (isValidRoutePlatformIndex()) {
            editingRoute.platformIds.get(editingRoutePlatformIndex).customDestination = textFieldCustomDestination.getValue();
            editingRoute.setPlatformIds(packet -> KSDPacketClient.sendUpdate(KSDPacket.KSD_PACKET_UPDATE_ROUTE, packet));
            executeRouteUpdate(mtrRoute -> {
                mtrRoute.platformIds.get(editingRoutePlatformIndex).customDestination = textFieldCustomDestination.getValue();
                mtrRoute.setPlatformIds(packet -> KSDPacketClient.sendUpdate(PACKET_UPDATE_ROUTE, packet));
            });
        }
        startEditingRoute(editingRoute, isNew);
    }

    private void stopEditing() {
        editingStation = null;
        editingRoute = null;
        widgetMap.stopEditing();
        toggleButtons();
    }

    private boolean isValidRoutePlatformIndex() {
        return editingRoute != null && editingRoutePlatformIndex >= 0 && editingRoutePlatformIndex < editingRoute.platformIds.size();
    }

    private void toggleButtons() {
        final boolean hasPermission = ClientData.hasPermission();
        final boolean showRouteDestinationFields = isValidRoutePlatformIndex();
        buttonAddStation.visible = selectedTab == SelectedTab.STATIONS && editingStation == null && hasPermission;
        buttonAddRoute.visible = selectedTab == SelectedTab.ROUTES && editingRoute == null && hasPermission;
        buttonDoneEditingStation.visible = selectedTab == SelectedTab.STATIONS && editingStation != null;
        buttonDoneEditingStation.active = KSDAreaBase.nonNullCorners(editingStation);
        buttonDoneEditingRoute.visible = selectedTab == SelectedTab.ROUTES && editingRoute != null && !showRouteDestinationFields;
        buttonDoneEditingRouteDestination.visible = selectedTab == SelectedTab.ROUTES && editingRoute != null && showRouteDestinationFields;
        final boolean showTextFields = (selectedTab == SelectedTab.STATIONS && editingStation != null) || (selectedTab == SelectedTab.ROUTES && editingRoute != null && !showRouteDestinationFields);
        textFieldName.visible = showTextFields;
        textFieldCustomDestination.visible = showRouteDestinationFields;
        colorSelector.visible = showTextFields;
        dashboardList.height = height - SQUARE_SIZE * 2 - (showTextFields || showRouteDestinationFields ? SQUARE_SIZE + TEXT_FIELD_PADDING : 0);
    }

    private void selectMapType(KSDWidgetMap.MapType mapType) {
        widgetMap.setMapType(mapType, editingStation);
        buttonSelectMapType.setMessage(Text.translatable(String.format("gui.ksd.map_type.%s", mapType).toLowerCase()));
    }

    private void executeStationUpdate(Consumer<Station> action) {
        if (editingStation != null) {
            DataUtilities.execute(ClientData.STATIONS, s -> s.id == editingStation.id, action);
        }
    }

    private void executeRouteUpdate(Consumer<Route> action) {
        if (editingRoute != null) {
            DataUtilities.execute(ClientData.ROUTES, r -> r.id == editingRoute.id, action);
        }
    }

    private enum SelectedTab {
        STATIONS,
        ROUTES
    }
}
