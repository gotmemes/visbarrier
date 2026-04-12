package io.github.gotmemes.visbarrier.mixin.mixins;

import io.github.gotmemes.visbarrier.compat.v1_9.BlockPosCapture;
import io.github.gotmemes.visbarrier.mixin.SupportedVersions;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockModelRenderer.class)
@SupportedVersions("[1.9,1.12)")
public class BlockModelRendererMixin_v1_9 {

    @Inject(
            method = "renderModel(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/VertexBuffer;Z)Z",
            at = @At("HEAD")
    )
    private void captureBlockPos(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn,
                                  BlockPos posIn, VertexBuffer bufferIn, boolean checkSides,
                                  CallbackInfoReturnable<Boolean> cir) {
        BlockPosCapture.clear();
        BlockPosCapture.set(posIn, worldIn);
    }

    @Inject(
            method = "renderModel(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/VertexBuffer;Z)Z",
            at = @At("RETURN")
    )
    private void clearBlockPos(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn,
                                BlockPos posIn, VertexBuffer bufferIn, boolean checkSides,
                                CallbackInfoReturnable<Boolean> cir) {
        BlockPosCapture.clear();
    }
}
