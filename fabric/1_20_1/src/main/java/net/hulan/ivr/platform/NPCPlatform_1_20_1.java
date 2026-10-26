package net.hulan.ivr.platform;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.hulan.ivr.entity.IVRNPC;
import net.hulan.ivr.entity.IVRNPCs;
import net.minecraft.world.entity.player.Player;

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

    @Override
    public void registerAttributes() {
        FabricDefaultAttributeRegistry.register(IVRNPCs.TICKETS.get(), IVRNPC.createAttributes());
        FabricDefaultAttributeRegistry.register(IVRNPCs.FARE_ADJUSTMENTS.get(), IVRNPC.createAttributes());
    }
}
