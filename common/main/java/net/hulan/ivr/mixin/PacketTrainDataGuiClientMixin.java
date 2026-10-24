package net.hulan.ivr.mixin;

import mtr.data.NameColorDataBase;
import mtr.data.TransportMode;
import mtr.packet.PacketTrainDataGuiClient;
import net.hulan.ivr.IVR;
import net.hulan.ksd.client.KSDClientData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static net.hulan.ksd.client.KSDClientData.DATA_CACHE;

@Mixin(value = PacketTrainDataGuiClient.class)
public class PacketTrainDataGuiClientMixin {

    @Inject(method = "sendUpdate", remap = false, at = @At("TAIL"))
    private static void sendUpdate(ResourceLocation packetId, FriendlyByteBuf packet, CallbackInfo ci) {
        DATA_CACHE.sync();
        DATA_CACHE.refreshDynamicResources();
    }

    @Inject(method = "receiveChunk", remap = false, at = @At("TAIL"))
    private static void receiveChunk(Minecraft minecraftClient, FriendlyByteBuf packet, CallbackInfo ci) {
        try {
            DATA_CACHE.sync();
            DATA_CACHE.refreshDynamicResources();
        } catch (Exception var8) {
            IVR.LOGGER.error("Could not receive chunk from server");
            IVR.LOGGER.error(var8.toString());
            for (StackTraceElement ste : var8.getStackTrace()) {
                IVR.LOGGER.error(ste.toString());
            }
        }
    }

    @Inject(method = "receiveUpdateOrDeleteS2C", remap = false, at = @At("TAIL"))
    private static <T extends NameColorDataBase> void receiveUpdateOrDeleteS2C(Minecraft minecraftClient, FriendlyByteBuf packet, Set<T> dataSet, Map<Long, T> cacheMap, BiFunction<Long, TransportMode, T> createDataWithId, boolean isDelete, CallbackInfo ci) {
        KSDClientData.DATA_CACHE.sync();
        KSDClientData.DATA_CACHE.refreshDynamicResources();
    }
}
