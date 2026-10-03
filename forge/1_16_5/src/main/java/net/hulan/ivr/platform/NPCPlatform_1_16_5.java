package net.hulan.ivr.platform;

import net.hulan.ivr.entity.NPCs;
import net.minecraft.world.entity.player.Player;

@SuppressWarnings("unused")
public class NPCPlatform_1_16_5 extends NPCPlatform {

    protected boolean isCreativePlayer(Player player) {
        return player.abilities.instabuild;
    }

    protected float getPlayerYaw(Player player) {
        return player.yRot;
    }

    public void registerEntities(EntityRegistrar registrar) {
        registrar.register("tickets", NPCs.TICKETS);
        registrar.register("fare_adjustments", NPCs.FARE_ADJUSTMENTS);
    }

}
