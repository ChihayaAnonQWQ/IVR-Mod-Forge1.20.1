package net.hulan.ksd.mixin;

import mtr.data.Train;
import mtr.data.DataCache;
import mtr.data.Depot;
import mtr.data.TrainServer;
import mtr.path.PathData;
import net.hulan.ksd.data.FirstClassValidationSystem;
import net.hulan.ksd.data.KSDRailwayData;
import net.hulan.ksd.data.KSDRoute;
import net.hulan.ksd.utils.DataUtilities;
import net.hulan.ksd.utils.RailDataUtilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks the last car seen for each passenger. MTR has no callback for moving
 * between cars, so simulateCar is used with the same local transform as MTR.
 */
@Mixin(TrainServer.class)
public abstract class TrainServerFirstClassMixin extends Train {

    @Unique
    private static final double CAR_EXIT_MARGIN = 1.5D;
    @Unique
    private static final double CAR_INTERIOR_LIMIT = 0.8D;
    @Unique
    private static final double CAR_CHANGE_MOVEMENT = 1.0D;
    @Unique
    private static final int CAR_CHANGE_CONFIRMATIONS = 3;

    @Unique
    private final Map<UUID, Integer> ivr$lastPassengerCars = new HashMap<>();
    @Unique
    private final Map<UUID, Integer> ivr$observedCars = new HashMap<>();
    @Unique
    private final Map<UUID, Double> ivr$observedDistances = new HashMap<>();
    @Unique
    private final Map<UUID, Boolean> ivr$leftPreviousCars = new HashMap<>();
    @Unique
    private final Map<UUID, Double> ivr$previousCarPositions = new HashMap<>();
    @Unique
    private final Map<UUID, Double> ivr$currentCarPositions = new HashMap<>();
    @Unique
    private final Map<UUID, Integer> ivr$pendingCars = new HashMap<>();
    @Unique
    private final Map<UUID, Integer> ivr$pendingTicks = new HashMap<>();
    @Unique
    private final Map<UUID, Double> ivr$pendingStartPositions = new HashMap<>();

    @Shadow(remap = false)
    private long routeId;

    /** Required by Java because this mixin inherits Train. Never called directly. */
    protected TrainServerFirstClassMixin(long sidingId, long id, float railProgress, String trainId, String baseTrainType,
                                         int width, java.util.List<PathData> path, java.util.List<Double> distances,
                                         int trainCars, int spacing, float accelerationConstant, boolean isManualAllowed,
                                         int maxManualSpeed, int manualToAutomaticTime) {
        super(sidingId, id, railProgress, trainId, baseTrainType, width, path, distances, trainCars, spacing,
                accelerationConstant, isManualAllowed, maxManualSpeed, manualToAutomaticTime);
    }

    @Inject(method = "simulateTrain", at = @At("HEAD"), remap = false)
    private void ivr$beginCarObservation(Level world, float ticksElapsed, Depot depot, DataCache dataCache,
                                         List<?> trainPositions, Map<?, ?> trainsInPlayerRange, Map<?, ?> schedulesForPlatform,
                                         Map<?, ?> trainDelays, CallbackInfoReturnable<Boolean> cir) {
        ivr$observedCars.clear();
        ivr$observedDistances.clear();
        ivr$leftPreviousCars.clear();
        ivr$currentCarPositions.clear();
    }

