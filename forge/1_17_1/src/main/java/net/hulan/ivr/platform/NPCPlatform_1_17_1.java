package net.hulan.ivr.platform;

import net.hulan.ivr.entity.NPCs;
import net.minecraft.world.entity.player.Player;
import net.hulan.ivr.render.NPCSteveRenderer;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@SuppressWarnings("unused")
public class NPCPlatform_1_17_1 extends NPCPlatform {

    protected boolean isCreativePlayer(Player player) {
        return player.getAbilities().instabuild;
    }

    protected float getPlayerYaw(Player player) {
        return player.getYRot();
    }

    public void registerEntities(EntityRegistrar registrar) {
        registrar.register("tickets", NPCs.TICKETS);
        registrar.register("fare_adjustments", NPCs.FARE_ADJUSTMENTS);
    }

    @SubscribeEvent
    public static void registerForgeRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(NPCs.TICKETS.get(), NPCSteveRenderer::new);
        event.registerEntityRenderer(NPCs.FARE_ADJUSTMENTS.get(), NPCSteveRenderer::new);
    }

}
