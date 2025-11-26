package io.github.gotmemes.visbarrier.mixin.mixins;

import io.github.gotmemes.visbarrier.Visbarrier;
import io.github.gotmemes.visbarrier.mixin.SupportedVersions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBarrier;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockBarrier.class)
@SupportedVersions("[1.8,1.9)")
public abstract class BlockBarrierMixin_v1_8 extends Block {
    public BlockBarrierMixin_v1_8() {
        super(null);
    }

    @Override
    public int getRenderType() {
        return Visbarrier.isVisible ? 3 : -1;
    }

    @Override
    public EnumWorldBlockLayer getBlockLayer() {
        return EnumWorldBlockLayer.TRANSLUCENT;
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess worldIn, BlockPos pos, EnumFacing side) {
        return worldIn.getBlockState(pos).getBlock() != this;
    }
}
