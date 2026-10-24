package net.hulan.ksd.mixin;

import mtr.block.BlockPSDAPGBase;
import mtr.block.BlockPlatform;
import mtr.data.Depot;
import mtr.data.RailwayData;
import mtr.data.Train;
import mtr.path.PathData;
import net.hulan.ksd.client.KSDClientData;
import net.hulan.ksd.utils.DataUtilities;
import net.hulan.ksd.data.KSDPlatform;
import net.hulan.ksd.data.KSDRailwayData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Train.class)
public abstract class TrainMixin {

    @Shadow(remap = false)
    protected float elapsedDwellTicks;

    @Shadow(remap = false)
    public abstract int getTotalDwellTicks();

    @Shadow(remap = false)
    @Final
    public static int DOOR_MOVE_TIME;

    @Shadow(remap = false)
    @Final
    protected static int DOOR_DELAY;

    @Shadow(remap = false)
    @Final
    public List<PathData> path;

    @Shadow(remap = false)
    protected int nextPlatformIndex;

    @Shadow(remap = false)
    protected boolean doorTarget;

    @Redirect(method = "calculateCar", remap = false, at = @At(value = "INVOKE", target = "Lmtr/data/Train;scanDoors(Lnet/minecraft/world/level/Level;DDDFFDI)Z", ordinal = 0))
    private boolean scanLeftDoor(Train instance,
                                 Level world,
                                 double trainX,
                                 double trainY,
                                 double trainZ,
                                 float checkYaw,
                                 float pitch,
                                 double halfSpacing,
                                 int dwellTicks) {
        return iVR$spanishCheck((TrainInvoker) instance, world, trainX, trainY, trainZ, checkYaw, pitch, halfSpacing, dwellTicks, KSDPlatform.DoorOpeningSide.LEFT);
    }

    @Redirect(method = "calculateCar", remap = false, at = @At(value = "INVOKE", target = "Lmtr/data/Train;scanDoors(Lnet/minecraft/world/level/Level;DDDFFDI)Z", ordinal = 1))
    private boolean scanRightDoor(Train instance,
                                  Level world,
                                  double trainX,
                                  double trainY,
                                  double trainZ,
                                  float checkYaw,
                                  float pitch,
                                  double halfSpacing,
                                  int dwellTicks) {
        return iVR$spanishCheck((TrainInvoker) instance, world, trainX, trainY, trainZ, checkYaw, pitch, halfSpacing, dwellTicks, KSDPlatform.DoorOpeningSide.RIGHT);
    }

    @Inject(method = "simulateTrain",
            at = @At(value = "FIELD",
                     target = "Lmtr/data/Train;doorTarget:Z",
                     shift = At.Shift.AFTER,
                     ordinal = 1,
                     opcode = Opcodes.GETFIELD),
            remap = false)
    private void tempDoorOpen(Level world, float ticksElapsed, Depot depot, CallbackInfo ci) {
        doorTarget = openDoors(world, doorTarget);
    }

    @Unique
    private boolean iVR$spanishCheck(TrainInvoker invoker,
                                     Level world,
                                     double trainX,
                                     double trainY,
                                     double trainZ,
                                     float checkYaw,
                                     float pitch,
                                     double halfSpacing,
                                     int dwellTicks,
                                     KSDPlatform.DoorOpeningSide leftOrRight) {
        boolean scanDoor;
        float phase = (float) getTotalDwellTicks() / 2;
        long platformId = path.get(nextPlatformIndex).savedRailBaseId;
        KSDPlatform platform = getPlatform(world, platformId);
        if (platform != null && platform.isSpanishPlatform && !platform.doorOpeningSide.equals(KSDPlatform.DoorOpeningSide.DEFAULT)) {
            boolean spanishCheck, original = iVR$scanDoors(invoker, world, trainX, trainY, trainZ, checkYaw, pitch, halfSpacing);
            spanishCheck = platform.doorOpeningSide.equals(leftOrRight);
            if (phase + DOOR_DELAY <= elapsedDwellTicks && elapsedDwellTicks < phase * 2) {
                spanishCheck = !spanishCheck;
            }
            scanDoor = original && spanishCheck;
            if (scanDoor) {
                iVR$openPSDOrAPG(invoker, world, trainX, trainY, trainZ, checkYaw, pitch, halfSpacing, dwellTicks);
            }
            return scanDoor;
        } else {
            return invoker.invokeScanDoors(world, trainX, trainY, trainZ, checkYaw, pitch, halfSpacing, dwellTicks);
        }
    }

