package net.hulan.ksd.sreen;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.client.IDrawing;
import mtr.data.IGui;
import mtr.mappings.ScreenMapper;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.MutableComponent;

public class ConfirmationScreen extends ScreenMapper implements IGui {

    private final Runnable deleteCallback;
    private final MutableComponent text;
    private final ScreenMapper previousScreen;
    private final Button buttonYes;
    private final Button buttonNo;

    public ConfirmationScreen(Runnable deleteCallback, MutableComponent text, ScreenMapper previousScreen) {
        super(Text.literal(""));
        this.deleteCallback = deleteCallback;
        this.text = text;
        this.previousScreen = previousScreen;
        this.buttonYes = UtilitiesClient.newButton(Text.translatable("gui.yes"), (button) -> onYes());
        this.buttonNo = UtilitiesClient.newButton(Text.translatable("gui.no"), (button) -> onNo());
    }

    protected void init() {
        super.init();
        IDrawing.setPositionAndWidth(buttonYes, width / 2 - 100 - 10, height / 2, 100);
        IDrawing.setPositionAndWidth(buttonNo, width / 2 + 10, height / 2, 100);
        addDrawableChild(buttonYes);
        addDrawableChild(buttonNo);
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        try {
            renderBackground(guiGraphics);
            super.render(guiGraphics, mouseX, mouseY, delta);
            guiGraphics.drawCenteredString(font, text, width / 2, height / 2 - 40 + 6, ARGB_WHITE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClose() {
        super.onClose();
        if (minecraft != null) {
            UtilitiesClient.setScreen(minecraft, previousScreen);
        }
    }

    private void onYes() {
        deleteCallback.run();
        onClose();
    }

    private void onNo() {
        onClose();
    }
}
