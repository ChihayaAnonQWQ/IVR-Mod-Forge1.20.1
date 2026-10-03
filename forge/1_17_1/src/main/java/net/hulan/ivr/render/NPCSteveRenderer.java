package net.hulan.ivr.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mtr.mappings.EntityRendererMapper;
import net.hulan.ivr.entity.NPC;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class NPCSteveRenderer<T extends NPC> extends EntityRendererMapper<T> {

    private static final float MODEL_SCALE = 1.3333333F;
    private static final float HEAD_Y_OFFSET = 21.5F / 16.0F / MODEL_SCALE;
    private static final float NAME_TAG_VERTICAL_OFFSET = 0F;
    private static final ResourceLocation STEVE_TEXTURE = DefaultPlayerSkin.getDefaultSkin(UUID.nameUUIDFromBytes("Steve".getBytes()));

    private final PlayerModel<T> model;

    public NPCSteveRenderer(Object context) {
        super(context);
        this.model = new PlayerModel<>(((EntityRendererProvider.Context) context).bakeLayer(ModelLayers.PLAYER), false);
        this.model.hat.visible = true;
    }

    public void render(@NotNull T entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int light) {
        model.setupAnim(entity, 0, 0, partialTick, yaw, entity.getXRot());
        poseStack.pushPose();
        poseStack.scale(-MODEL_SCALE, -MODEL_SCALE, MODEL_SCALE);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(
                RenderType.entityTranslucent(getTextureLocation(entity))
        );
        model.head.visible = false;
        model.hat.visible = false;
        model.renderToBuffer(
                poseStack,
                vertexConsumer,
                light,
                OverlayTexture.NO_OVERLAY,
                1,
                1,
                1,
                1);
        model.head.visible = true;
        model.hat.visible = true;
        poseStack.pushPose();
        poseStack.translate(0.0F, HEAD_Y_OFFSET, 0.0F);
        poseStack.translate(model.head.x / 16.0F, model.head.y / 16.0F, model.head.z / 16.0F);
        poseStack.scale(1.0F / MODEL_SCALE, 1.0F / MODEL_SCALE, 1.0F / MODEL_SCALE);
        poseStack.translate(-model.head.x / 16.0F, -model.head.y / 16.0F, -model.head.z / 16.0F);
        model.head.render(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
        model.hat.render(poseStack, vertexConsumer, light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        poseStack.popPose();
        renderAdjustedNameTag(entity, poseStack, bufferSource, light);
    }

    private void renderAdjustedNameTag(T entity, PoseStack poseStack, MultiBufferSource bufferSource, int light) {
        poseStack.pushPose();
        poseStack.translate(0.0F, NAME_TAG_VERTICAL_OFFSET, 0.0F);
        renderNameTag(entity, entity.getName(), poseStack, bufferSource, light);
        poseStack.popPose();
    }

    public @NotNull ResourceLocation getTextureLocation(T entity) {
        ResourceLocation texture = entity.getTexture();
        return texture == null ? STEVE_TEXTURE : texture;
    }
}
