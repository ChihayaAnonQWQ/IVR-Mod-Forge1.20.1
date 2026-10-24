package net.hulan.ksd.sreen;

import mtr.mappings.ButtonMapper;
import mtr.mappings.UtilitiesClient;
import net.hulan.ksd.utils.RenderUtilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** A button that can display a full-size texture while retaining its normal click behavior. */
public class TextureButton extends ButtonMapper {

    private static final int MISSING_TEXTURE_BACKGROUND = 0xFF4A4A4A;
    private static final int HOVER_BORDER_COLOR = 0xFFFFFFFF;
    private static final int HOVER_BORDER_WIDTH = 2;
    private static final int TEXT_PADDING = 4;
    private static final int LINE_HEIGHT = 10;
    private static final int TEXT_HEIGHT = 8;
    private ResourceLocation texture;

    public TextureButton(Component message, Button.OnPress onPress) {
        this(null, message, onPress);
    }

    public TextureButton(ResourceLocation texture, Component message, Button.OnPress onPress) {
        super(0, 0, 0, 0, message, onPress);
        this.texture = texture;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public void setTexture(ResourceLocation texture) {
        this.texture = texture;
    }

    public void setLayoutHeight(int height) {
        this.height = height;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        final int widgetX = UtilitiesClient.getWidgetX(this);
        final int widgetY = UtilitiesClient.getWidgetY(this);
        final int widgetWidth = getWidth();
        final int widgetHeight = getHeight();
        // Flush any batched screen background before drawing the texture through raw vertex buffers.
        guiGraphics.flush();
        if (hasTexture()) {
            RenderUtilities.getInstance().drawTexture(guiGraphics.pose(), texture, widgetX, widgetY, widgetWidth, widgetHeight);
        } else {
            drawMissingTexture(guiGraphics);
        }
        drawMessage(guiGraphics);
        if (isMouseOver(mouseX, mouseY)) {
            drawHoverBorder(guiGraphics);
        }
    }

    private boolean hasTexture() {
        try {
            return UtilitiesClient.hasResource(texture);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void drawMissingTexture(GuiGraphics guiGraphics) {
        final int widgetX = UtilitiesClient.getWidgetX(this);
        final int widgetY = UtilitiesClient.getWidgetY(this);
        final int widgetWidth = getWidth();
        final int widgetHeight = getHeight();
        guiGraphics.fill(widgetX, widgetY, widgetX + widgetWidth, widgetY + widgetHeight, MISSING_TEXTURE_BACKGROUND);
    }

    private void drawMessage(GuiGraphics guiGraphics) {
        final String message = getMessage().getString();
        if (message.isEmpty() || getWidth() <= TEXT_PADDING * 2 || getHeight() <= TEXT_PADDING * 2) {
            return;
        }
        final Font font = Minecraft.getInstance().font;
        final int availableWidth = getWidth() - TEXT_PADDING * 2;
        final List<String> lines = wrapMessage(message, font, availableWidth);
        if (lines.isEmpty()) {
            return;
        }
        final int textHeight = TEXT_HEIGHT + (lines.size() - 1) * LINE_HEIGHT;
        int widestLine = 0;
        for (String line : lines) {
            widestLine = Math.max(widestLine, font.width(line));
        }
        final float scale = Math.min(1.0F, Math.min(
                widestLine == 0 ? 1.0F : (float) availableWidth / widestLine,
                (float) (getHeight() - TEXT_PADDING * 2) / textHeight));
        final int widgetX = UtilitiesClient.getWidgetX(this);
        final int widgetY = UtilitiesClient.getWidgetY(this);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(widgetX + getWidth() / 2.0F, widgetY + getHeight() / 2.0F, 0);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            // 1.20.1 moved Font#draw to GuiGraphics; the pose set above carries the button centering transform.
            guiGraphics.drawString(font, line, -font.width(line) / 2.0F,
                    -textHeight / 2.0F + i * LINE_HEIGHT, 0xFFFFFFFF, true);
        }
        guiGraphics.pose().popPose();
    }

    private static List<String> wrapMessage(String message, Font font, int maxWidth) {
        final List<String> lines = new ArrayList<>();
        final StringBuilder line = new StringBuilder();
        final StringBuilder word = new StringBuilder();
        boolean pendingSpace = false;
        for (int offset = 0; offset < message.length();) {
            final int codePoint = message.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (codePoint == '\n') {
                appendWord(lines, line, word, pendingSpace, font, maxWidth);
                lines.add(line.toString());
                line.setLength(0);
                pendingSpace = false;
            } else if (Character.isWhitespace(codePoint)) {
                appendWord(lines, line, word, pendingSpace, font, maxWidth);
                pendingSpace = true;
            } else if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) {
                appendWord(lines, line, word, pendingSpace, font, maxWidth);
                pendingSpace = false;
                appendToken(lines, line, new String(Character.toChars(codePoint)), false, font, maxWidth);
            } else {
                word.appendCodePoint(codePoint);
            }
        }
        appendWord(lines, line, word, pendingSpace, font, maxWidth);
        lines.add(line.toString());
        return lines;
    }

    private static void appendWord(List<String> lines, StringBuilder line, StringBuilder word,
                                   boolean pendingSpace, Font font, int maxWidth) {
        if (!word.isEmpty()) {
            appendToken(lines, line, word.toString(), pendingSpace, font, maxWidth);
            word.setLength(0);
        }
    }

    private static void appendToken(List<String> lines, StringBuilder line, String token,
                                    boolean spaceBefore, Font font, int maxWidth) {
        final String candidate = line.isEmpty() ? token : line + (spaceBefore ? " " : "") + token;
        if (!line.isEmpty() && font.width(candidate) > maxWidth) {
            lines.add(line.toString());
            line.setLength(0);
        }
        if (!line.isEmpty() && spaceBefore) {
            line.append(' ');
        }
        line.append(token);
    }

    private void drawHoverBorder(GuiGraphics guiGraphics) {
        final int widgetX = UtilitiesClient.getWidgetX(this);
        final int widgetY = UtilitiesClient.getWidgetY(this);
        final int right = widgetX + getWidth();
        final int bottom = widgetY + getHeight();
        guiGraphics.fill(widgetX, widgetY, right, widgetY + HOVER_BORDER_WIDTH, HOVER_BORDER_COLOR);
        guiGraphics.fill(widgetX, bottom - HOVER_BORDER_WIDTH, right, bottom, HOVER_BORDER_COLOR);
        guiGraphics.fill(widgetX, widgetY + HOVER_BORDER_WIDTH, widgetX + HOVER_BORDER_WIDTH, bottom - HOVER_BORDER_WIDTH, HOVER_BORDER_COLOR);
        guiGraphics.fill(right - HOVER_BORDER_WIDTH, widgetY + HOVER_BORDER_WIDTH, right, bottom - HOVER_BORDER_WIDTH, HOVER_BORDER_COLOR);
    }
}
