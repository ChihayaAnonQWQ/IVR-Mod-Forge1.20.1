package net.hulan.ksd.sreen;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.mappings.ScreenMapper;
import mtr.mappings.Text;
import mtr.screen.WidgetBetterCheckbox;
import net.hulan.ksd.data.*;
import net.hulan.ksd.packet.KSDPacketClient;
import net.hulan.ksd.utils.RailDataUtilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class OctopusFareAdjustmentScreen extends PaymentScreen implements IGui {

    private final KSDStation current;
    private final KSDStation destination;
    private final ItemStack octopusItem;
    private UUID uuid;
    private boolean expired;
    private int expiredFare;
    private final WidgetBetterCheckbox buttonIsConcessionary;

    public OctopusFareAdjustmentScreen(@NotNull KSDStation current,
                                       @NotNull KSDStation destination,
                                       int mtrBalance,
                                       ScreenMapper parent,
                                       ItemStack octopusItem,
                                       BlockPos storeBlockPos) {
        super(mtrBalance, parent, storeBlockPos);
        this.current = current;
        this.destination = destination;
        this.octopusItem = octopusItem;
        buttonIsConcessionary = new WidgetBetterCheckbox(0, 0, 0, SQUARE_SIZE, Text.translatable("gui.ksd.is_concessionary"), b -> {});
    }

    protected void init() {
        super.init();
        IDrawing.setPositionAndWidth(buttonIsConcessionary,width / 2 - 50, height / 2, 100);
        addDrawableChild(buttonIsConcessionary);
        loadOctopusData();
    }

    public void tick() {
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, getCurrentText(), width / 2, height / 2 - 60, ARGB_WHITE);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, getDestinationText(), width / 2, height / 2 - 40, ARGB_WHITE);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, getExpiredText(), width / 2, height / 2 - 20, ARGB_WHITE);
    }

    void countTotal() {
        total = expired ? expiredFare / (buttonIsConcessionary.selected() ? 2 : 1) : 0;
        super.countTotal();
    }

    void setPaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod.equals(PaymentMethod.OCTOPUS)) {
            paymentMethod = paymentMethod.next();
        }
        super.setPaymentMethod(paymentMethod);
    }

    void extraAction() {
        KSDPacketClient.sendAdjustOctopusFareC2S(
                uuid,
                storeBlockPos);
    }

    void payWithOctopus(UUID uuid) {
    }

    private void loadOctopusData() {
        CompoundTag octopusTag = octopusItem.getOrCreateTag();
        UUID octopusUuid = octopusTag.getUUID("uuid");
        long expireTime = KCRTicketSystem.getEntryTime(octopusTag) + net.hulan.ksd.utils.Utilities.EXPIRE_TIME;
        boolean isConcessionary = octopusTag.getBoolean("is_concessionary");
        uuid = octopusUuid;
        expired = KCRTicketSystem.isExpired(expireTime);
        expiredFare = KCRTicketSystem.getExpiredFare(expireTime);
        setIsConcessionary(isConcessionary);
        countTotal();
        failed = false;
    }

    private void setIsConcessionary(boolean isConcessionary) {
        buttonIsConcessionary.setChecked(isConcessionary);
        buttonIsConcessionary.active = false;
    }

    private MutableComponent getCurrentText() {
        String[] currentMain = RailDataUtilities.getSplitName(current);
        return Text.translatable("gui.ksd.current", isEnglish() && currentMain.length >= 2 ? currentMain[1] : currentMain[0]);
    }

    private MutableComponent getDestinationText() {
        String[] destinationMain = RailDataUtilities.getSplitName(destination);
        return Text.translatable("gui.ksd.destination", isEnglish() && destinationMain.length >= 2 ? destinationMain[1] : destinationMain[0]);
    }

    private MutableComponent getExpiredText() {
        return expired ? Text.translatable("gui.ksd.expired", String.valueOf(expiredFare)) : Text.translatable("gui.ksd.not_expired");
    }
}