    @Unique
    private boolean iVR$scanDoors(TrainInvoker invoker,
                                  Level world,
                                  double trainX,
                                  double trainY,
                                  double trainZ,
                                  float checkYaw,
                                  float pitch,
                                  double halfSpacing) {
        if (invoker.invokeSkipScanBlocks(world, trainX, trainY, trainZ)) {
            return false;
        } else {
            boolean hasPlatform = false;
            Vec3 offsetVec = (new Vec3(1.0F, 0.0F, 0.0F)).yRot(checkYaw).xRot(pitch);
            Vec3 traverseVec = (new Vec3(0.0F, 0.0F, 1.0F)).yRot(checkYaw).xRot(pitch);
            for(int checkX = 1; checkX <= 3; ++checkX) {
                for(int checkY = -2; checkY <= 3; ++checkY) {
                    for(double checkZ = -halfSpacing; checkZ <= halfSpacing; ++checkZ) {
                        BlockPos checkPos = RailwayData.newBlockPos(trainX + offsetVec.x * (double)checkX + traverseVec.x * checkZ, trainY + (double)checkY, trainZ + offsetVec.z * (double)checkX + traverseVec.z * checkZ);
                        Block block = world.getBlockState(checkPos).getBlock();
                        if (block instanceof BlockPlatform || block instanceof BlockPSDAPGBase) {
                            hasPlatform = true;
                        }
                    }
                }
            }
            return hasPlatform;
        }
    }

    @Unique
    private void iVR$openPSDOrAPG(TrainInvoker invoker,
                                  Level world,
                                  double trainX,
                                  double trainY,
                                  double trainZ,
                                  float checkYaw,
                                  float pitch,
                                  double halfSpacing,
                                  int dwellTicks) {
        Vec3 offsetVec = (new Vec3(1.0F, 0.0F, 0.0F)).yRot(checkYaw).xRot(pitch);
        Vec3 traverseVec = (new Vec3(0.0F, 0.0F, 1.0F)).yRot(checkYaw).xRot(pitch);
        for(int checkX = 1; checkX <= 3; ++checkX) {
            for(int checkY = -2; checkY <= 3; ++checkY) {
                for(double checkZ = -halfSpacing; checkZ <= halfSpacing; ++checkZ) {
                    BlockPos checkPos = RailwayData.newBlockPos(trainX + offsetVec.x * (double)checkX + traverseVec.x * checkZ, trainY + (double)checkY, trainZ + offsetVec.z * (double)checkX + traverseVec.z * checkZ);
                    Block block = world.getBlockState(checkPos).getBlock();
                    if (block instanceof BlockPlatform || block instanceof BlockPSDAPGBase) {
                        invoker.invokeOpenDoors(world, block, checkPos, dwellTicks);
                    }
                }
            }
        }
    }

    @Unique
    private boolean openDoors(Level world, boolean original) {
        long platformId = path.get(nextPlatformIndex).savedRailBaseId;
        KSDPlatform platform = getPlatform(world, platformId);
        if (platform == null || !platform.isSpanishPlatform || platform.doorOpeningSide.equals(KSDPlatform.DoorOpeningSide.DEFAULT)) {
            return original;
        }
        float phase = (float) getTotalDwellTicks() / 2;
        if (0 <= elapsedDwellTicks && elapsedDwellTicks < DOOR_DELAY) {
            return false;
        } else if (DOOR_DELAY <= elapsedDwellTicks && elapsedDwellTicks < phase - DOOR_MOVE_TIME) {
            return true;
        } else if (phase - DOOR_MOVE_TIME <= elapsedDwellTicks && elapsedDwellTicks < phase + DOOR_DELAY) {
            return false;
        } else if (phase + DOOR_DELAY <= elapsedDwellTicks && elapsedDwellTicks < phase * 2 - DOOR_MOVE_TIME) {
            return true;
        } else if (phase * 2 - DOOR_MOVE_TIME <= elapsedDwellTicks && elapsedDwellTicks < phase * 2) {
            return false;
        } else {
            return original;
        }
    }

    @Unique
    private KSDPlatform getPlatform(Level world, long platformId) {
        if (world.isClientSide) {
            return DataUtilities.getPlatform(KSDClientData.PLATFORMS, platformId);
        } else {
            KSDRailwayData railwayData = KSDRailwayData.getInstance(world);
            if (railwayData == null) {
                return null;
            }
            return DataUtilities.getPlatform(railwayData.platforms, platformId);
        }
    }
}
