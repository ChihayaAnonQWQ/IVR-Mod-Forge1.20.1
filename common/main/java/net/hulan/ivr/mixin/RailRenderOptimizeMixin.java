package net.hulan.ivr.mixin;

import mtr.data.Rail;
import net.hulan.ivr.utils.SegmentVisibility;
import net.hulan.ivr.utils.RenderTimingDebug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At;

/** Skips MTR rail quads only when their generated bounds are outside view. */
@Mixin(Rail.class)
public abstract class RailRenderOptimizeMixin {

    /**
     * Rail.render subdivides a curve into many quads.  Keep the rail callback
     * alive for visible quads, but avoid submitting quads whose four corners
     * are all outside the 300-block render volume or camera frustum.
     */
    @Redirect(method = "renderSegment",
            at = @At(value = "INVOKE",
                    target = "Lmtr/data/Rail$RenderRail;renderRail(DDDDDDDDDD)V"),
            remap = false)
    private void ivr$renderRailQuadIfVisible(Rail.RenderRail renderRail,
                                               double x1, double z1,
                                               double x2, double z2,
                                               double x3, double z3,
                                               double x4, double z4,
                                               double y1, double y2) {
        long started = RenderTimingDebug.begin(RenderTimingDebug.RAIL);
        boolean visible;
        try {
            visible = SegmentVisibility.shouldRenderRailQuad(x1, z1, x2, z2, x3, z3, x4, z4, y1, y2);
        } finally {
            RenderTimingDebug.end(RenderTimingDebug.RAIL, started);
        }
        if (visible) {
            renderRail.renderRail(x1, z1, x2, z2, x3, z3, x4, z4, y1, y2);
        }
    }
}
