package net.hulan.ksd.sreen;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.client.IDrawing;
import mtr.mappings.ScreenMapper;
import mtr.mappings.Text;
import mtr.screen.WidgetBetterTextField;
import net.hulan.ksd.data.Octopus;
import net.hulan.ksd.data.PaymentMethod;
import net.hulan.ksd.packet.KSDPacketClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class AddValueMachineScreen extends PaymentScreen {

    private int addValue;
    private UUID uuid;
    private int balance;
    private int totalBalance;
    private final ItemStack octopusItem;
    private final WidgetBetterTextField textFieldAddValue;

    public AddValueMachineScreen(int mtrBalance, ScreenMapper parent, ItemStack octopusItem, BlockPos storeBlockPos) {
        super(mtrBalance, parent, storeBlockPos);
        this.octopusItem = octopusItem;
        textFieldAddValue = new WidgetBetterTextField("Value");
    }

    protected void init() {
        super.init();
        IDrawing.setPositionAndWidth(textFieldAddValue,width / 2 - 20, height / 2 - 20, 40);
        textFieldAddValue.setResponder(this::setAddValue);
        addDrawableChild(textFieldAddValue);
        loadOctopusData();
    }

    public void tick() {
        textFieldAddValue.tick();
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, getBalanceText(), width / 2, height / 2 - 60, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, getAddValueText(), width / 2, height / 2 - 40, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, getTotalBalanceText(), width / 2, height / 2 + 2, 0xFFFFFFFF);
    }

    void countTotal() {
        total = addValue;
        totalBalance = balance + total;
        super.countTotal();
    }

    void setPaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod.equals(PaymentMethod.OCTOPUS)) {
            paymentMethod = paymentMethod.next();
        }
        super.setPaymentMethod(paymentMethod);
    }

    void extraAction() {
        KSDPacketClient.sendOctopusAddValueC2S(
                uuid,
                addValue,
                Octopus.History.Source.ADD_VALUE);
    }

    void payWithOctopus(UUID uuid) {
    }

    private void loadOctopusData() {
        CompoundTag octopusTag = octopusItem.getOrCreateTag();
        uuid = octopusTag.getUUID("uuid");
        balance = octopusTag.getInt("balance");
    }

    private void setAddValue(String text) {
        int value;
        try {
            value = Integer.parseInt(textFieldAddValue.getValue());
        } catch (RuntimeException e) {
            value = 0;
        }
        addValue = Mth.clamp(value, 0, 1000);
        countTotal();
    }

    private MutableComponent getBalanceText() {
        return Text.translatable("gui.mtr.balance", balance);
    }

    private MutableComponent getAddValueText() {
        return Text.translatable("gui.ksd.add_value");
    }

    private MutableComponent getTotalBalanceText() {
        return Text.translatable("gui.mtr.total_balance", totalBalance);
    }
}