    @Inject(method = "simulateTrain", at = @At("RETURN"), remap = false)
    private void ivr$finishCarObservation(Level world, float ticksElapsed, Depot depot, DataCache dataCache,
                                          List<?> positions, Map<?, ?> playerTrains, Map<?, ?> schedules,
                                          Map<?, ?> delays, CallbackInfoReturnable<Boolean> cir) {
        ivr$lastPassengerCars.keySet().retainAll(ridingEntities);
        ivr$pendingCars.keySet().retainAll(ridingEntities);
        ivr$pendingTicks.keySet().retainAll(ridingEntities);
        ivr$pendingStartPositions.keySet().retainAll(ridingEntities);
        ivr$pendingCars.keySet().retainAll(ivr$observedCars.keySet());
        ivr$pendingTicks.keySet().retainAll(ivr$observedCars.keySet());
        ivr$pendingStartPositions.keySet().retainAll(ivr$observedCars.keySet());
        if (ivr$observedCars.isEmpty()) {
            ivr$pendingCars.clear();
            ivr$pendingTicks.clear();
            ivr$pendingStartPositions.clear();
            ivr$previousCarPositions.clear();
            ivr$previousCarPositions.putAll(ivr$currentCarPositions);
            return;
        }
        for (Map.Entry<UUID, Integer> observation : ivr$observedCars.entrySet()) {
            UUID playerId = observation.getKey();
            int observedCar = observation.getValue();
            Integer previousCar = ivr$lastPassengerCars.get(playerId);
            if (previousCar == null) {
                ivr$lastPassengerCars.put(playerId, observedCar);
                continue;
            }
            if (previousCar == observedCar) {
                ivr$pendingCars.remove(playerId);
                ivr$pendingTicks.remove(playerId);
                ivr$pendingStartPositions.remove(playerId);
                continue;
            }
            // A bend can make adjacent car boxes disagree for several ticks even
            // when the passenger has not crossed the connection between them.
            if (!Boolean.TRUE.equals(ivr$leftPreviousCars.get(playerId))
                    || ivr$observedDistances.get(playerId) > CAR_INTERIOR_LIMIT) {
                ivr$pendingCars.remove(playerId);
                ivr$pendingTicks.remove(playerId);
                ivr$pendingStartPositions.remove(playerId);
                continue;
            }
            Double previousPosition = ivr$previousCarPositions.get(playerId);
            Double currentPosition = ivr$currentCarPositions.get(playerId);
            if (previousPosition == null || currentPosition == null) {
                ivr$pendingCars.remove(playerId);
                ivr$pendingTicks.remove(playerId);
                ivr$pendingStartPositions.remove(playerId);
                continue;
            }
            boolean sameCandidate = observedCar == ivr$pendingCars.getOrDefault(playerId, -1);
            double startPosition = sameCandidate
                    ? ivr$pendingStartPositions.getOrDefault(playerId, previousPosition) : previousPosition;
            int confirmations = sameCandidate ? ivr$pendingTicks.getOrDefault(playerId, 0) + 1 : 1;
            if (confirmations < CAR_CHANGE_CONFIRMATIONS
                    || Math.abs(currentPosition - startPosition) < CAR_CHANGE_MOVEMENT) {
                ivr$pendingCars.put(playerId, observedCar);
                ivr$pendingTicks.put(playerId, confirmations);
                ivr$pendingStartPositions.put(playerId, startPosition);
                continue;
            }
            ivr$pendingCars.remove(playerId);
            ivr$pendingTicks.remove(playerId);
            ivr$pendingStartPositions.remove(playerId);
            ivr$lastPassengerCars.put(playerId, observedCar);
            if (world != null) {
                KSDRailwayData railwayData = KSDRailwayData.getInstance(world);
                KSDRoute route = railwayData == null ? null : DataUtilities.getRoute(railwayData.routes, routeId);
                if (route != null && RailDataUtilities.hasFirstClassService(route)
                        && observedCar == route.firstClassCar) {
                    Player player = world.getPlayerByUUID(playerId);
                    if (player != null && !player.isSpectator()) {
                        FirstClassValidationSystem.validateEnteringFirstClass(world, railwayData, player);
                    }
                }
            }
        }
        ivr$previousCarPositions.clear();
        ivr$previousCarPositions.putAll(ivr$currentCarPositions);
    }

    @Inject(method = "simulateCar", at = @At("HEAD"), remap = false)
    private void ivr$validateFirstClassEntry(Level world, int ridingCar, float ticksElapsed,
                                             double carX, double carY, double carZ, float carYaw, float carPitch,
                                             double prevCarX, double prevCarY, double prevCarZ, float prevCarYaw, float prevCarPitch,
                                             boolean doorLeftOpen, boolean doorRightOpen, double realSpacing, CallbackInfo ci) {
        if (ridingEntities.isEmpty()) {
            ivr$lastPassengerCars.clear();
            ivr$pendingCars.clear();
            ivr$pendingTicks.clear();
            ivr$pendingStartPositions.clear();
            return;
        }
        double halfLength = realSpacing / 2.0D;
        double halfWidth = width / 2.0D;
        for (UUID playerId : ridingEntities) {
            Player player = world.getPlayerByUUID(playerId);
            if (player == null || player.isSpectator()) {
                continue;
            }
            Vec3 localPosition = player.position().subtract(carX, carY, carZ).yRot(-carYaw).xRot(-carPitch);
            Integer previousCar = ivr$lastPassengerCars.get(playerId);
            if (previousCar != null && previousCar == ridingCar) {
                ivr$leftPreviousCars.put(playerId, Math.abs(localPosition.z) > halfLength + CAR_EXIT_MARGIN);
                ivr$currentCarPositions.put(playerId, localPosition.z);
            }
            boolean inside = Math.abs(localPosition.x) < halfWidth + 0.5D
                    && Math.abs(localPosition.y) < 2.5D
                    && Math.abs(localPosition.z) <= halfLength;
            if (inside) {
                Integer currentCandidate = ivr$observedCars.get(playerId);
                double distance = Math.abs(localPosition.z) / Math.max(1.0D, halfLength);
                boolean candidateIsPrevious = previousCar != null && currentCandidate != null
                        && currentCandidate.intValue() == previousCar.intValue();
                if (currentCandidate == null || (previousCar != null && ridingCar == previousCar)
                        || (!candidateIsPrevious && distance < ivr$observedDistances.get(playerId))) {
                    ivr$observedCars.put(playerId, ridingCar);
                    ivr$observedDistances.put(playerId, distance);
                }
            }
        }
    }
}
