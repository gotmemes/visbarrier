package io.github.gotmemes.visbarrier.mixin.mixins;

import io.github.gotmemes.visbarrier.compat.v1_12.BlockPosCapture;
import io.github.gotmemes.visbarrier.mixin.SupportedVersions;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Captures the BlockPos/IBlockAccess being rendered so {@code BarrierCTMModel.getQuads} can look up
 * neighbours for connected textures.
 *
 * <p><b>Why the outer {@code renderModel} and not {@code renderModelSmooth}/{@code renderModelFlat}:</b>
 * Forge 1.12's {@code net.minecraftforge.client.model.pipeline.ForgeBlockModelRenderer} (installed by
 * default whenever the Forge lighting pipeline is enabled) <i>overrides</i> {@code renderModelSmooth} and
 * {@code renderModelFlat}. When the block-model renderer instance is that subclass, the vanilla
 * (mixin-patched) smooth/flat bodies never execute — so a HEAD inject on them never fires, the
 * ThreadLocal stays empty, and {@code BarrierCTMModel.getQuads} silently takes its {@code pos == null}
 * fallback (the plain barrier with no CTM). Forge does <i>not</i> override the outer {@code renderModel}
 * entry point, so capturing there — a parent frame that still encloses {@code model.getQuads()} — works
 * under both the vanilla and the Forge pipeline. This mirrors the 1.8/1.9 mixins, which capture on their
 * single {@code renderModel} method.
 */
@Mixin(BlockModelRenderer.class)
@SupportedVersions("[1.12,)")
public class BlockModelRendererMixin_v1_12 {

    @Inject(
            method = "renderModel(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
            at = @At("HEAD")
    )
    private void captureBlockPos(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn,
                                  BlockPos posIn, BufferBuilder bufferIn, boolean checkSides,
                                  long rand, CallbackInfoReturnable<Boolean> cir) {
        // clear() before set() defends against stale state if a prior renderModel call
        // exited via exception (RETURN inject doesn't fire on exception paths in Mixin 0.7.x).
        BlockPosCapture.clear();
        BlockPosCapture.set(posIn, worldIn);
    }

    @Inject(
            method = "renderModel(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
            at = @At("RETURN")
    )
    private void clearBlockPos(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn,
                                BlockPos posIn, BufferBuilder bufferIn, boolean checkSides,
                                long rand, CallbackInfoReturnable<Boolean> cir) {
        BlockPosCapture.clear();
    }
}
