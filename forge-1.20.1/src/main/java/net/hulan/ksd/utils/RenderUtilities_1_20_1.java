package net.hulan.ksd.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

/** Minecraft 1.20.1 implementation of the immediate-mode GUI draw helpers. */
@SuppressWarnings("unused")
public class RenderUtilities_1_20_1 extends RenderUtilities {

    public void beginDrawingCircle(BufferBuilder buffer) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
    }

    public void beginDrawingTexture(BufferBuilder buffer, ResourceLocation texture) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
    }

    public void finishDrawingCircle() {
    }

    public void finishDrawingTexture() {
    }
}
