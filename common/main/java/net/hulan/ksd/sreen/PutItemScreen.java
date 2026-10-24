package net.hulan.ksd.sreen;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.data.IGui;
import mtr.mappings.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class PutItemScreen extends ScreenMapper implements IGui {

    private final String itemKey;
    private final Item item;
    private final int amount;
    private final PutMethod putMethod;
    private final boolean readOnly;
    private final PutCallback callback;
    private final List<PutItemSlot> slots = new ArrayList<>(36);
    private Inventory inventory;
    private int left;
    private int top;
    private int selectedInventoryIndex = -1;
    private ItemStack carriedStack = ItemStack.EMPTY;
    private ItemStack placedStack = ItemStack.EMPTY;
    private static final int COLUMNS = 9;
    private static final int PANEL_WIDTH = 176;
    private static final int PANEL_HEIGHT = 166;
    private static final int INVENTORY_TOP = 44;
    private static final int HOTBAR_TOP = 102;
    private static final int TARGET_SLOT_SIZE = 20;

    public PutItemScreen(String itemKey, Item item, PutMethod putMethod, boolean readOnly, PutCallback callback) {
        this(itemKey, item, 1, putMethod, readOnly, callback);
    }

    public PutItemScreen(String itemKey, Item item, int amount, PutMethod putMethod, boolean readOnly, PutCallback callback) {
        super(Text.literal(""));
        this.itemKey = itemKey;
        this.item = item;
        this.amount = Math.max(1, amount);
        this.putMethod = putMethod;
        this.readOnly = readOnly;
        this.callback = callback;
    }

    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        slots.clear();
        if (minecraft != null && minecraft.player != null) {
            inventory = Utilities.getInventory(minecraft.player);
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < COLUMNS; column++) {
                    addSlot(column + row * COLUMNS + COLUMNS, column, row, false);
                }
            }
            for (int column = 0; column < COLUMNS; column++) {
                addSlot(column, column, 0, true);
            }
        }
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        guiGraphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFFC6C6C6);
        guiGraphics.fill(left + 4, top + 4, left + PANEL_WIDTH - 4, top + PANEL_HEIGHT - 4, 0xFF4A4A4A);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, getTitleText(), left + PANEL_WIDTH / 2, top + 7, ARGB_WHITE);

        int targetX = left + (PANEL_WIDTH - TARGET_SLOT_SIZE) / 2;
        int targetY = top + 18;
        guiGraphics.fill(targetX, targetY, targetX + TARGET_SLOT_SIZE, targetY + TARGET_SLOT_SIZE, 0xFF8B8B8B);
        guiGraphics.fill(targetX + 1, targetY + 1, targetX + TARGET_SLOT_SIZE - 1, targetY + TARGET_SLOT_SIZE - 1, 0xFF373737);
        if (isTargetMouseOver(mouseX, mouseY)) {
            guiGraphics.fill(targetX + 1, targetY + 1, targetX + TARGET_SLOT_SIZE - 1, targetY + TARGET_SLOT_SIZE - 1, 0x60FFFFFF);
        }
        Item previewItem = resolveItemKey();
        ItemStack targetPreview = placedStack.isEmpty() && previewItem != null ? new ItemStack(previewItem) : placedStack;
        if (!targetPreview.isEmpty()) {
            // Flush the batched slot fills before the raw item render so the item stays on top.
            guiGraphics.flush();
            net.hulan.ksd.utils.Utilities.getInstance().renderGuiItem(guiGraphics.pose(), Minecraft.getInstance().getItemRenderer(),
                    Minecraft.getInstance().font, targetPreview, targetX + 2, targetY + 2);
        }

        for (PutItemSlot slot : slots) {
            slot.render(guiGraphics, mouseX, mouseY, delta);
        }
        super.render(guiGraphics, mouseX, mouseY, delta);

        PutItemSlot hovered = getSlotAt(mouseX, mouseY);
        if (hovered != null && !hovered.getItemStack().isEmpty()) {
            guiGraphics.renderTooltip(font, hovered.getItemStack(), mouseX, mouseY);
        }
        if (!carriedStack.isEmpty()) {
            guiGraphics.flush();
            net.hulan.ksd.utils.Utilities.getInstance().renderGuiItem(guiGraphics.pose(), Minecraft.getInstance().getItemRenderer(),
                    Minecraft.getInstance().font, carriedStack, mouseX - 8, mouseY - 8);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return true;
        }
        if (isTargetMouseOver(mouseX, mouseY)) {
            if (!carriedStack.isEmpty()) {
                placeCarriedStack();
            }
            return true;
        }
        PutItemSlot slot = getSlotAt(mouseX, mouseY);
        if (slot != null) {
            handleInventorySlotClick(slot);
            return true;
        }
        // Consume clicks outside slots while carrying a stack so it can never be dropped from this screen.
        return !carriedStack.isEmpty() || super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean isPauseScreen() {
        return false;
    }

    private void addSlot(int inventoryIndex, int column, int row, boolean hotbar) {
        PutItemSlot slot = new PutItemSlot(inventoryIndex, inventory.getItem(inventoryIndex));
        slot.setPosition(left + 7 + column * PutItemSlot.SIZE,
                top + (hotbar ? HOTBAR_TOP : INVENTORY_TOP + row * PutItemSlot.SIZE));
        slots.add(slot);
    }

    private boolean isTargetMouseOver(double mouseX, double mouseY) {
        int targetX = left + (PANEL_WIDTH - TARGET_SLOT_SIZE) / 2;
        int targetY = top + 18;
        return mouseX >= targetX
                && mouseY >= targetY
                && mouseX < targetX + TARGET_SLOT_SIZE
                && mouseY < targetY + TARGET_SLOT_SIZE;
    }

    private PutItemSlot getSlotAt(double mouseX, double mouseY) {
        for (PutItemSlot slot : slots) {
            if (slot.isMouseOver(mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    /** Implements normal left-click pickup, placement, merging and stack swapping. */
    private void handleInventorySlotClick(PutItemSlot slot) {
        ItemStack slotStack = slot.getItemStack();
        if (carriedStack.isEmpty()) {
            if (!slotStack.isEmpty()) {
                selectedInventoryIndex = slot.getInventoryIndex();
                carriedStack = slot.takeStack();
            }
        } else if (slotStack.isEmpty()) {
            slot.setItemStack(carriedStack);
            carriedStack = ItemStack.EMPTY;
            selectedInventoryIndex = -1;
        } else if (ItemStack.isSameItemSameTags(carriedStack, slotStack)) {
            int transferred = Math.min(carriedStack.getCount(), slotStack.getMaxStackSize() - slotStack.getCount());
            slotStack.grow(transferred);
            carriedStack.shrink(transferred);
            if (carriedStack.isEmpty()) {
                carriedStack = ItemStack.EMPTY;
                selectedInventoryIndex = -1;
            }
        } else {
            slot.setItemStack(carriedStack);
            carriedStack = slotStack;
            selectedInventoryIndex = slot.getInventoryIndex();
        }
    }

    /** Applies the target-item, amount and read-only rules from the original screen API. */
    private void placeCarriedStack() {
        if (carriedStack.getItem() != item) {
            return;
        }
        if (readOnly) {
            placedStack = carriedStack.copy();
            returnCarriedStack();
            onClose();
            runCallback();
        } else {
            int transferred = carriedStack.getCount();
            if (placedStack.isEmpty()) {
                placedStack = carriedStack.copy();
                placedStack.setCount(transferred);
            } else {
                placedStack.grow(transferred);
            }
            carriedStack.shrink(transferred);
            if (carriedStack.isEmpty()) {
                carriedStack = ItemStack.EMPTY;
                selectedInventoryIndex = -1;
            }
            if (placedStack.getCount() >= amount) {
                onClose();
                runCallback();
            }
        }
    }

    private void returnCarriedStack() {
        returnToTemporarySlots(carriedStack, selectedInventoryIndex);
        carriedStack = ItemStack.EMPTY;
        selectedInventoryIndex = -1;
    }

    public void onClose() {
        if (minecraft != null) {
            UtilitiesClient.setScreen(minecraft, null);
        }
    }

    private void runCallback() {
        if (callback != null) {
            callback.put(placedStack, placedStack.getCount());
        }
    }

    /** Returns a cursor stack to its original temporary slot, or merges it into another temporary slot. */
    private void returnToTemporarySlots(ItemStack stack, int preferredIndex) {
        if (stack.isEmpty()) {
            return;
        }
        PutItemSlot preferredSlot = getSlotByInventoryIndex(preferredIndex);
        if (preferredSlot != null && preferredSlot.getItemStack().isEmpty()) {
            preferredSlot.setItemStack(stack);
            return;
        }
        for (PutItemSlot slot : slots) {
            ItemStack slotStack = slot.getItemStack();
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameTags(stack, slotStack)) {
                int transferred = Math.min(stack.getCount(), slotStack.getMaxStackSize() - slotStack.getCount());
                slotStack.grow(transferred);
                stack.shrink(transferred);
                if (stack.isEmpty()) {
                    return;
                }
            }
        }
        for (PutItemSlot slot : slots) {
            if (slot.getItemStack().isEmpty()) {
                slot.setItemStack(stack);
                return;
            }
        }
    }

    private PutItemSlot getSlotByInventoryIndex(int inventoryIndex) {
        for (PutItemSlot slot : slots) {
            if (slot.getInventoryIndex() == inventoryIndex) {
                return slot;
            }
        }
        return null;
    }

    private MutableComponent getTitleText() {
        return readOnly ?
                Text.translatable("gui.ksd.read_item", getPutMethodText(), item.getDescription().getString()) :
                Text.translatable("gui.ksd.put_item", getPutMethodText(), amount, item.getDescription().getString());
    }

    private MutableComponent getPutMethodText() {
        return Text.translatable("gui.ksd.put_item_" + putMethod.name().toLowerCase());
    }

    /** Resolves itemKey as either an item registry id or a translation/description key. */
    private Item resolveItemKey() {
        if (itemKey == null || itemKey.isEmpty()) {
            return item;
        }
        ResourceLocation id = new ResourceLocation(itemKey);
        Item registryItem = RegistryUtilities.registryGetItem().get(id);
        if (registryItem != net.minecraft.world.item.Items.AIR) {
            return registryItem;
        }
        return item;
    }

    public enum PutMethod {

        PUT,
        PAT,
        INSERT
    }

    @FunctionalInterface
    public interface PutCallback {
        void put(ItemStack stack, int amount);
    }
}
