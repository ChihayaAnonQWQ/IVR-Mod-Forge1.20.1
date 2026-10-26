package net.hulan.ksd.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;

/** Minecraft 1.20.1 / Forge 47 implementation of the version-specific helpers. */
@SuppressWarnings("unused")
public class Utilities_1_20_1 extends Utilities {

    public void registerCommand(LiteralArgumentBuilder<CommandSourceStack> command) {
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent event) ->
                event.getDispatcher().register(command));
    }

    public void renderGuiItem(PoseStack poseStack, ItemRenderer itemRenderer, Font font, ItemStack itemStack, int x, int y) {
        // 1.20.1 moved both calls from ItemRenderer to GuiGraphics.
        final Minecraft minecraft = Minecraft.getInstance();
        final GuiGraphics guiGraphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
        guiGraphics.pose().mulPoseMatrix(poseStack.last().pose());
        guiGraphics.renderItem(itemStack, x, y);
        guiGraphics.renderItemDecorations(font, itemStack, x, y);
        guiGraphics.flush();
    }

    public void registerItemModelPredicator(Item item, ResourceLocation id, ModelPredictor modelPredictor) {
        ItemProperties.register(
                item,
                id,
                (itemStack, clientLevel, livingEntity, seed) -> modelPredictor.predictor(itemStack, clientLevel, livingEntity)
        );
    }

    public BlockBehaviour.Properties createBlockProperties() {
        // 1.20.1 removed Material; keep the metal look/sound of Material.METAL.
        return BlockBehaviour.Properties.of().sound(SoundType.METAL);
    }
}
