package net.hulan.ivr.platform;

import net.hulan.ivr.entity.IVRNPCs;
import net.hulan.ivr.render.NPCSteveRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Minecraft 1.20.1 / Forge 47 platform bridge. */
@SuppressWarnings("unused")
public class NPCPlatform_1_20_1 extends NPCPlatform {

    @Override
    protected boolean isCreativePlayer(Player player) {
        return player.getAbilities().instabuild;
    }

    @Override
    protected float getPlayerYaw(Player player) {
        return player.getYRot();
    }

    @Override
    public void registerEntities(EntityRegistrar registrar) {
        registrar.register("tickets", IVRNPCs.TICKETS);
        registrar.register("fare_adjustments", IVRNPCs.FARE_ADJUSTMENTS);
    }

    @SubscribeEvent
    public static void registerForgeRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(IVRNPCs.TICKETS.get(), NPCSteveRenderer::new);
        event.registerEntityRenderer(IVRNPCs.FARE_ADJUSTMENTS.get(), NPCSteveRenderer::new);
    }
}
