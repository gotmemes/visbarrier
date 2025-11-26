package io.github.gotmemes.visbarrier.mixin.mixins;

import io.github.gotmemes.visbarrier.Visbarrier;
import io.github.gotmemes.visbarrier.mixin.SupportedVersions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBarrier;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockBarrier.class)
@SupportedVersions("[1.9,)")
public abstract class BlockBarrierMixin_v1_9 extends Block {
    private BlockBarrierMixin_v1_9() {
        super(null);
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return Visbarrier.isVisible ? EnumBlockRenderType.MODEL : EnumBlockRenderType.INVISIBLE;
    }

    @Override
    public BlockRenderLayer getBlockLayer() {
        return BlockRenderLayer.TRANSLUCENT;
    }

    @Override
    public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return blockAccess.getBlockState(pos.offset(side)).getBlock() != this;
    }
}
