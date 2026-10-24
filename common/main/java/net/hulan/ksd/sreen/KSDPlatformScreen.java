package net.hulan.ksd.sreen;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.client.ClientData;
import mtr.client.IDrawing;
import mtr.data.TransportMode;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import mtr.packet.PacketTrainDataGuiClient;
import mtr.screen.SavedRailScreenBase;
import mtr.screen.WidgetBetterCheckbox;
import net.hulan.ksd.utils.DataUtilities;
import net.hulan.ksd.data.KSDPlatform;
import net.hulan.ksd.packet.KSDPacketClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

import static net.hulan.ksd.packet.KSDPacket.KSD_PACKET_UPDATE_PLATFORM;

public class KSDPlatformScreen extends SavedRailScreenBase<KSDPlatform> {

    private KSDPlatform.DoorOpeningSide doorOpeningSide;
    private static final Component DWELL_TIME_TEXT = Text.translatable("gui.mtr.dwell_time");
    private final WidgetBetterCheckbox buttonIsSpanishPlatform;
    private final Button buttonDoorOpeningSide;
    private final KSDDashboardScreen dashboardScreen;
    private static final int CHECKBOX_WIDTH = 160;

    public KSDPlatformScreen(KSDPlatform savedRailBase, TransportMode transportMode, KSDDashboardScreen dashboardScreen) {
        super(savedRailBase, transportMode, null, DWELL_TIME_TEXT);
        this.dashboardScreen = dashboardScreen;
        buttonIsSpanishPlatform = new WidgetBetterCheckbox(0, 0, 0, SQUARE_SIZE, Text.translatable("gui.ksd.is_spanish_platform"), this::setIsSpanishPlatform);
        buttonDoorOpeningSide = UtilitiesClient.newButton(Text.translatable("gui.mtr.add_value"), button -> setDoorOpeningSideText(doorOpeningSide.next()));
    }

    protected void init() {
        super.init();
        UtilitiesClient.setWidgetY(sliderDwellTimeMin, 44);
        UtilitiesClient.setWidgetY(sliderDwellTimeSec, 54);
        IDrawing.setPositionAndWidth(buttonIsSpanishPlatform, 20 + textWidth, 100, CHECKBOX_WIDTH);
        IDrawing.setPositionAndWidth(buttonDoorOpeningSide, 20 + textWidth, 100 + SQUARE_SIZE, CHECKBOX_WIDTH);
        setDoorOpeningSideText(savedRailBase.doorOpeningSide);
        addDrawableChild(buttonIsSpanishPlatform);
        addDrawableChild(buttonDoorOpeningSide);
        setIsSpanishPlatform(savedRailBase.isSpanishPlatform);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        if (showScheduleControls) {
            guiGraphics.drawString(font, DWELL_TIME_TEXT, 20, 50, -1);
        }
    }

    public void onClose() {
        int minutes = sliderDwellTimeMin.getIntValue();
        float second = (float) sliderDwellTimeSec.getIntValue() / 2.0F;
        savedRailBase.setDwellTime((int) ((second + (float) (minutes * 60)) * 2.0F), (packet) -> KSDPacketClient.sendUpdate(KSD_PACKET_UPDATE_PLATFORM, packet));
        savedRailBase.setSpanishPlatform(buttonIsSpanishPlatform.selected(), doorOpeningSide, (packet) -> KSDPacketClient.sendUpdate(KSD_PACKET_UPDATE_PLATFORM, packet));
        DataUtilities.execute(ClientData.PLATFORMS, p -> p.id == savedRailBase.id, platform -> {
            platform.name = savedRailBase.name;
            platform.setDwellTime((int) ((second + (float) (minutes * 60)) * 2.0F), (packet) -> PacketTrainDataGuiClient.sendUpdate(PACKET_UPDATE_PLATFORM, packet));
        });
        if (minecraft != null) {
            minecraft.setScreen(null);
            UtilitiesClient.setScreen(minecraft, dashboardScreen);
        }
    }

    private void setIsSpanishPlatform(boolean isSpanishPlatform) {
        buttonIsSpanishPlatform.setChecked(isSpanishPlatform);
        buttonDoorOpeningSide.visible =  isSpanishPlatform;
    }

    private void setDoorOpeningSideText(KSDPlatform.DoorOpeningSide newDoorOpeningSide) {
        doorOpeningSide = newDoorOpeningSide;
        buttonDoorOpeningSide.setMessage(Text.translatable(String.format("gui.ksd.door_opening_side_%s", doorOpeningSide).toLowerCase(Locale.ENGLISH)));
    }

    protected String getNumberStringKey() {
        return "gui.mtr.platform_number";
    }

    protected ResourceLocation getPacketIdentifier() {
        return KSD_PACKET_UPDATE_PLATFORM;
    }
}
