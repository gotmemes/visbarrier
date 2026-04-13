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

@Mixin(BlockModelRenderer.class)
@SupportedVersions("[1.12,)")
public class BlockModelRendererMixin_v1_12 {

    // Forge 1.12 split renderModel into renderModelSmooth + renderModelFlat
    // (both Forge-added, keep their MCP names at runtime). Injecting here puts
    // the capture on the same stack frame as model.getQuads(), guaranteeing the
    // ThreadLocal is populated when BarrierCTMModel.getQuads reads it.

    @Inject(
            method = "renderModelSmooth(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
            at = @At("HEAD")
    )
    private void captureBlockPosSmooth(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn,
                                        BlockPos posIn, BufferBuilder bufferIn, boolean checkSides,
                                        long rand, CallbackInfoReturnable<Boolean> cir) {
        BlockPosCapture.clear();
        BlockPosCapture.set(posIn, worldIn);
    }

    @Inject(
            method = "renderModelSmooth(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
            at = @At("RETURN")
    )
    private void clearBlockPosSmooth(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn,
                                      BlockPos posIn, BufferBuilder bufferIn, boolean checkSides,
                                      long rand, CallbackInfoReturnable<Boolean> cir) {
        BlockPosCapture.clear();
    }

    @Inject(
            method = "renderModelFlat(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
            at = @At("HEAD")
    )
    private void captureBlockPosFlat(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn,
                                      BlockPos posIn, BufferBuilder bufferIn, boolean checkSides,
                                      long rand, CallbackInfoReturnable<Boolean> cir) {
        BlockPosCapture.clear();
        BlockPosCapture.set(posIn, worldIn);
    }

    @Inject(
            method = "renderModelFlat(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
            at = @At("RETURN")
    )
    private void clearBlockPosFlat(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn,
                                    BlockPos posIn, BufferBuilder bufferIn, boolean checkSides,
                                    long rand, CallbackInfoReturnable<Boolean> cir) {
        BlockPosCapture.clear();
    }
}
