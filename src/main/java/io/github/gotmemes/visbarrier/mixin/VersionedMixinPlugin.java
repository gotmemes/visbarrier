package io.github.gotmemes.visbarrier.mixin;

import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;
import net.minecraftforge.fml.common.versioning.InvalidVersionSpecificationException;
import net.minecraftforge.fml.common.versioning.VersionRange;
import org.objectweb.asm.*;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class VersionedMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String resourcePath = mixinClassName.replace('.', '/') + ".class";
        final String[] version = { null };

        String annotationDescriptor = Type.getDescriptor(SupportedVersions.class);

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Enumeration<URL> resources = classLoader.getResources(resourcePath);
            if (!resources.hasMoreElements()) {
                return true;
            }

            try (InputStream inputStream = resources.nextElement().openStream()) {
                ClassReader reader = new ClassReader(inputStream);
                reader.accept(new ClassVisitor(Opcodes.ASM5) {
                    @Override
                    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                        if (desc.equals(annotationDescriptor)) {
                            return new AnnotationVisitor(Opcodes.ASM5) {
                                @Override
                                public void visit(String name, Object value) {
                                    if ("value".equals(name)) {
                                        version[0] = value.toString();
                                    }
                                }
                            };
                        }
                        return null;
                    }
                }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            }

        } catch (IOException e) {
            throw new RuntimeException("Error reading class file for mixin: " + mixinClassName, e);
        }

        if (version[0] == null) {
            return true;
        }


        try {
            VersionRange versionRange = VersionRange.createFromVersionSpec(version[0]);
            DefaultArtifactVersion currentVersion = new DefaultArtifactVersion(Loader.instance().getMinecraftModContainer().getVersion());
            return versionRange.containsVersion(currentVersion);
        } catch (InvalidVersionSpecificationException e) {
            FMLLog.warning("Invalid version specification for mixin: %s", mixinClassName);
            return true;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
