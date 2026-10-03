package net.hulan.ivr.mixin;

import net.hulan.ivr.utils.RailVisibility;
import net.hulan.ivr.utils.ANTERailChunkBounds;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies to both ANTE batching modes on Fabric and Forge. */
@Pseudo
@Mixin(targets = {
        "fabric.cn.zbx1425.mtrsteamloco.render.rail.MeshBuildingRailChunk",
        "fabric.cn.zbx1425.mtrsteamloco.render.rail.InstancedRailChunk",
        "forge.cn.zbx1425.mtrsteamloco.render.rail.MeshBuildingRailChunk",
        "forge.cn.zbx1425.mtrsteamloco.render.rail.InstancedRailChunk"
}, remap = false)
public abstract class ANTERailChunkEnqueueMixin {

    @Inject(method = "enqueue", at = @At("HEAD"), cancellable = true, remap = false)
    private void ivr$enqueueRailChunkIfVisible(CallbackInfo ci) {
        if (!RailVisibility.shouldRenderBox(
                ((ANTERailChunkBounds) this).ivr$getRailChunkBounds())) {
            ci.cancel();
        }
    }
}
