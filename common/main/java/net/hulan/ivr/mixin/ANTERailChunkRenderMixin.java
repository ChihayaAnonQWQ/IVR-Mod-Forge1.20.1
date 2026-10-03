package net.hulan.ivr.mixin;

import net.hulan.ivr.utils.ANTERailChunkBounds;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

/** Exposes the ANTE base class bounds to both concrete chunk renderers. */
@Pseudo
@Mixin(targets = {
        "fabric.cn.zbx1425.mtrsteamloco.render.rail.RailChunkBase",
        "forge.cn.zbx1425.mtrsteamloco.render.rail.RailChunkBase"
}, remap = false)
public abstract class ANTERailChunkRenderMixin implements ANTERailChunkBounds {

    @Shadow(remap = false)
    public AABB boundingBox;

    @Override
    public AABB ivr$getRailChunkBounds() {
        return boundingBox;
    }
}
