package net.hulan.ksd.sreen;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.client.IDrawing;
import mtr.mappings.ScreenMapper;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import net.hulan.ksd.KSDItems;
import net.hulan.ksd.data.KCRTicketSystem;
import net.hulan.ksd.data.PaymentMethod;
import net.hulan.ksd.packet.KSDPacketClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Items;

import java.util.UUID;

public abstract class PaymentScreen extends ScreenMapper implements KSDGui {

    int total;
    boolean failed;
    final int mtrBalance;
    final ScreenMapper parent;
    final BlockPos storeBlockPos;
    MutableComponent failedMessage;
    protected PaymentMethod paymentMethod;
    private final Button buttonPaymentMethod;
    private final Button buttonConfirm;
    private final Button buttonCancel;

    protected PaymentScreen(int mtrBalance, ScreenMapper parent, BlockPos storeBlockPos) {
        super(Text.literal(""));
        this.mtrBalance = mtrBalance;
        this.parent = parent;
        this.storeBlockPos = storeBlockPos;
        buttonPaymentMethod = UtilitiesClient.newButton(Text.translatable("gui.mtr.add_value"), button -> setPaymentMethod(paymentMethod.next()));
        buttonConfirm = UtilitiesClient.newButton(Text.translatable("gui.ksd.confirm"), button -> process(true));
        buttonCancel = UtilitiesClient.newButton(Text.translatable("gui.ksd.cancel"), button -> process(false));
    }

    protected void init() {
        IDrawing.setPositionAndWidth(buttonPaymentMethod,width / 2 - 100, height / 2 + 20, 202);
        IDrawing.setPositionAndWidth(buttonConfirm, width / 2 - PAYING_PADDING - 100, height / 2 + 102, 100);
        IDrawing.setPositionAndWidth(buttonCancel, width / 2 + PAYING_PADDING, height / 2 + 102, 100);
        addDrawableChild(buttonPaymentMethod);
        addDrawableChild(buttonConfirm);
        addDrawableChild(buttonCancel);
        setPaymentMethod(PaymentMethod.EMERALDS);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        KSDGui.renderBg(guiGraphics, width, height, 440, 290);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, getTotalText(), width / 2, height / 2 + 42, ARGB_WHITE);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, getNeedToPayText(), width / 2, height / 2 + 62, ARGB_WHITE);
        if (failed) {
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, getFailedText(), width / 2, height / 2 + 82, RGB_RED | ARGB_BLACK);
        }
        super.render(guiGraphics, mouseX, mouseY, delta);
    }

    public boolean isPauseScreen() {
        return false;
    }

    public void onClose() {
        if (minecraft != null) {
            UtilitiesClient.setScreen(minecraft, parent);
        }
    }

    void countTotal() {
        failed = false;
    }

    void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
        buttonPaymentMethod.setMessage(Text.translatable(String.format("gui.ksd.payment_method_%s", paymentMethod).toLowerCase()));
        failed = false;
    }

    private MutableComponent getTotalText() {
        return Text.translatable("gui.ksd.total", String.valueOf(total));
    }

    private MutableComponent getNeedToPayText() {
        return Text.translatable("gui.ksd.need_to_pay", switch (paymentMethod) {
            case EMERALDS -> Text.translatable("gui.ksd.emeralds", (int) Math.ceil((double) total / 16)).getString();
            case MTR_BALANCE, OCTOPUS -> Text.translatable("gui.ksd.hkd", total).getString();
        });
    }

    private MutableComponent getFailedText() {
        return Text.translatable("gui.ksd.failed", failedMessage);
    }

    final void process(boolean confirm) {
        if (confirm && total > 0) {
            switch (paymentMethod) {
                case EMERALDS -> {
                    if (minecraft != null) {
                        final int needToPay = KCRTicketSystem.getEmeraldCount(total);
                        UtilitiesClient.setScreen(minecraft, new PutItemScreen(
                                "item.minecraft.emerald",
                                Items.EMERALD,
                                needToPay,
                                PutItemScreen.PutMethod.INSERT,
                                false,
                                (itemStack, actualPayment) -> {
                                    KSDPacketClient.sendPaymentC2S(
                                            PaymentMethod.EMERALDS,
                                            needToPay,
                                            actualPayment,
                                            storeBlockPos);
                                    extraAction();
                                    if (minecraft != null) {
                                        UtilitiesClient.setScreen(minecraft, null);
                                    }
                                }));
                    }
                }
                case MTR_BALANCE -> {
                    if (mtrBalance < 0) {
                        failed = true;
                        failedMessage = Text.translatable("gui.ksd.insufficient_mtr_balance");
                    } else {
                        KSDPacketClient.sendPaymentC2S(
                                PaymentMethod.MTR_BALANCE,
                                total,
                                total,
                                storeBlockPos);
                        extraAction();
                        if (minecraft != null) {
                            UtilitiesClient.setScreen(minecraft, null);
                        }
                    }
                }
                case OCTOPUS -> {
                    if (minecraft != null) {
                        UtilitiesClient.setScreen(minecraft, new PutItemScreen(
                                "item.ksd.octopus",
                                KSDItems.OCTOPUS.get(),
                                PutItemScreen.PutMethod.PAT,
                                true,
                                (octopusItem, count) -> {
                                    CompoundTag octopusTag = octopusItem.getOrCreateTag();
                                    UUID uuid = octopusTag.getUUID("uuid");
                                    int balance = octopusTag.getInt("balance");
                                    if (balance < 0) {
                                        failed = true;
                                        failedMessage = Text.translatable("insufficient_octopus");
                                    } else {
                                        payWithOctopus(uuid);
                                        if (minecraft != null) {
                                            UtilitiesClient.setScreen(minecraft, null);
                                        }
                                    }
                                }));
                    }
                }
            }
        } else {
            if (minecraft != null) {
                UtilitiesClient.setScreen(minecraft, parent);
            }
        }
    }

    abstract void extraAction();

    abstract void payWithOctopus(UUID uuid);

    public static boolean isEnglish() {
        return Minecraft.getInstance().options.languageCode.startsWith("en");
    }
}
