package io.github.gotmemes.visbarrier.mixin.mixins;

import io.github.gotmemes.visbarrier.Visbarrier;
import io.github.gotmemes.visbarrier.mixin.SupportedVersions;
import net.minecraft.client.particle.Barrier;
import net.minecraft.client.particle.EntityFX;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Barrier.class)
@SupportedVersions("[1.9]")
public class BarrierMixin_v1_9 extends EntityFX {
    protected BarrierMixin_v1_9() {
        super(null, 0, 0, 0);
    }

    @Inject(method = "Lnet/minecraft/client/particle/Barrier;func_180434_a(Lnet/minecraft/client/renderer/VertexBuffer;Lnet/minecraft/entity/Entity;FFFFFF)V", at = @At("HEAD"), cancellable = true, remap = false)
    public void removeParticle(CallbackInfo ci) {
        if (Visbarrier.isVisible) {
            this.particleMaxAge = Integer.MIN_VALUE;
            ci.cancel();
        }
    }
}
