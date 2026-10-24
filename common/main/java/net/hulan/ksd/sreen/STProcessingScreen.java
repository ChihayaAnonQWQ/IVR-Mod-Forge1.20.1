package net.hulan.ksd.sreen;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import mtr.screen.WidgetBetterCheckbox;
import net.hulan.ksd.client.KSDClientData;
import net.hulan.ksd.data.*;
import net.hulan.ksd.packet.KSDPacketClient;
import net.hulan.ksd.utils.RailDataUtilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class STProcessingScreen extends PaymentScreen implements IGui {

    private final KSDStation current;
    private final KSDStation destination;
    private int fare;
    private int amount;
    private final KCRSTMachineScreen stmMachineScreen;
    private final WidgetBetterCheckbox buttonIsConcessionary;
    private final WidgetBetterCheckbox buttonFCAvailable;
    private final List<Button> amountButtons = new ArrayList<>(10);

    protected STProcessingScreen(@NotNull KSDStation current, @NotNull KSDStation destination, int mtrBalance, KCRSTMachineScreen stmMachineScreen) {
        super(mtrBalance, stmMachineScreen, stmMachineScreen.machinePos);
        this.current = current;
        this.destination = destination;
        this.stmMachineScreen = stmMachineScreen;
        buttonIsConcessionary = new WidgetBetterCheckbox(0, 0, 0, SQUARE_SIZE, Text.translatable("gui.ksd.is_concessionary"), this::setIsConcessionary);
        buttonFCAvailable = new WidgetBetterCheckbox(0, 0, 0, SQUARE_SIZE, Text.translatable("gui.ksd.fc_available"), this::setFCAvailable);
        for (int i = 0; i < 10; i++) {
            final int amount = i + 1;
            amountButtons.add(UtilitiesClient.newButton(Text.literal(String.valueOf(amount)), button -> setAmount(amount)));
        }
    }

    protected void init() {
        super.init();
        IDrawing.setPositionAndWidth(buttonIsConcessionary,width / 2 - 100, height / 2 - 60, 100);
        IDrawing.setPositionAndWidth(buttonFCAvailable,width / 2, height / 2 - 60, 100);
        for (int i = 0; i < 10; i++) {
            Button button = amountButtons.get(i);
            IDrawing.setPositionAndWidth(button, width / 2 + (i - 5) * 20, height / 2, 20);
            addDrawableChild(button);
        }
        addDrawableChild(buttonIsConcessionary);
        addDrawableChild(buttonFCAvailable);
        setAmount(1);
        setTicketType(stmMachineScreen.ticketType);
        setIsConcessionary(false);
        setFCAvailable(false);
    }

    public void tick() {
        for (int i = 0; i < 10; i++) {
            amountButtons.get(i).active = amount != i + 1;
        }
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, getCurrentText(), width / 2, height / 2 - 120, ARGB_WHITE);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, getDestinationText(), width / 2, height / 2 - 100, ARGB_WHITE);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, getFareText(), width / 2, height / 2 - 80, ARGB_WHITE);
        if (buttonFCAvailable.selected()) {
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, getWarningMessage(), width / 2, height / 2 - 36, RGB_RED | ARGB_BLACK);
        }
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, getAmountText(), width / 2, height / 2 - 16, ARGB_WHITE);
    }

    void countTotal() {
        WayFinder wayFinder = KSDClientData.DATA_CACHE.wayFinder;
        fare = KCRTicketSystem.getFare(wayFinder, current, destination, buttonIsConcessionary.selected(), buttonFCAvailable.selected());
        total = fare * amount;
        super.countTotal();
    }

    void extraAction() {
        KSDPacketClient.sendPurchaseSTC2S(
                fare,
                amount,
                stmMachineScreen.ticketType,
                buttonIsConcessionary.selected(),
                buttonFCAvailable.selected(),
                stmMachineScreen.machinePos);
    }

    void payWithOctopus(UUID uuid) {
        KSDPacketClient.sendOctopusAddValueC2S(
                uuid,
                -total,
                switch (stmMachineScreen.ticketType) {
                    case MTR -> Octopus.History.Source.MTR;
                    case KCR -> Octopus.History.Source.KCR;
                    case LRT -> Octopus.History.Source.LRT;
                });
        extraAction();
    }

    private void setAmount(int amount) {
        this.amount = Mth.clamp(amount, 1, 10);
        countTotal();
    }

    private void setTicketType(SingleTicketSystem.TicketType ticketType) {
        buttonFCAvailable.visible = ticketType.equals(SingleTicketSystem.TicketType.KCR);
    }

    private void setIsConcessionary(boolean isConcessionary) {
        buttonIsConcessionary.setChecked(isConcessionary);
        countTotal();
    }

    private void setFCAvailable(boolean firstClassAvailable) {
        buttonFCAvailable.setChecked(firstClassAvailable);
        countTotal();
    }

    private MutableComponent getCurrentText() {
        String[] currentMain = RailDataUtilities.getSplitName(current);
        return Text.translatable("gui.ksd.current", isEnglish() && currentMain.length >= 2 ? currentMain[1] : currentMain[0]);
    }

    private MutableComponent getDestinationText() {
        String[] destinationMain = RailDataUtilities.getSplitName(destination);
        return Text.translatable("gui.ksd.destination", isEnglish() && destinationMain.length >= 2 ? destinationMain[1] : destinationMain[0]);
    }

    private MutableComponent getFareText() {
        return Text.translatable("gui.ksd.fare", String.valueOf(fare));
    }

    private MutableComponent getWarningMessage() {
        return Text.translatable("gui.ksd.st_fc_warning");
    }

    private MutableComponent getAmountText() {
        return Text.translatable("gui.ksd.amount");
    }
}
