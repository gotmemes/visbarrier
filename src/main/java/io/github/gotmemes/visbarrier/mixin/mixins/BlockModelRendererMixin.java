package io.github.gotmemes.visbarrier.mixin.mixins;

import io.github.gotmemes.visbarrier.ctm.BlockPosCapture;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockModelRenderer.class)
public class BlockModelRendererMixin {

    @Inject(
            method = "renderModel(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/resources/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/BlockPos;Lnet/minecraft/client/renderer/WorldRenderer;Z)Z",
            at = @At("HEAD")
    )
    private void captureBlockPos(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn,
                                  BlockPos posIn, WorldRenderer rendererIn, boolean checkSides,
                                  CallbackInfoReturnable<Boolean> cir) {
        // clear() before set() defends against stale state if a prior renderModel call
        // exited via exception (RETURN inject doesn't fire on exception paths in Mixin 0.7.x)
        BlockPosCapture.clear();
        BlockPosCapture.set(posIn, worldIn);
    }

    @Inject(
            method = "renderModel(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/resources/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/BlockPos;Lnet/minecraft/client/renderer/WorldRenderer;Z)Z",
            at = @At("RETURN")
    )
    private void clearBlockPos(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn,
                                BlockPos posIn, WorldRenderer rendererIn, boolean checkSides,
                                CallbackInfoReturnable<Boolean> cir) {
        BlockPosCapture.clear();
    }
}
