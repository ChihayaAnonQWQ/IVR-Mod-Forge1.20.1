package net.hulan.ivr.entity;

import net.hulan.ksd.packet.KSDPacketClient;
import net.hulan.ivr.platform.NPCPlatform;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public abstract class NPC extends IVRNPC {

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    protected NPC(EntityType<? extends NPC> type, Level level) {
        super(type, level);
        setNoAi(true);
        setInvulnerable(false);
    }

    public abstract String getRole();

    protected @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && level.isClientSide) {
            KSDPacketClient.sendNPCInteraction(this);
            return InteractionResult.sidedSuccess(true);
        }
        return InteractionResult.CONSUME;
    }

    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player) {
            if (NPCPlatform.isCreative(player)) {
                kill();
                return true;
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    public void tick() {
        setDeltaMovement(0, 0, 0);
        super.tick();
        setDeltaMovement(0, 0, 0);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    public boolean isPushable() {
        return false;
    }
}
