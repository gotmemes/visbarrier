package io.github.gotmemes.visbarrier.mixin.mixins;

import io.github.gotmemes.visbarrier.Visbarrier;
import io.github.gotmemes.visbarrier.mixin.SupportedVersions;
import net.minecraft.client.particle.Barrier;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Barrier.class)
@SupportedVersions("[1.12,1.12.2]")
public class BarrierMixin_v1_12 extends Particle {
    protected BarrierMixin_v1_12() {
        super(null, 0, 0, 0);
    }

    @Inject(method = "Lnet/minecraft/client/particle/Barrier;func_180434_a(Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/entity/Entity;FFFFFF)V", at = @At("HEAD"), cancellable = true, remap = false)
    public void removeParticle(CallbackInfo ci) {
        if (Visbarrier.isVisible) {
            this.particleMaxAge = Integer.MIN_VALUE;
            ci.cancel();
        }
    }
}
