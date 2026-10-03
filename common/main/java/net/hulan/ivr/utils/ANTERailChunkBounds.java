package net.hulan.ivr.utils;

import net.minecraft.world.phys.AABB;

/** Internal bridge exposed by the optional ANTE rail-chunk mixin. */
public interface ANTERailChunkBounds {

    AABB ivr$getRailChunkBounds();
}
