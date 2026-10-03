package net.hulan.ivr.platform;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.hulan.ivr.entity.NPC;
import net.hulan.ivr.entity.NPCs;
import net.minecraft.world.entity.player.Player;

@SuppressWarnings("unused")
public class NPCPlatform_1_19_2 extends NPCPlatform {
    @Override protected boolean isCreativePlayer(Player player) { return player.getAbilities().instabuild; }
    @Override protected float getPlayerYaw(Player player) { return player.getYRot(); }
    @Override
    public void registerEntities(EntityRegistrar registrar) {
        registrar.register("tickets", NPCs.TICKETS);
        registrar.register("fare_adjustments", NPCs.FARE_ADJUSTMENTS);
    }

    @Override
    public void registerAttributes() {
        FabricDefaultAttributeRegistry.register(NPCs.TICKETS.get(), NPC.createAttributes());
        FabricDefaultAttributeRegistry.register(NPCs.FARE_ADJUSTMENTS.get(), NPC.createAttributes());
    }
}
