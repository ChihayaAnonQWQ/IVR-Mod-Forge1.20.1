package top.mcmtr.data;

/**
 * Compile-time-only shape of MSD's rigid catenary renderer.
 *
 * <p>MSD (MTR Station Decoration) is an optional runtime dependency of IVR.
 * This stub exists solely so {@code MSDRigidCatenaryRenderMixin} can compile
 * against the real class shape; it is never packaged into the mod jar and the
 * optional mixin plugin skips the mixin when MSD is absent.</p>
 */
public abstract class RigidCatenary {

    /** Mirror of {@code top.mcmtr.data.RigidCatenary.RenderRigidCatenary}. */
    public interface RenderRigidCatenary {
        void renderRigidCatenary(
                double x1, double z1, double x2, double z2,
                double x3, double z3, double x4, double z4,
                double xs1, double zs1, double xs2, double zs2,
                double xs3, double zs3, double xs4, double zs4,
                double y1, double y2);
    }
}
