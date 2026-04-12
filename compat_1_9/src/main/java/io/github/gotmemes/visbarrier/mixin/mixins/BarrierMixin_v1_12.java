package io.github.gotmemes.visbarrier.mixin.mixins;

import io.github.gotmemes.visbarrier.VisbarrierState;
import io.github.gotmemes.visbarrier.mixin.SupportedVersions;
import net.minecraft.client.particle.Barrier;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Targets MC 1.12's Barrier particle, but lives in compat_1_9 because this mixin uses
 * {@code remap = false} with a raw SRG method descriptor — no direct reference to 1.12's
 * {@code BufferBuilder} class is needed, so it compiles cleanly against 1.9.4's deobf jar.
 * Keeping it here lets compat_1_9's mixin plugin + refmap cover all particle variants
 * across 1.9–1.12 from a single jar without pulling in 1.12's API.
 */
@Mixin(Barrier.class)
@SupportedVersions("[1.12,1.12.2]")
public class BarrierMixin_v1_12 extends Particle {
    protected BarrierMixin_v1_12() {
        super(null, 0, 0, 0);
    }

    // BufferBuilder replaces VertexBuffer in 1.12; remap=false so the string descriptor
    // compiles cleanly against 1.9.4 jars with no actual BufferBuilder import needed.
    @Inject(method = "Lnet/minecraft/client/particle/Barrier;func_180434_a(Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/entity/Entity;FFFFFF)V", at = @At("HEAD"), cancellable = true, remap = false)
    public void removeParticle(CallbackInfo ci) {
        if (VisbarrierState.barriersVisible) {
            this.particleMaxAge = Integer.MIN_VALUE;
            ci.cancel();
        }
    }
}
