package net.hulan.ivr.mixin;

import net.hulan.ivr.utils.BlockEntityCullDebug;
import net.hulan.ivr.utils.RenderTimingDebug;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Adds IVR's own block entity culling counts to the F3 overlay. */
@Mixin(DebugScreenOverlay.class)
public abstract class BlockEntityCullDebugOverlayMixin {

    @Inject(method = "getGameInformation", at = @At("RETURN"), cancellable = true)
    private void ivr$addBlockEntityCullCounts(CallbackInfoReturnable<List<String>> ci) {
        List<String> lines = new ArrayList<>(ci.getReturnValue());
        lines.add(BlockEntityCullDebug.getDebugLine());
        lines.addAll(Arrays.asList(RenderTimingDebug.getDebugLines()));
        ci.setReturnValue(lines);
    }
}
