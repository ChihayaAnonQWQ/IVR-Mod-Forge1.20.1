package net.hulan.ksd.mixin;

import mtr.data.RailwayData;
import mtr.data.VehicleRidingServer;
import net.hulan.ksd.data.*;
import net.hulan.ksd.utils.DataUtilities;
import net.hulan.ksd.utils.RailDataUtilities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

@Mixin(VehicleRidingServer.class)
public class VehicleRidingServerMixin {

    @Inject(method = "mountRider", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private static void mountRider(Level world,
                                   Set<UUID> ridingEntities,
                                   long id,
                                   long routeId,
                                   double carX,
                                   double carY,
                                   double carZ,
                                   double length,
                                   double width,
                                   float carYaw,
                                   float carPitch,
                                   boolean doorOpen,
                                   boolean canMount,
                                   int percentageOffset,
                                   ResourceLocation packetId,
                                   Function<Player, Boolean> canRide,
                                   Consumer<Player> ridingCallback,
                                   CallbackInfo ci) {
        RailwayData railwayData = RailwayData.getInstance(world);
        double halfLength = length / (double)2.0F;
        double halfWidth = width / (double)2.0F;
        double margin = halfLength + (double)3.0F;
        if (railwayData != null) {
            world.getEntitiesOfClass(Player.class, new AABB(carX + margin, carY + margin, carZ + margin, carX - margin, carY - margin, carZ - margin), (player) -> !player.isSpectator() && !ridingEntities.contains(player.getUUID()) && railwayData.railwayDataCoolDownModule.canRide(player) && canRide.apply(player)).forEach((player) -> {
                Vec3 positionRotated = player.position().subtract(carX, carY, carZ).yRot(-carYaw).xRot(-carPitch);
                if (Math.abs(positionRotated.x) < halfWidth + (double)0.5F && Math.abs(positionRotated.y) < (double)2.5F && Math.abs(positionRotated.z) <= halfLength && !railwayData.railwayDataCoolDownModule.shouldDismount(player)) {
                    KSDRailwayData ksd = KSDRailwayData.getInstance(world);
                    if (ksd != null) {
                        KSDRoute route = DataUtilities.getRoute(ksd.routes, routeId);
                        if (route != null && RailDataUtilities.hasFirstClassService(route) && route.firstClassCar == percentageOffset) {
                            FirstClassValidationSystem.setIllegal(world, player);
                        }
                    }
                }
            });
        }
    }
}
