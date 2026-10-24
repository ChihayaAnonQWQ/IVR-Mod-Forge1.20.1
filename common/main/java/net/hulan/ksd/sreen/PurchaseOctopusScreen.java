package net.hulan.ksd.sreen;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.mappings.ScreenMapper;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import mtr.screen.WidgetBetterCheckbox;
import mtr.screen.WidgetBetterTextField;
import net.hulan.ksd.data.OctopusSystem;
import net.hulan.ksd.data.PaymentMethod;
import net.hulan.ksd.packet.KSDPacketClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PurchaseOctopusScreen extends PaymentScreen implements IGui {

    private int amount;
    private int addValue;
    private final WidgetBetterCheckbox buttonIsConcessionary;
    private final WidgetBetterCheckbox buttonAddValueNow;
    private final WidgetBetterTextField textFieldAddValue;
    private final List<Button> amountButtons = new ArrayList<>(10);

    public PurchaseOctopusScreen(int mtrBalance, ScreenMapper parent, BlockPos storeBlockPos) {
        super(mtrBalance, parent, storeBlockPos);
        buttonIsConcessionary = new WidgetBetterCheckbox(0, 0, 0, SQUARE_SIZE, Text.translatable("gui.ksd.is_concessionary"), this::setIsConcessionary);
        buttonAddValueNow = new WidgetBetterCheckbox(0, 0, 0, SQUARE_SIZE, Text.translatable("gui.ksd.add_value_now"), this::setAddValueNow);
        textFieldAddValue = new WidgetBetterTextField("Value");
        for (int i = 0; i < 10; i++) {
            final int amount = i + 1;
            amountButtons.add(UtilitiesClient.newButton(Text.literal(String.valueOf(amount)), button -> setAmount(amount)));
        }
    }

    protected void init() {
        super.init();
        IDrawing.setPositionAndWidth(buttonIsConcessionary,width / 2 - 50, height / 2 - 100, 100);
        IDrawing.setPositionAndWidth(buttonAddValueNow,width / 2 - 50, height / 2 - 80, 100);
        IDrawing.setPositionAndWidth(textFieldAddValue,width / 2 - 20, height / 2 - 40, 40);
        textFieldAddValue.setResponder(this::setAddValue);
        for (int i = 0; i < 10; i++) {
            Button button = amountButtons.get(i);
            IDrawing.setPositionAndWidth(button, width / 2 + (i - 5) * 20, height / 2, 20);
            addDrawableChild(button);
        }
        addDrawableChild(buttonIsConcessionary);
        addDrawableChild(buttonAddValueNow);
        addDrawableChild(textFieldAddValue);
        setAmount(1);
        setIsConcessionary(false);
        setAddValueNow(false);
    }

    public void tick() {
        for (int i = 0; i < 10; i++) {
            amountButtons.get(i).active = amount != i + 1;
        }
        textFieldAddValue.tick();
    }


    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, getBasePriceText(), width / 2, height / 2 - 120, 0xFFFFFF);
        if (buttonAddValueNow.selected()) {
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, getAddValueText(), width / 2, height / 2 - 56, ARGB_WHITE);
        }
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, getAmountText(), width / 2, height / 2 - 16, ARGB_WHITE);
    }

    void countTotal() {
        total = (OctopusSystem.BASE_PRICE + addValue) * amount;
        super.countTotal();
    }

    void setPaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod.equals(PaymentMethod.OCTOPUS)) {
            paymentMethod = paymentMethod.next();
        }
        super.setPaymentMethod(paymentMethod);
    }

    void extraAction() {
        KSDPacketClient.sendPurchaseOctopusC2S(
                addValue,
                buttonIsConcessionary.selected(),
                amount);
    }

    void payWithOctopus(UUID uuid) {
    }

    private void setAmount(int amount) {
        this.amount = Mth.clamp(amount, 1, 10);
        countTotal();
    }

    private void setIsConcessionary(boolean isConcessionary) {
        buttonIsConcessionary.setChecked(isConcessionary);
        countTotal();
    }

    private void setAddValueNow(boolean addValueNow) {
        buttonAddValueNow.setChecked(addValueNow);
        textFieldAddValue.visible = addValueNow;
        countTotal();
    }

    private void setAddValue(String text) {
        int value;
        if (buttonAddValueNow.selected()) {
            try {
                value = Integer.parseInt(textFieldAddValue.getValue());
            } catch (RuntimeException e) {
                value = 0;
            }
        } else {
            value = 0;
        }
        addValue = Mth.clamp(value, 0, 500);
        countTotal();
    }

    private MutableComponent getBasePriceText() {
        return Text.translatable("gui.ksd.base_price", OctopusSystem.BASE_PRICE);
    }

    private MutableComponent getAddValueText() {
        return Text.translatable("gui.ksd.add_value");
    }

    private MutableComponent getAmountText() {
        return Text.translatable("gui.ksd.amount");
    }
}
