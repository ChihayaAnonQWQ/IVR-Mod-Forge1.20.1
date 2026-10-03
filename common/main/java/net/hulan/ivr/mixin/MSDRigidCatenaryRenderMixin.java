package net.hulan.ivr.mixin;

import net.hulan.ivr.utils.SegmentVisibility;
import net.hulan.ivr.utils.RenderTimingDebug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Filters the actual quads emitted by MSD, not samples of the center line. */
@Pseudo
@Mixin(targets = "top.mcmtr.data.RigidCatenary", remap = false)
public abstract class MSDRigidCatenaryRenderMixin {

    @Redirect(method = "renderSegment",
            at = @At(value = "INVOKE", target = "Ltop/mcmtr/data/RigidCatenary$RenderRigidCatenary;renderRigidCatenary(DDDDDDDDDDDDDDDDDD)V"),
            remap = false)
    private void ivr$renderVisibleSegment(top.mcmtr.data.RigidCatenary.RenderRigidCatenary renderer,
            double x1, double z1, double x2, double z2, double x3, double z3, double x4, double z4,
            double xs1, double zs1, double xs2, double zs2, double xs3, double zs3, double xs4, double zs4,
            double y1, double y2) {
        long started = RenderTimingDebug.begin(RenderTimingDebug.MSD);
        boolean visible;
        try {
            visible = SegmentVisibility.shouldRenderCatenarySegment(
                    x1, z1, x2, z2, x3, z3, x4, z4,
                    xs1, zs1, xs2, zs2, xs3, zs3, xs4, zs4, y1, y2);
        } finally {
            RenderTimingDebug.end(RenderTimingDebug.MSD, started);
        }
        if (visible) {
            long geometryStarted = RenderTimingDebug.begin(RenderTimingDebug.MSD_GEOMETRY);
            try {
                renderer.renderRigidCatenary(
                        x1, z1, x2, z2, x3, z3, x4, z4,
                        xs1, zs1, xs2, zs2, xs3, zs3, xs4, zs4, y1, y2);
                RenderTimingDebug.recordControlPoints(RenderTimingDebug.MSD_GEOMETRY, 8L);
            } finally {
                RenderTimingDebug.end(RenderTimingDebug.MSD_GEOMETRY, geometryStarted);
            }
        }
    }
}
