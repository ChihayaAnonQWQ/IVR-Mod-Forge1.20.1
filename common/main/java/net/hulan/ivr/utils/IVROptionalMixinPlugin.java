package net.hulan.ivr.utils;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Selects version-specific optional mixins from the classes available at runtime. */
public final class IVROptionalMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogManager.getLogger("ivr");
    private static final String FABRIC_ANTE = "fabric/cn/zbx1425/mtrsteamloco/render/rail/";
    private static final String FORGE_ANTE = "forge/cn/zbx1425/mtrsteamloco/render/rail/";

    @Override
    public void onLoad(String mixinPackage) {
        LOGGER.info("IVR optional Mixin compatibility (static check only): MTR Fabric loop={}, MTR Forge loop={}, MSD={}, ANTE={}",
                renderTrainsStatus("(Lmtr/entity/EntitySeat;FLnet/minecraft/class_4587;Lnet/minecraft/class_4597;)V"),
                renderTrainsStatus("(Lmtr/entity/EntitySeat;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"),
                hasClass("top/mcmtr/data/RigidCatenary.class")
                        ? hasSupportedRigidCatenary() ? "eligible" : "incompatible signature" : "mod absent",
                anteStatus());
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith(".RenderTrainsLoopOptimizeMixin")) {
            return hasRenderTrainsOverload("(Lmtr/entity/EntitySeat;FLnet/minecraft/class_4587;Lnet/minecraft/class_4597;)V");
        }
        if (mixinClassName.endsWith(".RenderTrainsLoopOptimizeForgeMixin")) {
            return hasRenderTrainsOverload("(Lmtr/entity/EntitySeat;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V");
        }
        if (mixinClassName.endsWith(".MSDRigidCatenaryRenderMixin")) {
            return hasSupportedRigidCatenary();
        }
        if (mixinClassName.endsWith(".ANTERailChunkRenderMixin")
                || mixinClassName.endsWith(".ANTERailChunkEnqueueMixin")) {
            return isAnteCompatible(FABRIC_ANTE) || isAnteCompatible(FORGE_ANTE);
        }
        return true;
    }

    private static boolean hasClass(String resource) {
        try (InputStream bytes = IVROptionalMixinPlugin.class.getClassLoader().getResourceAsStream(resource)) {
            return bytes != null;
        } catch (IOException exception) {
            return false;
        }
    }

    private static String renderTrainsStatus(String descriptor) {
        return !hasClass("mtr/render/RenderTrains.class") ? "mod absent"
                : hasRenderTrainsOverload(descriptor) ? "eligible" : "incompatible signature";
    }

    private static String anteStatus() {
        if (!hasClass(FABRIC_ANTE + "RailChunkBase.class")
                && !hasClass(FORGE_ANTE + "RailChunkBase.class")) {
            return "mod absent";
        }
        return isAnteCompatible(FABRIC_ANTE) || isAnteCompatible(FORGE_ANTE)
                ? "eligible" : "incompatible signature";
    }

    private static boolean isAnteCompatible(String prefix) {
        try (InputStream bytes = IVROptionalMixinPlugin.class.getClassLoader()
                .getResourceAsStream(prefix + "RailChunkBase.class")) {
            if (bytes == null || !hasClass(prefix + "MeshBuildingRailChunk.class")
                    || !hasClass(prefix + "InstancedRailChunk.class")) {
                return false;
            }
            ClassNode target = new ClassNode();
            new ClassReader(bytes).accept(target, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            for (FieldNode field : target.fields) {
                if (field.name.equals("boundingBox") && field.desc.equals("Lnet/minecraft/world/phys/AABB;")) {
                    return hasEnqueue(prefix + "MeshBuildingRailChunk.class")
                            && hasEnqueue(prefix + "InstancedRailChunk.class");
                }
            }
        } catch (IOException | RuntimeException exception) {
            return false;
        }
        return false;
    }

    private static boolean hasEnqueue(String resource) {
        try (InputStream bytes = IVROptionalMixinPlugin.class.getClassLoader().getResourceAsStream(resource)) {
            if (bytes == null) {
                return false;
            }
            ClassNode target = new ClassNode();
            new ClassReader(bytes).accept(target, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return target.methods.stream().anyMatch(method -> method.name.equals("enqueue") && method.desc.equals("()V"));
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static boolean hasRenderTrainsOverload(String descriptor) {
        try (InputStream bytes = IVROptionalMixinPlugin.class.getClassLoader()
                .getResourceAsStream("mtr/render/RenderTrains.class")) {
            if (bytes == null) {
                return false;
            }
            ClassNode target = new ClassNode();
            new ClassReader(bytes).accept(target, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return target.methods.stream().anyMatch(method -> method.name.equals("render") && method.desc.equals(descriptor));
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static boolean hasSupportedRigidCatenary() {
        try (InputStream bytes = IVROptionalMixinPlugin.class.getClassLoader()
                .getResourceAsStream("top/mcmtr/data/RigidCatenary.class")) {
            if (bytes == null) {
                return false;
            }
            ClassNode target = new ClassNode();
            new ClassReader(bytes).accept(target, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return target.methods.stream().anyMatch(method -> {
                if (!method.name.equals("renderSegment")
                        || !method.desc.equals("(Ljava/util/List;Ltop/mcmtr/data/RigidCatenary$RenderRigidCatenary;)V")) {
                    return false;
                }
                for (org.objectweb.asm.tree.AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof MethodInsnNode call
                            && call.owner.equals("top/mcmtr/data/RigidCatenary$RenderRigidCatenary")
                            && call.name.equals("renderRigidCatenary")
                            && call.desc.equals("(DDDDDDDDDDDDDDDDDD)V")) {
                        return true;
                    }
                }
                return false;
            });
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
