package net.hulan.ksd.sreen;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.client.ClientData;
import mtr.client.IDrawing;
import mtr.data.DataConverter;
import mtr.data.NameColorDataBase;
import mtr.data.Station;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import mtr.packet.PacketTrainDataGuiClient;
import mtr.screen.DashboardList;
import mtr.screen.EditNameColorScreenBase;
import mtr.screen.WidgetBetterTextField;
import net.hulan.ksd.utils.DataUtilities;
import net.hulan.ksd.data.KSDStation;
import net.hulan.ksd.packet.KSDPacket;
import net.hulan.ksd.packet.KSDPacketClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class EditKSDStationScreen extends EditNameColorScreenBase<KSDStation> {
    
    String editingExit;
    int editingDestinationIndex;
    int clickDelay;
    private final Component stationZoneText = Text.translatable("gui.mtr.zone");
    private final Component exitParentsText = Text.translatable("gui.mtr.exit_parents");
    private final Component exitDestinationsText = Text.translatable("gui.mtr.exit_destinations");
    private final WidgetBetterTextField textFieldZone;
    private final WidgetBetterTextField textFieldExitParentLetter;
    private final WidgetBetterTextField textFieldExitParentNumber;
    private final WidgetBetterTextField textFieldExitDestination;
    private final Button buttonAddExitParent;
    private final Button buttonDoneExitParent;
    private final Button buttonAddExitDestination;
    private final Button buttonDoneExitDestination;
    private final DashboardList exitParentList;
    private final DashboardList exitDestinationList;
    private final KSDDashboardScreen dashboardScreen;
    private static final int EXIT_PANELS_START = 70;

    public EditKSDStationScreen(KSDStation station, KSDDashboardScreen dashboardScreen) {
        super(station, null, "gui.mtr.station_name", "gui.mtr.station_color");
        this.dashboardScreen = dashboardScreen;
        textFieldZone = new WidgetBetterTextField(WidgetBetterTextField.TextFieldFilter.INTEGER, "", 6);
        textFieldExitParentLetter = new WidgetBetterTextField(WidgetBetterTextField.TextFieldFilter.LETTER, "A", 1);
        textFieldExitParentNumber = new WidgetBetterTextField(WidgetBetterTextField.TextFieldFilter.POSITIVE_INTEGER, "1", 2);
        textFieldExitDestination = new WidgetBetterTextField("");
        buttonAddExitParent = UtilitiesClient.newButton(Text.translatable("gui.mtr.add_exit"), (button) -> checkClickDelay(() -> changeEditingExit("", -1)));
        buttonDoneExitParent = UtilitiesClient.newButton(Text.translatable("gui.done"), (button) -> checkClickDelay(this::onDoneExitParent));
        buttonAddExitDestination = UtilitiesClient.newButton(Text.translatable("gui.mtr.add_exit_destination"), (button) -> checkClickDelay(() -> changeEditingExit(editingExit, station.exits.containsKey(editingExit) ? station.exits.get(editingExit).size() : -1)));
        buttonDoneExitDestination = UtilitiesClient.newButton(Text.translatable("gui.done"), (button) -> checkClickDelay(this::onDoneExitDestination));
        exitParentList = new DashboardList(null, null, this::onEditExitParent, null, null, this::onDeleteExitParent, null, () -> ClientData.EXIT_PARENTS_SEARCH, (text) -> ClientData.EXIT_PARENTS_SEARCH = text);
        exitDestinationList = new DashboardList(null, null, this::onEditExitDestination, this::onSortExitDestination, null, this::onDeleteExitDestination, this::getExitDestinationList, () -> ClientData.EXIT_DESTINATIONS_SEARCH, (text) -> ClientData.EXIT_DESTINATIONS_SEARCH = text);
    }

    protected void init() {
        setPositionsAndInit(0, width / 2, width / 4 * 3);
        IDrawing.setPositionAndWidth(textFieldZone, width / 4 * 3 + 2, 22, width / 4 - 4);
        int yExitText = height - 40 - 2;
        IDrawing.setPositionAndWidth(textFieldExitParentLetter, 2, yExitText, width / 4 - 4);
        IDrawing.setPositionAndWidth(textFieldExitParentNumber, 2 + width / 4, yExitText, width / 4 - 4);
        IDrawing.setPositionAndWidth(textFieldExitDestination, width / 2 + 2, yExitText, width / 2 - 4);
        IDrawing.setPositionAndWidth(buttonAddExitParent, 0, height - 20, width / 2);
        IDrawing.setPositionAndWidth(buttonDoneExitParent, 0, height - 20, width / 2);
        IDrawing.setPositionAndWidth(buttonAddExitDestination, width / 2, height - 20, width / 2);
        IDrawing.setPositionAndWidth(buttonDoneExitDestination, width / 2, height - 20, width / 2);
        textFieldZone.setValue(String.valueOf(data.zone));
        exitParentList.x = 0;
        exitParentList.y = EXIT_PANELS_START;
        exitParentList.height = height - EXIT_PANELS_START - 20;
        exitParentList.width = width / 2;
        exitDestinationList.x = width / 2;
        exitDestinationList.y = EXIT_PANELS_START;
        exitDestinationList.height = height - EXIT_PANELS_START - 20;
        exitDestinationList.width = width / 2;
        exitParentList.init(this::addDrawableChild);
        exitDestinationList.init(this::addDrawableChild);
        addDrawableChild(textFieldZone);
        addDrawableChild(textFieldExitParentLetter);
        addDrawableChild(textFieldExitParentNumber);
        addDrawableChild(textFieldExitDestination);
        addDrawableChild(buttonAddExitParent);
        addDrawableChild(buttonDoneExitParent);
        addDrawableChild(buttonAddExitDestination);
        addDrawableChild(buttonDoneExitDestination);
        changeEditingExit(null, -1);
    }

    public void tick() {
        super.tick();
        if (clickDelay > 0) {
            --clickDelay;
        }
        textFieldZone.tick();
        textFieldExitParentLetter.tick();
        textFieldExitParentNumber.tick();
        textFieldExitDestination.tick();
        exitParentList.tick();
        exitDestinationList.tick();

        List<DataConverter> exitParents = DataUtilities.sortAndMapToList(data.exits.keySet(), value -> {
            List<String> destinations = data.exits.get(value);
            String additional = destinations.size() > 1 ? "(+" + (destinations.size() - 1) + ")" : "";
            return new DataConverter(!destinations.isEmpty() ? value + "|" + destinations.get(0) + "|" + additional : value, 0);
        });
        exitParentList.setData(exitParents, false, false, true, false, false, true);
        List<DataConverter> exitDestinations = parentExists() ? DataUtilities.mapToList(data.exits.get(editingExit), value -> new DataConverter(value, 0)) : new ArrayList<>();
        exitDestinationList.setData(exitDestinations, false, false, true, true, false, true);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        try {
            renderBackground(guiGraphics);
            renderTextFields(guiGraphics);
            guiGraphics.vLine(width / 2, 50, height, Integer.MAX_VALUE);
            exitParentList.render(guiGraphics, font);
            exitDestinationList.render(guiGraphics, font);
            guiGraphics.drawCenteredString(font, stationZoneText, width / 8 * 7, 6, -1);
            guiGraphics.drawCenteredString(font, exitParentsText, width / 4, 56, -1);
            if (parentExists()) {
                guiGraphics.drawCenteredString(font, exitDestinationsText, 3 * width / 4, 56, -1);
            }
            super.render(guiGraphics, mouseX, mouseY, delta);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void mouseMoved(double mouseX, double mouseY) {
        exitParentList.mouseMoved(mouseX, mouseY);
        exitDestinationList.mouseMoved(mouseX, mouseY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        exitParentList.mouseScrolled(mouseX, mouseY, amount);
        exitDestinationList.mouseScrolled(mouseX, mouseY, amount);
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(null);
            UtilitiesClient.setScreen(minecraft, dashboardScreen);
        }
        saveData();
    }

    protected void saveData() {
        super.saveData();
        try {
            data.zone = Integer.parseInt(textFieldZone.getValue());
        } catch (Exception var2) {
            data.zone = 0;
        }
        data.setZone((packet) -> KSDPacketClient.sendUpdate(KSDPacket.KSD_PACKET_UPDATE_STATION, packet));
        executeStationUpdate(mtrStation -> {
            mtrStation.name = data.name;
            mtrStation.color = data.color;
            mtrStation.zone = data.zone;
            mtrStation.setZone((packet) -> PacketTrainDataGuiClient.sendUpdate(PACKET_UPDATE_STATION, packet));
        });
    }

    private void changeEditingExit(String editingExit, int editingDestinationIndex) {
        this.editingExit = editingExit;
        this.editingDestinationIndex = parentExists() ? editingDestinationIndex : -1;
        if (editingExit != null) {
            textFieldExitParentLetter.setValue(editingExit.toUpperCase(Locale.ENGLISH).replaceAll("[^A-Z]", ""));
            textFieldExitParentNumber.setValue(editingExit.replaceAll("\\D", ""));
        }
        if (editingDestinationIndex >= 0 && editingDestinationIndex < data.exits.get(editingExit).size()) {
            textFieldExitDestination.setValue(data.exits.get(editingExit).get(editingDestinationIndex));
        } else {
            textFieldExitDestination.setValue("");
        }
        textFieldExitParentLetter.visible = editingExit != null;
        textFieldExitParentNumber.visible = editingExit != null;
        textFieldExitDestination.visible = editingDestinationIndex >= 0;
        buttonAddExitParent.visible = editingExit == null;
        buttonDoneExitParent.visible = editingExit != null;
        buttonAddExitDestination.visible = parentExists() && editingDestinationIndex < 0;
        buttonDoneExitDestination.visible = editingDestinationIndex >= 0;
        exitDestinationList.x = parentExists() ? width / 2 : width;
        exitParentList.height = height - EXIT_PANELS_START - (editingExit == null ? 20 : 44);
        exitDestinationList.height = height - EXIT_PANELS_START - (editingDestinationIndex >= 0 ? 44 : 20);
    }

    private void onDoneExitParent() {
        String parentLetter = textFieldExitParentLetter.getValue();
        String parentNumber = textFieldExitParentNumber.getValue();
        if (!parentLetter.isEmpty() && !parentNumber.isEmpty()) {
            try {
                String exitParent = parentLetter + Integer.parseInt(parentNumber);
                data.setExitParent(editingExit, exitParent, (packet) -> KSDPacketClient.sendUpdate(KSDPacket.KSD_PACKET_UPDATE_STATION, packet));
                executeStationUpdate(mtrStation -> mtrStation.setExitParent(editingExit, exitParent, (packet) -> PacketTrainDataGuiClient.sendUpdate(PACKET_UPDATE_STATION, packet)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        changeEditingExit(null, -1);
    }

    private void onDoneExitDestination() {
        String destination = textFieldExitDestination.getValue();
        if (parentExists() && editingDestinationIndex >= 0 && !destination.isEmpty()) {
            List<String> destinations = data.exits.get(editingExit);
            if (editingDestinationIndex < destinations.size()) {
                destinations.set(editingDestinationIndex, destination);
            } else {
                destinations.add(destination);
            }
            data.setExitDestinations(editingExit, (packet) -> KSDPacketClient.sendUpdate(KSDPacket.KSD_PACKET_UPDATE_STATION, packet));
            executeStationUpdate(mtrStation -> {
                List<String> mtrDestinations = mtrStation.exits.get(editingExit);
                if (editingDestinationIndex < mtrDestinations.size()) {
                    mtrDestinations.set(editingDestinationIndex, destination);
                } else {
                    mtrDestinations.add(destination);
                }
                mtrStation.setExitDestinations(editingExit, (packet) -> PacketTrainDataGuiClient.sendUpdate(PACKET_UPDATE_STATION, packet));
            });
        }
        changeEditingExit(editingExit, -1);
    }

    private void onEditExitParent(NameColorDataBase listData, int index) {
        changeEditingExit(formatExitName(listData.name), -1);
    }

    private void onDeleteExitParent(NameColorDataBase listData, int index) {
        data.deleteExitParent(formatExitName(listData.name), (packet) -> KSDPacketClient.sendUpdate(KSDPacket.KSD_PACKET_UPDATE_STATION, packet));
        executeStationUpdate(mtrStation -> mtrStation.deleteExitParent(formatExitName(listData.name), (packet) -> PacketTrainDataGuiClient.sendUpdate(PACKET_UPDATE_STATION, packet)));
        changeEditingExit(null, -1);
    }

    private void onEditExitDestination(NameColorDataBase listData, int index) {
        changeEditingExit(editingExit, index);
    }

    private void onSortExitDestination() {
        data.setExitDestinations(editingExit, (packet) -> KSDPacketClient.sendUpdate(KSDPacket.KSD_PACKET_UPDATE_STATION, packet));
        executeStationUpdate(mtrStation -> mtrStation.setExitDestinations(editingExit, (packet) -> PacketTrainDataGuiClient.sendUpdate(PACKET_UPDATE_STATION, packet)));
        changeEditingExit(editingExit, -1);
    }

    private void onDeleteExitDestination(NameColorDataBase listData, int index) {
        if (parentExists()) {
            data.exits.get(editingExit).remove(listData.name);
            data.setExitDestinations(editingExit, (packet) -> KSDPacketClient.sendUpdate(KSDPacket.KSD_PACKET_UPDATE_STATION, packet));
            executeStationUpdate(mtrStation -> {
                mtrStation.exits.get(editingExit).remove(listData.name);
                mtrStation.deleteExitParent(editingExit, (packet) -> PacketTrainDataGuiClient.sendUpdate(PACKET_UPDATE_STATION, packet));
            });
        }
        changeEditingExit(editingExit, -1);
    }

    private List<String> getExitDestinationList() {
        return parentExists() ? data.exits.get(editingExit) : new ArrayList<>();
    }

    private void checkClickDelay(Runnable callback) {
        if (clickDelay == 0) {
            callback.run();
            clickDelay = 10;
        }

    }

    private boolean parentExists() {
        return editingExit != null && data.exits.containsKey(editingExit);
    }

    private static String formatExitName(String text) {
        return text.split("\\|")[0];
    }

    private void executeStationUpdate(Consumer<Station> action) {
        DataUtilities.execute(ClientData.STATIONS, s -> s.id == data.id, action);
    }
}
